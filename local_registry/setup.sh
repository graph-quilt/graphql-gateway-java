#!/bin/bash

# By default, when this script is run, create registry with some providers
CREATE_WITH_PROVIDER=true

# get the location of local_registry
GQL_GW_DIR=$(dirname "$0")

showUsage() {
  echo "
  usage: ./setup.sh [-h | --help] [--empty-registry]
    -h | --help                   Displays Usage

    --empty-registry              Creates a local registry without any prpvider.
  "
}

# Create Topic Function
createTopic() {
  echo "Creating bucket: topics"
  aws --endpoint-url=http://localhost:4572 s3 mb s3://topics
}

# Create Topic Function
createDefaultRegistrations() {
  echo "Updating topics bucket with registry from $GQL_GW_DIR/dev folder"
  aws --endpoint-url=http://localhost:4572 s3 cp dev s3://topics/graphql-gateway/dev/ --recursive
  echo "Registry setup completed"
}

##############
# MAIN
##############
while [[ "$1" != "" ]]; do
  arg="${1}"
  case "${arg}" in
  -h | --help)
    showUsage
    exit 0
    ;;
  --empty-registry)
    CREATE_WITH_PROVIDER=false
    shift
    ;;
  *)
    echo "Unknown argument: $1"
    showUsage
    exit 1
    ;;

  esac
done

# Call create Topic
createTopic

if [ $CREATE_WITH_PROVIDER = true ]; then
  # execute in subshell
  (cd $GQL_GW_DIR; createDefaultRegistrations)
  exit 0
else
  echo "Created an empty registry."
  exit 0
fi
