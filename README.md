# vertx-hocon-config-example
A Maven example project demonstrating how a Vertx microservice application can use a HOCON configuration file and how it can be 
tested.

# Using Vertx HOCON configuration module
First, add the dependency for Vertx HOCON module:

    <dependency>
        <groupId>io.vertx</groupId>
        <artifactId>vertx-config-hocon</artifactId>
        <version>3.5.1</version>
    </dependency>
    
This also brings `vertx-config` transitively. In this sample project we are using a combination of JSON and HOCON config 
processors, so we need to add a configuration for `ConfigProcessor SPI` that would make the application support both 
formats. We can achieve this by adding a file named `io.vertx.config.spi.ConfigProcessor` under:

    src/main/resources/META-INF/services
    
with the following content:

    io.vertx.config.impl.spi.JsonProcessor
    io.vertx.config.hocon.HoconProcessor
             

# HOCON configuration file
This project uses the following simple configuration file in HOCON format:

    httpServer {
        port = 8080
        answer {
            title = "Some title"
            body = "Some body"
            routes = [ "/", "/all" ]
        }
    }

    httpServer.port           = ${?HTTP_PORT}
    httpServer.answer.title   = ${?RESPONSE_TITLE}
    httpServer.answer.body    = ${?RESPONSE_BODY}
    httpServer.answer.routes += ${?ROUTES}

This file is located under the project resources (`src/main/resources`). The application will have it inside the JAR.
There are four values in this configuration file which can be overwritten by environment variables:

 * `HTTP_PORT`
 * `RESPONSE_TITLE`
 * `RESPONSE_BODY`
 * `ROUTES`

If during runtime these properties are set and available via `System.getenv()`, their values will replace the respective 
default values defined in the HOCON configuration file.

We want to test both:
 
 * default values which are defined in the HOCON configuration file
 * exporting the mentioned environment variables indeed replaces the default values  
 
Testing value substitution by environment variables can be useful for revealing typos in the HOCON file.


 