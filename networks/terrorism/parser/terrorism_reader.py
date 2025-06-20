import json
import pandas                as     pd
from   pathlib               import Path
from   neo4j_pandas_creation import edges_dataframes_creation, nodes_dataframes_creation
from   temporal_mri_json     import static_edges_json_creation, static_nodes_json_creation
from   terrorism_models      import nodes_edges_creation, NodesManaging, EdgesManaging



path_1 = "../data/globalterrorismdb_0522dist.xlsx"
path_2 = "../data/globalterrorismdb_2021Jan-June_1222dist.xlsx"
path_3 = "../neo4j_import"
path_4 = "../temporal_mri_import"


if __name__ == "__main__":
    part2 = pd.read_excel(path_2)
    node_manager = NodesManaging()
    edge_manager = EdgesManaging()

    # NODES AND EDGES CREATION:
    for idx, row in part2.iterrows():
        print(idx)
        row.dropna(inplace=True)
        nodes = nodes_edges_creation(
            row, 
            node_manager, 
            edge_manager
        )

    # NEO4J CSV CREATION:
    nodes_dataframes = nodes_dataframes_creation(node_manager.nodes)
    edges_dataframes = edges_dataframes_creation(edge_manager.edges)

    # NEO4J CSV SAVING:
    Path(path_3).mkdir(parents=True, exist_ok=True)
    for node_type, node_df in nodes_dataframes.items():
        node_df.to_csv(f"{path_3}/{node_type}.csv", index=False)
    edges_dataframes.to_csv(f"{path_3}/edges.csv", index=False)

    
    # MRI JSON CREATION:
    nodes_static_props = static_nodes_json_creation(node_manager.nodes)
    edges_static_props = static_edges_json_creation(edge_manager.edges)

    # MRI JSON SAVING:
    Path(path_4).mkdir(parents=True, exist_ok=True)
    with open(f"{path_4}/nodes_static_props.json", "w") as f:
        json.dump(nodes_static_props, f, indent=4)
    with open(f"{path_4}/edges_static_props.json", "w") as f:
        json.dump(edges_static_props, f, indent=4)

        