package com.example.vertx.hocon.test;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class MyVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MyVerticle.class);

    protected JsonObject configuration;

    @Override
    public void start(Future<Void> startFuture) {
        this.readConfig(getHoconConfigPath())
                .compose(voi -> initialize())
                .setHandler(startFuture);
    }

    public JsonObject getConfiguration() {
        return configuration;
    }

    protected Future<Void> readConfig(String hoconConfigPath) {
        Future<Void> future = Future.future();

        ConfigRetrieverOptions options = new ConfigRetrieverOptions();
        options.addStore(new ConfigStoreOptions()
                .setType("json")
                .setConfig(vertx.getOrCreateContext().config()));

        if (hoconConfigPath != null) {
            options.addStore(new ConfigStoreOptions()
                    .setType("file")
                    .setFormat("hocon")
                    .setConfig(new JsonObject()
                            .put("path", hoconConfigPath)));
        }

        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        retriever.getConfig(ar -> {
            if (ar.succeeded()) {
                this.configuration = ar.result();
                LOG.info("Result configuration: {}", this.configuration.encodePrettily());
                future.complete();
            } else {
                LOG.error("Failed to read configuration", ar.cause());
                future.fail(ar.cause());
            }
        });
        return future;
    }

    protected abstract Future<Void> initialize();

    protected abstract String getHoconConfigPath();



}
