package com.example.vertx.hocon.test;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class MyHttpVerticle extends MyVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(MyHttpVerticle.class);
    private static final int DEFAULT_PORT = 8082;

    private HttpServer server;

    @Override
    public void stop() {
        LOG.info("Shutting down web server");
        server.close();
    }

    @Override
    protected Future<Void> initialize() {
        Future<HttpServer> webServerFuture = Future.future();
        Router router = Router.router(vertx);

        JsonObject httpServerConfig = configuration.getJsonObject("httpServer");
        Integer port = httpServerConfig.getInteger("port", DEFAULT_PORT);

        JsonObject answerConfig = httpServerConfig.getJsonObject("answer");
        JsonArray routes = answerConfig.getJsonArray("routes");

        routes.stream().map(o -> (String) o).forEach(routeStr -> {
            LOG.info("Adding '{}' route", routeStr);
            router.get(routeStr)
                    .produces(HttpHeaderValues.APPLICATION_JSON.toString())
                    .handler(this::getAll);
        });

        LOG.info("Starting web server on port {}", port);
        server = vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(port, webServerFuture);

        return webServerFuture.mapEmpty();
    }

    private void getAll(RoutingContext ctx) {
        HttpServerResponse response = ctx.request().response();
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);

        JsonObject httpServerConfig = configuration.getJsonObject("httpServer");
        JsonObject answerConfig = httpServerConfig.getJsonObject("answer");

        response.end(new JsonObject()
                .put("title", answerConfig.getValue("title"))
                .put("body", answerConfig.getValue("body"))
                .toBuffer());
    }

    @Override
    protected String getHoconConfigPath() {
        return "com/example/vertx/hocon/test/test.conf";
    }

}
