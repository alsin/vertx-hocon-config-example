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

# Reading HOCON configuration file
This way you can instruct Vertx to read your HOCON configuration file from the classpath:
    
    ConfigRetrieverOptions options = new ConfigRetrieverOptions();
    options.addStore(new ConfigStoreOptions()
                     .setType("file")
                     .setFormat("hocon")
                     .setConfig(new JsonObject().put("path", "com/example/vertx/hocon/test/test.conf")));

Then provide this configuration to the `ConfigRetriever` and read the configuration asynchronously: 

    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
    retriever.getConfig(ar -> {
        if (ar.succeeded()) {
            this.configuration = ar.result();
            future.complete();
        } else {
            future.fail(ar.cause());
        }
    });

Now you can work with the fetched configuration `JsonObject`. 
 
# Testing HOCON configuration 
Testing default values is as easy as simply defining an expected `JsonObject` configuration and comparing it with the 
retrieved configuration:

    private static final JsonObject EXPECTED_DEFAULT_CONFIG = new JsonObject()
            .put("httpServer", new JsonObject()
                    .put("port", 8080)
                    .put("answer", new JsonObject()
                            .put("title", "Some title")
                            .put("body", "Some body")
                            .put("routes", new JsonArray()
                                    .add("/")
                                    .add("/all"))));

    ...

    @Test
    public void testDefaultHoconConfig(TestContext context) {
        ...
        JsonObject configuration = ...;// Read the config
        context.assertEquals(EXPECTED_DEFAULT_CONFIG, configuration);
        ...
    }
    
Testing substitution of HOCON config values with environment variables is more tricky. Vertx's HOCON processor uses 
Typesafe library which makes its own copy of system environment variables (`System.getenv()`) in a static initializer.
It's not possible to modify Java runtime's environment variables directly. I was not able to find a completely clean 
solution for this task. Nevertheless, here are two options of how you can do it.

# Using PowerMock to mock static classes and methods which are used by Typesafe
This method implies mocking the Java's `System` class and its `getenv()` method. Also, I found out that I had to use 
PowerMock's `@PrepareForTest` annotation for the Typesafe internal implementation class 
`com.typesafe.config.impl.ConfigImpl` to make sure it does use our mocked `System` class.

    private static final JsonObject EXPECTED_REWRITTEN_CONFIG = new JsonObject()
            .put("httpServer", new JsonObject()
                    .put("port", "8081")
                    .put("answer", new JsonObject()
                            .put("title", "Response title")
                            .put("body", "Response body")
                            .put("routes", new JsonArray()
                                    .add("/")
                                    .add("/all")
                                    .add("/json"))));


    @Test
    @PrepareForTest({ System.class, ConfigImpl.class })
    public void testHoconConfigWithMockedEnvironmentVariables(TestContext context) {
        mockStatic(System.class);

        //Prepare your environment variables
        Map<String, String> envProps = new HashMap<>();
        envProps.put("HTTP_PORT", "8081");
        
        ...

        when(System.getenv()).thenReturn(envProps);

        //Fetch config and compare it with the expected
        
        ... 
        
        JsonObject configuration = ...;// Read the config
        context.assertEquals(EXPECTED_REWRITTEN_CONFIG, configuration);
    }

Notice that when you substitute numeric values in your HOCON configuration file with environment variables, they become 
strings. So, in our second expected `JsonObject` we define `port` as a string:

    .put("port", "8081")

Otherwise comparison will fail. 

# Using reflection to make the Map returned by System.getenv() modifiable
This solution is even dirtier than the previous one. But just listing it here as a solution :) It's based on using 
reflection to make the Map instance returned by `System.getenv()` modifiable. Thus, you can put there your environment 
variables during runtime. However we still need to make sure the Typesafe's `ConfigImpl` class gets reloaded (it might 
have been loaded and initialized in a previous test) and initialized with those environment variables we put into the 
`System`'s environment Map. For that we again can use the PowerMock's `@PrepareForTest` annotation:

    @Test
    @PrepareForTest({ ConfigImpl.class })
    public void testHoconConfigWithInjectedEnvironmentVariables(TestContext context) throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> unmodifiableEnvPropMap = System.getenv();
        Class<?> clazz = unmodifiableEnvPropMap.getClass();
        Field unmodifiableMapField = clazz.getDeclaredField("m");
        unmodifiableMapField.setAccessible(true);
        Map<String, String> envProps = (Map<String, String>) unmodifiableMapField.get(unmodifiableEnvPropMap);
        
        //Prepare your environment variables
        Map<String, String> envProps = new HashMap<>();
        envProps.put("HTTP_PORT", "8081");
        
        ...

        //Fetch config and compare it with the expected
        
        ... 
        
        JsonObject configuration = ...;// Read the config
        context.assertEquals(EXPECTED_REWRITTEN_CONFIG, configuration);
        
        ...
        
        //Clean your environment variables, better in @After code
        envProps.remove("HTTP_PORT");
        ...
        
    }

# Web server
You can also build the project with 

    mvn clean package
    
and then from the root of the project launch the application by executing:

    java -jar -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory target/vertx-hocon-test-example-1.0-SNAPSHOT-fat.jar

This will deploy a Verticle that will launch a web server on the port specified in the `test.conf` HOCON configuration file. 
The server will also read the endpoints (routes) for which it will handle GET requests by simply returning a JSON with 
the `title` and `body` properties also defined in the configuration file. E.g. after starting the server hit the following 
address in your browser: 

    http://localhost:8080/ 

And you should get a JSON in response:

    {"title":"Some title","body":"Some body"} 

