#!/usr/bin/env bash

# Display a usage message
usage="
usage: ./run.sh [-h | --help] [--s3-disabled] [VM_ARGS]
  -h | --help                   Displays Usage

  --s3-disabled                 Disables S3 Schema loading. Allows you to run service without S3 credentials.

  VM_ARGS                       Pass arbitrary JVM system properties to the service.
                                Example system property: -Dmy.property=true
"

function show_usage () {
  echo "$usage"
}

VM_ARGS=""

JAR_FILE_LOCATION="target/graphql-gateway-java.jar"

# Arg parsing
while [[ "$1" != "" ]]; do
    arg="${1}"
    case "${arg}" in
        -h | --help )
            show_usage
            exit 0
            ;;
        --s3-disabled )
            VM_ARGS="$VM_ARGS -Daws.s3.enabled=false"
            shift
            ;;
        --containerized )
            JAR_FILE_LOCATION="graphql-gateway-java.jar"
            shift
            ;;
        -D* )
            VM_ARGS="$VM_ARGS $arg"
            shift
            ;;
        * )
            echo "Unknown argument: $1"
            show_usage
            exit 1

    esac
done

sleep 30

java -Dspring.profiles.active=local -Dserver.max-http-header-size=17000 ${VM_ARGS} -jar $JAR_FILE_LOCATION