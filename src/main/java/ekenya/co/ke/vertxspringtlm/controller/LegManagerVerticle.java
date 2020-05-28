package ekenya.co.ke.vertxspringtlm.controller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

import static ekenya.co.ke.vertxspringtlm.controller.TemplateGeneratorVerticle.esbResponse;
import static ekenya.co.ke.vertxspringtlm.services.LegProcessorImpl.ASYNC_HANDLER_GATEWAY;
import static ekenya.co.ke.vertxspringtlm.services.LegProcessorImpl.INTIATE_TLM_MATRIX;

/**
 * @Version 1.0
 *
 * TODO:: HOW TO HANDLE CUSTOM MESSAGE RESPONSES ON THE RESPONSE
 * TODO:: IMPLEMENT THE CALLBACK FEATURE TO THE TLM SERVICE
 * TODO:: CREATE A DEDICATE LINE OF COMMUNICATION BETWEEN THE TLM AND THE ESB i.e HTTP RESPONSE SOCKETS
 *
 * {
 *     "serviceName":"service-name",
 *     "tlmTransactionRefNo": "TLM-TRANSACTION-REFNO",
 *     "requestBody":{
 *         "key":"value"
 *     }
 * }
 */
@Component
public class LegManagerVerticle extends AbstractVerticle {
    private final static Logger logger = Logger.getLogger(LegManagerVerticle.class.getName());
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
                        logger.info("server running on port ... "+servicePort);
                        startFuture.isComplete();
                    }
                    else{
                        startFuture.fail(event.cause());
                    }
                });
    }

    private void RouteTemplateGenerator(RoutingContext routingContext) {

        JsonObject requestJsonObject = routingContext.getBodyAsJson();
        String legName = routingContext.request().getParam("legName");

        logger.info("processing leg service request...");

        JsonObject jsonObject = new JsonObject();
        jsonObject.put("requestFields",requestJsonObject);
        jsonObject.put("legManager",legName);
        vertx.eventBus().send(INTIATE_TLM_MATRIX,jsonObject,event -> esbResponse(routingContext,event));
    }
}
