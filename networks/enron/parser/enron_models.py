import hashlib
import json
from dataclasses import dataclass, field
from itertools   import count


# ============================================================
# GLOBAL ID COUNTER
# Provides stable, sequential integer IDs for nodes.
# A single shared counter ensures uniqueness across all node
# types within one pipeline run.
# ============================================================

_node_id_counter = count(1)

def next_node_id() -> int:
    return next(_node_id_counter)


# ============================================================
# NODE MODELS
# ============================================================

@dataclass
class Person:
    """
    Represents a Person.

    Attributes:
        email  (str): normalised email address
        labels (list[str]): ["Person"]
        id     (int): auto-incremented integer, assigned on first creation
    """
    def __init__(self, email: str):
        self.labels = ["Person"]
        self.email  = email.strip().lower()
        self.id     = None          # assigned by NodesManaging on first insertion

    def get_id(self) -> int | None:
        return self.id

    def get_email(self) -> str:
        return self.email


@dataclass
class Email:
    """
    Represents an individual email message.

    Attributes:
        time   (str): ISO-8601 timestamp string
        labels (list[str]): always ["Email"]
        id     (int): auto-incremented integer, assigned on first creation
    """
    def __init__(self, time: str):
        self.labels = ["Email"]
        self.time   = time
        self.id     = None          # assigned by NodesManaging on first insertion

    def get_id(self) -> int | None:
        return self.id

    def get_time(self) -> str:
        return self.time


# ============================================================
# NODES MANAGING
# ============================================================

@dataclass
class NodesManaging:
    """
    Tracks Person and Email nodes, deduplicating persons by email address.

    Person nodes are deduplicated: the same email address always maps to the
    same integer ID, even across batches (as long as the same NodesManaging
    instance is used).

    Email nodes are never deduplicated — every parsed email message produces
    a unique Email node.
    """
    def __init__(self):
        # node lists flushed to JSON at each batch boundary
        self.nodes: dict[str, list] = {
            'person': [],
            'email' : [],
        }
        # deduplication registry: email_address -> int node_id
        self._person_registry: dict[str, int] = {}

    def reset_nodes(self):
        """Clear the per-batch lists without touching the deduplication registry."""
        self.nodes = {'person': [], 'email': []}

    # ----------------------------------------------------------
    # Person helpers
    # ----------------------------------------------------------

    def add_person(self, person: Person) -> int | None:
        """
        Adds a Person node if its email has not been seen before.

        Returns:
            The integer node_id (existing or newly assigned), or None if the
            email is empty / invalid.
        """
        email = person.get_email()
        if not email:
            return None

        if email in self._person_registry:
            return self._person_registry[email]

        person.id = next_node_id()
        self._person_registry[email] = person.id
        self.nodes['person'].append(person)
        return person.id

    # ----------------------------------------------------------
    # Email helpers
    # ----------------------------------------------------------

    def add_email(self, email_node: Email) -> int | None:
        """
        Adds an Email node (always unique — no deduplication).

        Returns:
            The integer node_id, or None if the timestamp is missing.
        """
        if not email_node.get_time():
            return None

        email_node.id = next_node_id()
        self.nodes['email'].append(email_node)
        return email_node.id


# ============================================================
# EDGES MANAGING
# ============================================================

@dataclass
class EdgesManaging:
    """
    Tracks Sent and Received edges, deduplicating by content hash.

    Edge types
    ----------
    SENT        : Person  -> Email   (the sender)
    RECEIVED    : Person  -> Email   (a To recipient)
    CCED        : Person  -> Email   (a CC recipient)
    BCCED       : Person  -> Email   (a BCC recipient)
    """
    def __init__(self):
        self.edges: list[dict]  = []
        self._seen: set[str]    = set()

    def reset_edges(self):
        """Clear the per-batch edge list without touching the seen-hash set."""
        self.edges = []

    def _add(self, src_id: int, dst_id: int, edge_type: str, extra: dict | None = None):
        """
        Internal helper — builds, deduplicates, and stores one edge.

        Args:
            src_id    : integer ID of the source node
            dst_id    : integer ID of the destination node
            edge_type : one of SENT / RECEIVED / CCED / BCCED
            extra     : optional dict of additional static properties
        """
        if src_id is None or dst_id is None:
            return

        payload = {'src': src_id, 'dst': dst_id, 'type': edge_type}
        edge_hash = hashlib.sha256(
            json.dumps(payload, sort_keys=True).encode('utf-8')
        ).hexdigest()

        if edge_hash in self._seen:
            return

        self._seen.add(edge_hash)
        edge = {
            'edge_id': edge_hash,
            'src'    : src_id,
            'dst'    : dst_id,
            'type'   : edge_type,
        }
        if extra:
            edge.update(extra)
        self.edges.append(edge)

    def add_sent_edge(self, sender_id: int, email_id: int):
        """SENT : Person -> Email"""
        self._add(sender_id, email_id, 'SENT')

    def add_received_edge(self, recipient_id: int, email_id: int):
        """RECEIVED : Person -> Email  (To field)"""
        self._add(recipient_id, email_id, 'RECEIVED')

    def add_cced_edge(self, person_id: int, email_id: int):
        """CCED : Person -> Email  (CC field)"""
        self._add(person_id, email_id, 'CCED')

    def add_bcced_edge(self, person_id: int, email_id: int):
        """BCCED : Person -> Email  (BCC field)"""
        self._add(person_id, email_id, 'BCCED')


# ============================================================
# HELPER FUNCTIONS
# ============================================================

def nodes_edges_creation(
    sender      : str,
    recipients  : list[str],
    timestamp   : str,
    cc          : list[str],
    bcc         : list[str],
    node_manager: NodesManaging,
    edge_manager: EdgesManaging,
):
    """
    Converts one parsed email into nodes and edges.

    Graph produced per email
    ------------------------
    - 1 Email node
    - 1 Person node for the sender  (deduplicated across all emails)
    - 1 Person node per recipient   (deduplicated)
    - 1 SENT edge    : sender     -> Email
    - N RECEIVED edges: recipient -> Email  (for each To address)
    - N CCED edges   : person     -> Email  (for each CC address)
    - N BCCED edges  : person     -> Email  (for each BCC address)
    """
    # 1. Create / retrieve sender node
    sender_id = node_manager.add_person(Person(sender))
    if sender_id is None:
        return

    # 2. Create Email node (always unique)
    email_node = Email(timestamp)
    email_id   = node_manager.add_email(email_node)
    if email_id is None:
        return

    # 3. SENT edge: sender -> email
    edge_manager.add_sent_edge(sender_id, email_id)

    # 4. RECEIVED edges: each To recipient -> email
    for recipient in recipients:
        r_id = node_manager.add_person(Person(recipient))
        if r_id is not None:
            edge_manager.add_received_edge(r_id, email_id)

    # 5. CCED edges
    for person in cc:
        p_id = node_manager.add_person(Person(person))
        if p_id is not None:
            edge_manager.add_cced_edge(p_id, email_id)

    # 6. BCCED edges
    for person in bcc:
        p_id = node_manager.add_person(Person(person))
        if p_id is not None:
            edge_manager.add_bcced_edge(p_id, email_id)