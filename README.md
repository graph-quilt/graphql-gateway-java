<div align="center">

  ![graphql-gateway-java](./logo.png)

</div>

[![ Master Build and Publish](https://github.com/graph-quilt/graphql-gateway-java/actions/workflows/master.yml/badge.svg?branch=master&event=push)](https://github.com/graph-quilt/graphql-gateway-java/actions/workflows/master.yml)
[![codecov](https://codecov.io/gh/graph-quilt/graphql-gateway-java/branch/master/graph/badge.svg?token=G392PV1BAI)](https://codecov.io/gh/graph-quilt/graphql-gateway-java) 
[![Apache 2](http://img.shields.io/badge/license-Apache%202-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0)

### Overview

graphql-gateway-java exposes data from various graph microservices using a single unified GraphQL schema. These microservices can be dynamically
registered with the Gateway using the `/register` endpoint. It uses [graphql orchestrator library](https://github.com/graph-quilt/graphql-orchestrator-java) for federating schemas from 
various data providers. 

Highlights:

* Dynamic registration so that the gateway is loosely coupled with the provider development lifecycle.
* Registering REST endpoints using the [@adapter](https://github.com/graph-quilt/graphql-service-adapters) directive and Service DSL
* Attribute Based Access Control using the [graphql-authorization-java](https://github.com/graph-quilt/graphql-authorization-java) library.

### Prerequisites

Make sure you have the following installed on your machine:

* [Java 11](https://www.oracle.com/java/technologies/downloads/#java11)
* [Maven](https://maven.apache.org/install.html)
* [Docker](https://docs.docker.com/engine/install/)
* [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) 

### Running graphql-gateway-java locally

When running the application locally, [localstack](https://localstack.cloud/) is used to mock AWS S3.  Follow these steps to start the application.

* Build the project:
    ```
    mvn clean install
    ```

* Run aws configure. Enter any value to the prompts presented. Since localstack is used, a real AWS credential is not needed, but make sure to input a valid region name (e.g. `us-west-1`) and output format (e.g. `json`).
    ```
    aws configure
    ```

* Start Docker daemon if not already running.
    
* Start the local registry. This starts a localstack docker container.
    ```
    ./local_registry/start.sh
    ```
  
* Set up the local registry. This will initialize the registry with example service providers in `local_registry` folder.
    ```
    ./local_registry/setup.sh --empty-registry
    ```
    
* Start the application. Once started, it listens on port 7000.

    ```
    ./run.sh
    ```
    OR 
   
    Run the class `GraphqlGatewayApplication` as Spring Application using IntelliJ. Set the active profile as **local**.

**NOTE: You do not need to restart the application when you register a new provider service**.


### Test using GraphiQL

* Navigate to [http://localhost:7000/graphiql](http://localhost:7000/graphiql)
* We also have [example subgraphs](https://github.com/graph-quilt/example-subgraphs) with registration instructions for easy testing.

### Documentation

* [graphql-gateway-documentation](https://graph-quilt.github.io/graphql-orchestrator-java/)


### Intellij development

* [Steps to enable lombok in IntelliJ](https://www.baeldung.com/lombok-ide)
* IntelliJ style guide is available in `src/format/intellij-styleguide.xml`. 

## Contributing

Please see our [contribution guide](.github/CONTRIBUTING.md).
