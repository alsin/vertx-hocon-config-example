package com.example.vertx.hocon.test;

import io.vertx.core.Future;

public class MyHttpVerticle extends MyVerticle {

    @Override
    protected Future<Void> initialize() {
        return Future.succeededFuture();
    }

    @Override
    protected String getHoconConfigPath() {
        return "com/example/vertx/hocon/test/test.conf";
    }
}
