package ekenya.co.ke.vertxspringtlm.controller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static ekenya.co.ke.vertxspringtlm.controller.TemplateGeneratorVerticle.esbResponse;
import static ekenya.co.ke.vertxspringtlm.services.LegProcessorImpl.ASYNC_HANDLER_GATEWAY;

@Component
public class LegManagerVerticle extends AbstractVerticle {

    @Value("${vertxServicePorts.servicePorts}")
    private int servicePort;

    /**
     * Start the verticle.<p>
     * This is called by Vert.x when the verticle instance is deployed. Don't call it yourself.<p>
     * If your verticle does things in its startup which take some time then you can override this method
     * and call the startFuture some time later when start up is complete.
     *
     * @param startFuture a future which should be called when verticle start-up is complete.
     * @throws Exception
     */
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);

        Router router = Router.router(vertx);
        router.route("/esb*").handler(BodyHandler.create());
        router.route("/esb/tlm/process/leg-service/:legName").handler(this::RouteTemplateGenerator);


        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(servicePort,event -> {
                    if (event.succeeded()){
                        startFuture.isComplete();
                    }
                    else{
                        startFuture.fail(event.cause());
                    }
                });


    }

    private void RouteTemplateGenerator(RoutingContext routingContext) {
        vertx.eventBus().send(ASYNC_HANDLER_GATEWAY,"",event -> esbResponse(routingContext,event));
    }
}
