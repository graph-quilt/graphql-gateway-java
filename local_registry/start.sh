#!/bin/bash

# get the location of local_registry
GQL_GW_DIR=$(dirname "$0")

#Clean up the registry
cleanUpLocalStack() {
  rm -rf ./.localstack/
}

#Fake AWS S3
startLocalStack() {
  docker-compose run --service-ports localstack
}

if [ -d "$GQL_GW_DIR/dev" ]
then
  # execute in subshell
  (cd $GQL_GW_DIR; cleanUpLocalStack; startLocalStack)
else
  echo "$GQL_GW_DIR/dev not found."
fi
