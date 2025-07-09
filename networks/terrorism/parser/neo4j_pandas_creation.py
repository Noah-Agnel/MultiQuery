import pandas as pd


def nodes_dataframes_creation(nodes_data):
    """
    Creates a pandas dataframe from the nodes

    Args:
        nodes (dict): A dictionary of nodes

    Returns:
        dict: A dictionary of pandas dataframes
    """
    nodes_dataframes = {}
    for node_type, nodes in nodes_data.items():
        nodes_dataframes[node_type] = [node.__dict__.copy() for node in nodes]
        for node in nodes_dataframes[node_type]:
            if "id" not in node:
                print(node)
            node["id:ID"] = node.pop("id")
            for key, value in node.items():
                if not isinstance(value, list):
                    continue
                node[key] = ":".join(value)
            node["labels:LABELS"]   = node.pop("labels")
        nodes_dataframes[node_type] = pd.DataFrame(nodes_dataframes[node_type])
    return nodes_dataframes


def edges_dataframes_creation(edges):
    """
    Creates a pandas dataframe from the edges
    """
    edges_dataframes = []
    for edge in edges:
        edge              = edge.copy()
        edge["src:ID"   ] = edge.pop("src")
        edge["dst:ID"   ] = edge.pop("dst")
        edge["type:TYPE"] = edge.pop("type")
        edge["edge_id"  ] = edge.pop("edge_id")
        edges_dataframes.append(edge)
    return pd.DataFrame(edges_dataframes)