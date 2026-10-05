package com.procurax.gateway;

import io.vertx.core.Vertx;

public final class GatewayApplication {

    private GatewayApplication() {
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        GatewayServer gateway = new GatewayServer(vertx, GatewayConfig.fromEnvironment());
        gateway.start().onSuccess(server ->
                System.getLogger(GatewayApplication.class.getName()).log(
                        System.Logger.Level.INFO, "Vert.x gateway listening on port {0}", server.actualPort()))
                .onFailure(error -> {
                    System.getLogger(GatewayApplication.class.getName()).log(
                            System.Logger.Level.ERROR, "Failed to start Vert.x gateway", error);
                    vertx.close();
                    System.exit(1);
                });
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                gateway.close().onComplete(ignored -> vertx.close())));
    }
}
