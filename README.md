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

Top Highlights include

* Dynamic registration so that the gateway is loosely coupled with the provider development lifecycle.
* Registering REST endpoints using the [@adapter](https://github.com/graph-quilt/graphql-service-adapters) directive and Service DSL
* Attribute Based Access Control using the [graphql-authorization-java](https://github.com/graph-quilt/graphql-authorization-java) library.

### Pre-requisites

Make sure you have the following installed on your machine

* jdk 1.8
* maven
* docker
* awscli - _make sure you have configured a value(any string value is fine) for keyid, key and region._

### Running the graphql-gateway-java locally

* Install [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2-mac.html#cliv2-mac-install-cmd) 
and configure credentials and region (any string value is fine).
    ```
    aws configure
    ```
    
* Start up the local registry
    ```
    ./local_registry/start.sh
    ```
  
* Set up the registry for the application. This will initialize the registry with providers in `local_registry` folder.
    ```
    ./local_registry/setup.sh
    ```
    OR
     
    Add `--empty-registry` option to create an empty registry. You will need this if you are registering a provider by 
    manually uploading files. 
    ```
    ./local_registry/setup.sh --empty-registry
    ```
    
* Start the application. Default port: 7000

    ```
    ./run.sh
    ```
    OR 
   
    Run the class `GraphqlGatewayApplication` as Spring Application using IntelliJ. Set the active profile as **local**.
    
**No need to restart the application when new you register a new provider service**.

* After making any updates to the providers in the `local_registry`, just re-run the correct `setup.sh`. 
**No need to restart the application**.

### Run with docker compose

The docker compose will start both **localstack** and **graphql gateway**.  To start, run the command

`$ docker compose up`

this creates empty registry.

Or, run

`$ docker compose -f docker-compose-local-registry.yml up`

You should see the Spring Boot Logo and a message "Started GraphQLGatewayApplication ..."
  
### Test using GraphiQL

* Navigate to [http://localhost:7000/graphiql](http://localhost:7000/graphiql)

### Documentation
[graphql-gateway-documentation](https://graph-quilt.github.io/graphql-orchestrator-java/) <br/>


### Intellij development

* [Steps to enable lombok in IntelliJ](https://www.baeldung.com/lombok-ide)
* IntelliJ style guide is available in `src/format/intellij-styleguide.xml`. 

## Contributing

Please see our [contribution guide](.github/CONTRIBUTING.md)
