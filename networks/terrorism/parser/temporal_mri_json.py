def static_nodes_json_creation(nodes):
    """
    Creates a JSON file from the nodes

    Args:
        nodes (dict): A dictionary of nodes

    Returns:
        dict: A dictionary of nodes static properties
    """
    nodes_static_props = {}
    for node_type, nodes in nodes.items():
        nodes_static_props[node_type] = []
        for node in nodes:
            node    = node.__dict__
            node_id = node.pop("id")
            labels  = node.pop("labels")
            nodes_static_props[node_type].append({
               "node_id"     : node_id,
               "labels"      : labels,
               "static_props": node,
               "is_active"   : True
            })
    return nodes_static_props


def static_edges_json_creation(edges):
    """
    Creates a JSON file from the edges

    Args:
        edges (dict): A dictionary of edges

    Returns:
        dict: A dictionary of edges static properties
    """
    edges_static_props = [
        {
            "edge_id"     : edge.pop("edge_id"),
            "source_id"   : edge.pop("src"),
            "target_id"   : edge.pop("dst"),
            "edge_type"   : edge.pop("type"),
            "static_props": edge
        }
        for edge in edges
    ]
    return edges_static_props