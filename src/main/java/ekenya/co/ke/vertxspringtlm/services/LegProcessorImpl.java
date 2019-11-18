package ekenya.co.ke.vertxspringtlm.services;

import ekenya.co.ke.vertxspringtlm.dao.wrapper.CustomResponse;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Component
public class LegProcessorImpl extends AbstractVerticle implements LegProcessor {

    private String ASYNC_HANDLER = "ASYNC-HANDLER";
    public static String ASYNC_HANDLER_GATEWAY = "ASYNC-HANDLER-GATEWAY";

    /**
     * If your verticle does a simple, synchronous start-up then override this method and put your start-up
     * code in here.
     *
     * @throws Exception
     */
    @Override
    public void start() throws Exception {
        super.start();

        vertx.eventBus().consumer(ASYNC_HANDLER_GATEWAY,event -> {

            Future<String> future = processAsyncService();

            future.setHandler(event1 -> {
                if (event1.succeeded()) {
                    CustomResponse customResponse = new CustomResponse();
                    LocalDateTime endLocalDateTime = LocalDateTime.now();
                    customResponse.setLegMessage(endLocalDateTime.toString());
                    customResponse.setLegStatus(HttpStatus.OK.value());

                    JsonObject jsonObject = JsonObject.mapFrom(customResponse);
                    event.reply(jsonObject);
                }else{
                    event.reply("500");
                }
            });
        });

        vertx.eventBus().consumer(ASYNC_HANDLER,event -> {

            LocalDateTime localDateTime = LocalDateTime.now();
            System.out.println("startProcess..."+localDateTime.toString());
            for (int i =0;i<30000;i++){
                System.out.println("awesome message..."+event.body().toString());
            }
            LocalDateTime endLocalDateTime = LocalDateTime.now();
            System.out.println("endProcess..."+endLocalDateTime.toString());
        });
    }

    @Override
    public Future<String> processAsyncService() {
        Future<String> stringFuture = Future.future();

        vertx.eventBus().publish(ASYNC_HANDLER,"publishing message");

        String s = "200";

        stringFuture.complete(s);
        return stringFuture;
    }
}
