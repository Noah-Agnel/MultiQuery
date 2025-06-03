*INTRODUCTION*


This document reports the tables schema for ICEBERG database.
We will use Spark as framework.

There are five tables:

**1. path_subset**

This table contains a structure similar to a sparse bit-matrix

```
CREATE TABLE node_label_pair (
    -- Keys
    pair_id           BIGINT,
    pair_hash         STRING,

    -- Source and Destination nodes labels
    src_labels        ARRAY<INT>,
    dst_labels        ARRAY<INT>,
    
    -- Edge labels
    -- directed
    edge_1_2_types    MAP<INT, INT>,
    edge_2_1_types    MAP<INT, INT>, 

    -- undirected
    edge_bi_types     MAP<INT, INT>,
    
    created_at        TIMESTAMP
) USING ICEBERG
PARTITIONED BY (
    bucket(32, pair_id)
);
```


**2.1 Source and Destination Nodes matched by bitmatrix**

This table contains all the source-destination nodes pairs.
The pairs are linked by node_label_pair
```
CREATE TABLE node_pair_matches (
    match_id          BIGINT,
    pair_id           BIGINT,
    source_node_id    BIGINT,
    target_node_id    BIGINT,
    created_at        TIMESTAMP
) USING ICEBERG
PARTITIONED BY (
    bucket(32, pair_id)
);
```

**2.2 match edges**

```
CREATE TABLE match_edges (
    match_edge_id     BIGINT,
    match_id          BIGINT,          
    edge_id           BIGINT,
    created_at        TIMESTAMP
) USING ICEBERG
PARTITIONED BY (
    bucket(32, match_id)
);
```


**3. Static nodes properties**

This table contains the static part of each node.

```
CREATE TABLE node_static_props (
    node_id        BIGINT,
    names_values   MAP<STRING, STRING>,
    created_at     TIMESTAMP,
    is_active      BOOLEAN
) USING ICEBERG
PARTITIONED BY (
    bucket(32, node_id)
);
```


**4. Dynamic nodes properties**

This table contains the dynamic nodes properties, therefore they
change with the time.

```
CREATE TABLE node_dynamic_props (
    node_id        BIGINT,
    name           STRING,
    value          STRING,
    from           TIMESTAMP,
    to             TIMESTAMP,
    created_at     TIMESTAMP
) USING ICEBERG
PARTITIONED BY (
    week(from),
    bucket(32, node_id),
    name
);
```

**5. Static edges properties**

This table contains the static edges properties. these are the 
properties where the values is equal to the whole period in which
the edge is defined.

```
CREATE TABLE edge_static_props (
    edge_id        BIGINT,
    names_values   MAP<STRING, STRING>,
    from           TIMESTAMP
    to             TIMESTAMP
    created_at     TIMESTAMP
) USING ICEBERG
PARTITIONED BY (
   bucket(32, edge_id)
);
```


**6. Dynamic edges properties**

This table contains the dynamic edges properties, therefore they
chane with the time.

```
CREATE TABLE edge_dynamic_props (
    edge_id        BIGINT,
    name           STRING,
    value          STRING,
    from           TIMESTAMP,
    to             TIMESTAMP
) USING ICEBERG
PARTITIONED BY (
    week(valid_from),
    bucket(32, edge_id),
    name
);
```