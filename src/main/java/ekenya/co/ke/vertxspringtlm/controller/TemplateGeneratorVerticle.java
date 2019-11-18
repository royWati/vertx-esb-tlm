package ekenya.co.ke.vertxspringtlm.controller;

import ekenya.co.ke.vertxspringtlm.dao.wrapper.CustomResponse;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import static ekenya.co.ke.vertxspringtlm.services.TemplateConfigurationImpl.CREATE_TLM_TEMPLATE;

@Component
public class TemplateGeneratorVerticle extends AbstractVerticle {

    @Value("${vertxServicePorts.templatePorts}")
    private int templatePort;
    /**
     * If your verticle does a simple, synchronous start-up then override this method and put your start-up
     * code in here.
     *
     * @throws Exception
     */
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);

        Router router = Router.router(vertx);
        router.route("/esb*").handler(BodyHandler.create());
        router.post("/esb/tml/generator/create/leg").handler(this::RouteTemplateGenerator);
        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(templatePort,event -> {
                    if (event.succeeded()){
                        startFuture.isComplete();
                    }
                    else{
                        startFuture.fail(event.cause());
                    }
                });

    }

    private void RouteTemplateGenerator(RoutingContext routingContext) {

        JsonObject jsonObject = routingContext.getBodyAsJson();

        vertx.eventBus().send(CREATE_TLM_TEMPLATE,jsonObject,event -> {
            esbResponse(routingContext,event);
        });
    }

    static void esbResponse(RoutingContext routingContext, AsyncResult<Message<Object>> event) {
        if (event.succeeded()) {
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(HttpStatus.OK.value())
                    .end(event.result().body().toString());
        }else{

            System.out.println(event.cause().toString());
            CustomResponse customResponse = new CustomResponse();
            customResponse.setLegMessage("service currently unavailable ");
            customResponse.setLegStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

            JsonObject jsonObject = JsonObject.mapFrom(customResponse);
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(HttpStatus.OK.value())
                    .end(jsonObject.toString());
        }
    }
}
