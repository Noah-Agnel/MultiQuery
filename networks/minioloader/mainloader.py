import argparse
import re
import os
from   minio import Minio


def normalize_bucket_name(bucket_name):
    """
    Normalize bucket name to follow S3/MinIO naming rules:
    - Convert to lowercase
    - Replace invalid characters with hyphens
    - Ensure it starts and ends with alphanumeric characters
    - Ensure length is between 3 and 63 characters
    """
    # Convert to lowercase
    normalized = bucket_name.lower()
    
    # Replace any character that's not lowercase letter, number, or hyphen with hyphen
    normalized = re.sub(r'[^a-z0-9-]', '-', normalized)
    
    # Remove consecutive hyphens
    normalized = re.sub(r'-+', '-', normalized)
    
    # Ensure it starts and ends with alphanumeric character
    normalized = normalized.strip('-')
    
    # Ensure minimum length
    if len(normalized) < 3:
        normalized = normalized + '-bucket'
    
    # Ensure maximum length
    if len(normalized) > 63:
        normalized = normalized[:63].rstrip('-')
    
    return normalized


def parse_arguments():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(description="Main loader for network processing")
    
    # Add your arguments here - examples of common parameter types
    parser.add_argument('--minio_host'  , '-i', type=str, required=True, help='minio host ip'    )
    parser.add_argument('--access_key'  , '-a', type=str, required=True, help='minio access key' )
    parser.add_argument('--secret_key'  , '-s', type=str, required=True, help='minio secret key' )
    parser.add_argument('--bucket_name' , '-b', type=str, required=True, help='minio bucket name')
    parser.add_argument('--network_path', '-n', type=str, required=True, help='network directory')
    
    return parser.parse_args()

def main():
    """Main function to run the loader."""
    args = parse_arguments()
    
    # Normalize bucket name to ensure S3/MinIO compliance
    normalized_bucket_name = normalize_bucket_name(args.bucket_name)
    if normalized_bucket_name != args.bucket_name:
        print(f"Bucket name '{args.bucket_name}' normalized to '{normalized_bucket_name}' for S3/MinIO compliance")

    # Connect to Minio
    print("Connecting to Minio...")
    client = Minio(
        args.minio_host,
        access_key = args.access_key,
        secret_key = args.secret_key,
        secure     = False
    )
    
    # Bucket creation if not exists
    if not client.bucket_exists(normalized_bucket_name):
        client.make_bucket(normalized_bucket_name)
        print(f"Created bucket: {normalized_bucket_name}")
    else:
        print(f"Bucket already exists: {normalized_bucket_name}")

    # Load files from path
    for file in os.listdir(args.network_path):
        file_low  = file.lower()

        
        # Skip if not nodes or edges
        if "node" not in file_low and "edge" not in file_low:
            continue

        # Skip if not json or csv
        if "json" not in file_low and "csv" not in file_low:
            continue

        # Load file
        file_path = os.path.join(args.network_path, file)
        print(f"Uploading {file}")   
        client.fput_object(
            normalized_bucket_name,
            file,
            file_path,
            content_type='application/json' if "json" in file else 'text/csv'
        )

    print("Done")
        
if __name__ == "__main__":
    main()
