import json
import pandas                as     pd
from   pathlib               import Path
from   neo4j_pandas_creation import edges_dataframes_creation, nodes_dataframes_creation
from   temporal_mri_json     import static_edges_json_creation, static_nodes_json_creation
from   terrorism_models      import nodes_edges_creation, NodesManaging, EdgesManaging
from   tqdm                  import tqdm


path_3 = "../neo4j_import"
path_4 = "../temporal_mri_import"


if __name__ == "__main__":
    config       = json.load(open("../config/config.json"))
    node_manager = NodesManaging()
    edge_manager = EdgesManaging()


    for path_key, path_value in config.items():
        if "path" not in path_key:
            continue
        
        print(f"Processing: {path_key}")
        header     = pd.read_excel(path_value, nrows=0)
        batch_num  = 0
        skip_rows  = 1
        batch_size = 100000

        while True:
            print(f"Processing batch: {batch_num}")

            # RESET:
            node_manager.reset_nodes()
            edge_manager.reset_edges()


            # READ BATCH:
            batch_df   = pd.read_excel(
                path_value, 
                skiprows = skip_rows, 
                nrows    = batch_size, 
                header   = None
            )
            
            batch_num += 1
            skip_rows += batch_size
            if batch_df.empty:
                break
            
            # SET COLUMNS:
            batch_df.columns = header.columns
            
            # NODES AND EDGES CREATION:
            for idx, row in tqdm(batch_df.iterrows()):
                row.dropna(inplace=True)
                nodes_edges_creation(
                    row, 
                    node_manager, 
                    edge_manager
                )

            if config["db_selection"] == "neo4j":
                # NEO4J CSV CREATION:
                print(f"Creating nodes and edges for neo4j")
                nodes_dataframes = nodes_dataframes_creation(node_manager.nodes)
                edges_dataframes = edges_dataframes_creation(edge_manager.edges)

                # NEO4J CSV SAVING:
                Path(path_3).mkdir(parents=True, exist_ok=True)
                for node_type, node_df in nodes_dataframes.items():
                    if batch_num == 0: 
                        node_df.to_csv(
                        f"{path_3}/{node_type}.csv",
                        index   = False
                        )
                    else:
                        node_df.to_csv(
                            f"{path_3}/{node_type}.csv", 
                            mode   = "a", 
                            header = False, 
                            index  = False
                        )
                
                if batch_num == 0:     
                    edges_dataframes.to_csv(
                        f"{path_3}/edges.csv",
                        index = False
                    )
                else:
                    edges_dataframes.to_csv(
                        f"{path_3}/edges.csv", 
                        mode   = "a", 
                        header = False, 
                        index  = False
                    )
            else:
                # MRI JSON CREATION:
                print(f"Creating nodes and edges for MRI")
                nodes_static_props = static_nodes_json_creation(node_manager.nodes)
                edges_static_props = static_edges_json_creation(edge_manager.edges)

                # MRI JSON SAVING:
                Path(path_4).mkdir(parents=True, exist_ok=True)
                for node_type, nodes in nodes_static_props.items():
                    if len(nodes) == 0:
                        continue
                    with open(f"{path_4}/{path_key}_{node_type}_nodes_static_props_{batch_num}.json", "w") as f:
                        json.dump(nodes, f, indent=4)
                with open(f"{path_4}/{path_key}_edges_static_props_{batch_num}.json", "w") as f:
                    json.dump(edges_static_props, f, indent=4)