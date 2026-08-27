# MultiQuery

Subgraph-matching engine for temporal multi-graphs. Given a small Cypher-style
`MATCH ... WHERE ... RETURN` pattern, finds every occurrence of that pattern in
a much larger target graph. Built on Scala, Apache Spark, and Apache Iceberg;
demonstrated against the ICIJ Panama Papers dataset (officers, entities,
addresses, and intermediaries).

## How it works

1. **BitMatrix encoding** (`bitmatrix` package) — every distinct label/edge-type
   combination in the target graph is encoded once as a fixed-width bitmask
   and persisted, so it's computed once rather than per query.
2. **Compatibility-domain pruning** (`CompatibilityDomainEngine`) — for each
   query edge, a bitwise-AND filter (`(target & query) == query`) discards
   target node-pairs that can't satisfy that edge, before touching any
   property values.
3. **Multi-way join** (`matching.MultiJoinMatching`) — the surviving per-edge
   candidate tables are joined on shared query variables into full subgraph
   matches.

`pipeline.FullPipeline` orchestrates the end-to-end query: Cypher parsing →
compatibility domain → WHERE pushdown → multi-way join → property joins →
remaining WHERE evaluation → RETURN projection, with each stage timed.
