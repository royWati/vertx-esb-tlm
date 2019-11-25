package ekenya.co.ke.vertxspringtlm.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import ekenya.co.ke.vertxspringtlm.dao.DataStore;
import ekenya.co.ke.vertxspringtlm.dao.LegBody;
import ekenya.co.ke.vertxspringtlm.dao.LegConfigurationManager;
import ekenya.co.ke.vertxspringtlm.dao.LegManager;
import ekenya.co.ke.vertxspringtlm.dao.legbody.*;
import ekenya.co.ke.vertxspringtlm.dao.redis.TransactionRecorder;
import ekenya.co.ke.vertxspringtlm.dao.wrapper.CustomResponse;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * @Version 1.0
 */
@Component
public class LegProcessorImpl extends AbstractVerticle implements LegProcessor {

    private String ASYNC_HANDLER = "ASYNC-HANDLER";
    public static String ASYNC_HANDLER_GATEWAY = "ASYNC-HANDLER-GATEWAY";
    public static String INTIATE_TLM_MATRIX ="INITIATE-TLM-MATRIX-ADDRESS";

    private final static Logger logger = Logger.getLogger(LegProcessorImpl.class.getName());

    @Value("${esb.connectionUrl}")
    private String esbConnectionurl;
    @Value("${templates.location}")
    private String templateLocation;

    @Autowired private TemplateConfiguration templateConfiguration;
    @Autowired private RedisProcessor redisProcessor;

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
                    customResponse.setLegMessage(event1.result());
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
            logger.info("startProcess..."+localDateTime.toString());
            for (int i =0;i<30000;i++){
                int j = i+1;
              //  System.out.println("awesome message..."+event.body().toString());
            }
            LocalDateTime endLocalDateTime = LocalDateTime.now();
            logger.info("endProcess..."+endLocalDateTime.toString());
        });

        vertx.eventBus().consumer(INTIATE_TLM_MATRIX,event -> {
            JsonObject jsonObject = new JsonObject(event.body().toString());

            Future<JsonObject> jsonObjectFuture = INITIATE_LEG_MATRIX(jsonObject,
                    templateConfiguration,redisProcessor);

            jsonObjectFuture.setHandler(event1 -> {
                if (event1.succeeded()){
                    event.reply(event1.result());
                }else{
                    CustomResponse customResponse = new CustomResponse();
                    customResponse.setLegMessage("failed to process leg transaction");
                    customResponse.setLegStatus(500);

                    JsonObject j = JsonObject.mapFrom(customResponse);

                    event.reply(j);
                }
            });
        });
    }

    @Override
    public Future<String> processAsyncService() {
        Future<String> stringFuture = Future.future();

        vertx.eventBus().publish(ASYNC_HANDLER,"publishing message");

        JsonObject jsonObject = new JsonObject();
        jsonObject.put("serviceName","cbs-balance-inquiry");
        jsonObject.put("tlmTransactionRefNo", UUID.randomUUID().toString());

        JsonObject requestObject = new JsonObject();
        requestObject.put("phone_number","254708691402");
        requestObject.put("message","this is an awesome message to send right this moment");

        jsonObject.put("requestBody",requestObject);


        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(jsonObject.toString(),headers);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(esbConnectionurl,httpEntity,String.class);

        logger.info("response code..."+responseEntity.getStatusCodeValue());
        logger.info(responseEntity.getBody());
        String s = responseEntity.getBody();

        stringFuture.complete(s);
        return stringFuture;
    }

    /**
     * this is core of the of the TLM.
     *
     * @param requestObject
     * @return
     */
    @Override
    public Future<JsonObject> INITIATE_LEG_MATRIX(JsonObject requestObject,
                                                  TemplateConfiguration templateConfiguration,
                                                  RedisProcessor redisProcessor) {

        Future<JsonObject> future = Future.future();

        // check if the service exists in the system
        String legName = requestObject.getString("legManager");
        JsonObject requestObjectFields = requestObject.getJsonObject("requestFields");

        logger.info("leg name..."+legName);

        LegManager legManager = GET_LEG_MANAGER(legName);

        try {
            logger.info(new ObjectMapper().writeValueAsString(legManager));
        }catch (Exception e ){
            logger.info(e.getMessage());
        }

        if (legManager !=null){
            // leg manager has been found now lets proceed to process the request

            // we load the configuration file of the LegManager
            String fileName = templateLocation+"/"+legManager.getLegname()+"-data.json";
            LegConfigurationManager legConfigurationManager = GET_LEG_CONFIG_FILE(fileName);

            try {
                logger.info(new ObjectMapper().writeValueAsString(legConfigurationManager));
            }catch (Exception e ){
                logger.info(e.getMessage());
            }

            if (legConfigurationManager !=null){

                //check if all client fields that have been requested have been captured
                List<MappingFields> clientFields = legConfigurationManager.getClientRequest();
                CustomResponse fieldValidation = VALIDATED_CLIENT_REQUEST_FIELDS(clientFields,
                        requestObjectFields);

                if (fieldValidation.getLegStatus()==1){
                    /**
                     * this means that the request that was validated in now okay
                     * we will write the application to redis in order
                     * to help in data storage of the application
                     */

                    TransactionRecorder transactionRecorder = GENERATE_TRANSACTION_RECORD(requestObjectFields,
                            redisProcessor);
                   logger.info("transaction record stored..."+transactionRecorder);

                    /**
                         start leg processing at this point.
                         All legs will start and end at a specific point
                        hence we need to coordinate the movement from
                        the first leg to the last leg
                        this will happen using handlers and step Ids
                     */

                    /**
                     * each leg has a starting point that dictates where will start from
                     * this position will keep on changing based on thee configuration set
                     * for the next leg
                     */
                    int position  = legManager.getStartingPoint();

                    /**
                     *this boolean result will enable us tell the loop that we have finally
                     * reached the end of the result hence to stop
                     */
                    boolean finalLeg = false;
                    /**
                     * this boolean result will be used when a leg encounters an error
                     * of any kind. this will enable us make a desicion on whether to continue
                     * processing the reqeust or terminate any point
                     */
                    boolean failedLeg = false;

                    logger.info("leg transaction starting...");
                    while (!finalLeg){

                        LegBody legBody = GET_LEG_BODY(legConfigurationManager.getBody(),position);

                        /**
                         *  the reason for passing the transactionId is because with every look cycle
                         *  the data set changes
                         */
                        ResponseEntity<String> responseEntity = MAKE_SERVICE_REQUEST(legBody,
                                transactionRecorder.getTransactionId(),redisProcessor);


                        logger.info("response from the request made..."+responseEntity);

                        if (responseEntity.getStatusCode().is2xxSuccessful()){

                            logger.info("this was a successful transaction...");
                            /*
                             since all requests from the esb are customized with fail overs
                             we check for esbStatus if the given value is 200
                             to indicate that the message is okay for processing
                             if not then we treat it as a failure and initiate the fail
                             mechanism

                             */

                            JsonObject responseObject = new JsonObject(Objects.requireNonNull(responseEntity.getBody()));

                             logger.info("this was a successful transaction...body.."+responseObject.toString());
                            int esbStatus = responseObject.getInteger("esbStatus");

                            boolean wasAnEsbSuccess = false;
                            int handlerId = 0;

                            if (esbStatus==200){
                                logger.info("this was a successful esb transaction...");
                                wasAnEsbSuccess = true;

                                handlerId = GET_HANDLER_ID(legBody.getResponse().getSuccess(),
                                        transactionRecorder.getTransactionId(),redisProcessor);
                            }else {
                                logger.info("this was a failed esb transaction...");
                                handlerId =GET_HANDLER_ID(legBody.getResponse().getFail(),
                                        transactionRecorder.getTransactionId(),redisProcessor);
                            }

                            logger.info("esbsuccess..."+wasAnEsbSuccess+"...handler..."+handlerId);

                            if (wasAnEsbSuccess && handlerId !=0){

                                logger.info("esb success and handler found...");
                                ResponseHandler handler = GET_RESPONSE_HANDLER(handlerId,
                                        legBody.getResponse().getSuccess());

                                position = handler.getStepId();
                                finalLeg = handler.isFinalLeg();
                            }else if (!wasAnEsbSuccess && handlerId !=0){
                                logger.info("esb fail and handler found...");
                                ResponseHandler handler = GET_RESPONSE_HANDLER(handlerId,
                                        legBody.getResponse().getFail());

                                try {
                                    System.out.println(new ObjectMapper().
                                            writeValueAsString(handler));
                                }catch (Exception e){

                                }
                                position = handler.getStepId();
                                finalLeg = handler.isFinalLeg();
                                try {
                                    System.out.println(new ObjectMapper().
                                            writeValueAsString(handler));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }else{
                                logger.info("serious failure...");
                                CustomResponse customResponse = new CustomResponse();
                                customResponse.setLegStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                                customResponse.setLegMessage("tlm transaction processing failed");

                                JsonObject jsonObject = JsonObject.mapFrom(customResponse);

                                future.complete(jsonObject);
                            }

                        }else{

                            /**
                             * TODO: ADD Notification and failed monitoring at this point
                             */
                            CustomResponse customResponse = new CustomResponse();
                            customResponse.setLegStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                            customResponse.setLegMessage("tlm transaction processing failed");

                            JsonObject jsonObject = JsonObject.mapFrom(customResponse);

                            future.complete(jsonObject);
                        }
                    }

                    if (!failedLeg){
                        JsonObject jsonObject= CREATE_LEG_RESPONSE(legConfigurationManager);
                        jsonObject.put("legStatus",200);
                        jsonObject.put("legMessage","leg processed successfully");
                        future.complete(jsonObject);
                    }
                }else{
                    JsonObject jsonObject = JsonObject.mapFrom(fieldValidation);
                    future.complete(jsonObject);
                }

            }else{

                // the configuration file was not found or encounted an error
                // hence we throw an error on the same

                CustomResponse customResponse = new CustomResponse();
                customResponse.setLegStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
                customResponse.setLegMessage("leg manager template not found");

                JsonObject jsonObject = JsonObject.mapFrom(customResponse);

                future.complete(jsonObject);
            }

        }else{
            logger.info("failed to locate leg manager");
            CustomResponse customResponse = new CustomResponse();
            customResponse.setLegStatus(HttpStatus.NOT_FOUND.value());
            customResponse.setLegMessage("leg manager not found");

            JsonObject jsonObject = JsonObject.mapFrom(customResponse);

            future.complete(jsonObject);
        }
        return future;
    }

    @Override
    public LegManager GET_LEG_MANAGER(String legName) {

        logger.info("leg name..."+legName);
     //   LegManager legManager=new LegManager();
        List<LegManager> legManagerList = new ArrayList<>();
        List<LegManager> list = templateConfiguration.allExistingLegs();
        for (LegManager leg : list){
            if (leg.getLegname().toLowerCase().equals(legName)){
            //    legManager =leg;
                legManagerList.add(leg);
            }
        }
        return legManagerList.size() > 0 ? legManagerList.get(0) : null;
    }

    @Override
    public LegConfigurationManager GET_LEG_CONFIG_FILE(String fileName) {
        logger.info("file name..."+fileName);
        LegConfigurationManager legConfigurationManager = new LegConfigurationManager();
        InputStream inputStream = null;
        try {
            inputStream =new FileInputStream(fileName);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String line = bufferedReader.readLine();

            StringBuilder stringBuilder = new StringBuilder();

            while (line !=null){
                stringBuilder.append(line);
                line = bufferedReader.readLine();
            }

            String contents = stringBuilder.toString();

            legConfigurationManager = new Gson().fromJson(contents,LegConfigurationManager.class);

            System.out.println(new ObjectMapper().writeValueAsString(legConfigurationManager));

            return legConfigurationManager;
        } catch (Exception e) {
            logger.info(e.getMessage());
            return null;
        }finally {
            if(inputStream !=null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.info(e.getMessage());
                }
            }
        }


    }

    @Override
    public CustomResponse VALIDATED_CLIENT_REQUEST_FIELDS(List<MappingFields> clientFields, JsonObject jsonObject) {
        CustomResponse customResponse = new CustomResponse();
        StringBuilder missingFields  = new StringBuilder();
        missingFields.append("request requires the following fields: ");
        StringJoiner sj = new StringJoiner(",", "[", "]");
        boolean result = true;
        for (MappingFields mappingFields : clientFields){
            if (!jsonObject.containsKey(mappingFields.getOriginatorField())){
                result = false;
                sj.add(mappingFields.getOriginatorField());
            }
        }

        if (result){
            customResponse.setLegStatus(1);
        }else{
            String message = missingFields.append(sj.toString()).toString();
            customResponse.setLegMessage(message);
            customResponse.setLegStatus(406);
        }

        return customResponse;
    }

    @Override
    public TransactionRecorder GENERATE_TRANSACTION_RECORD(JsonObject jsonObject,RedisProcessor redisProcessor) {
        TransactionRecorder transactionRecorder = new TransactionRecorder();
        long timeStampMillis = System.nanoTime();
        transactionRecorder.setTransactionId(timeStampMillis);
        String transRefNo = UUID.randomUUID().toString();
        transactionRecorder.setTransactionRefNo(transRefNo);

        List<DataStore> dataStores = new ArrayList<>();
        /**
         * the first data store will contain the client request body
         */
        DataStore dataStore = new DataStore();
        dataStore.setJsonObject(jsonObject.toString());
        dataStore.setStore("clientRequest");
        dataStore.setStoreId(0);

        dataStores.add(dataStore);
        transactionRecorder.setDataStoreList(dataStores);

        return redisProcessor.createTransactionRecorder(transactionRecorder);
    }

    @Override
    public LegBody GET_LEG_BODY(List<LegBody> list, int stepPosition) {
        LegBody legBody = new LegBody();
        for (LegBody l : list){
            if (stepPosition==l.getStepId())
                legBody = l;
        }
        return legBody;
    }

    @Override
    public ResponseEntity<String> MAKE_SERVICE_REQUEST(LegBody legBody,
                                                       long transactionId,
                                                       RedisProcessor redisProcessor) {
        logger.info("MAKE_SERVICE_REQUEST method execution ..1");

        JsonObject jsonObject = new JsonObject();
        jsonObject.put("service",legBody.getServiceName());
        jsonObject.put("transactionId",transactionId);

        /***
         * request body show look as follow
         * {
         *     "serviceName":"legBody.getServiceName()",
         *     "transactionId":transactionId
         * }
         */
        RestTemplate restTemplate = new RestTemplate();

        /**
         *second step. creating the request body of needed to make the request.
         * the data store present in redis will guide us to where each request
         * should be able to get its data from
         */

        // check if all the values are okay and have data too

        boolean okayValues = true;

        TransactionRecorder transactionRecorder = redisProcessor.findTransactionRecord(transactionId);

        try {
            logger.info("trans details..."+new ObjectMapper().writeValueAsString(transactionRecorder));
        }catch (Exception e){
            logger.info(e.getMessage());
        }

        List<DataStore> dataStoreList = transactionRecorder.getDataStoreList();

        JsonObject jsonObjectRequestBody = new JsonObject();

        for (LegRequest legRequest : legBody.getRequestFields()){
            String[] parameters = DATA_PARAMETERS(legRequest.getDataSource());
            logger.info("MAKE_SERVICE_REQUEST method execution ..3");
            String value = RETRIEVE_DATA_STORE(parameters,dataStoreList);

            if ("".equals(value)){
                okayValues = false;
            }else {
                jsonObjectRequestBody.put(legRequest.getMappingField(),value);
            }

        }

        logger.info("MAKE_SERVICE_REQUEST method execution ..4");
        DataStore store = new DataStore();

        if (okayValues){
            // add the fixed fields for the request
            for (FixedFields fixedFields : legBody.getFixedFields()){
                jsonObjectRequestBody.put(fixedFields.getField(),fixedFields.getValue());
            }

            logger.info("MAKE_SERVICE_REQUEST method execution ..5");
            // store this request data in case its needed by another leg for transaction processing
            DataStore dataStore = new DataStore();
            dataStore.setStoreId(legBody.getStepId());
            dataStore.setStore("request");
            dataStore.setJsonObject(jsonObjectRequestBody.toString());

            dataStoreList.add(dataStore);

            // add the jsonRequestBody to the jsonObject to create a complete request body
            jsonObject.put("request",jsonObjectRequestBody);

            /***
             * request body will look as follows
             * {
             *     "service":"legBody.getServiceName()",
             *     "transactionId":transactionId,
             *     "request":{
             *         "key1":"value1",
             *         "key2":"value2"
             *     }
             * }
             */

            //update the redis transactionRecord before initiating the transaction...
            transactionRecorder.setDataStoreList(dataStoreList);
            redisProcessor.createTransactionRecorder(transactionRecorder);

            //create the request entity
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(jsonObject.toString(),headers);
            System.out.println("MAKE_SERVICE_REQUEST method execution ..6");

            /**
             * the reason for creating this list is because we do know what response
             * awaits us, either success or failure...
             * hence we load the data to list and retrieve the first record to be processed
             */
            List<ResponseEntity<String>> entityList = new ArrayList<>();
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(esbConnectionurl,
                        entity,String.class);
                entityList.add(response);
            }catch (Exception e){
                e.printStackTrace();
                ResponseEntity<String> response =  new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                entityList.add(response);
            }
            /**
             * the request has been processed at this point
             */
            ResponseEntity<String> responseEntity = entityList.get(0);
            logger.info("MAKE_SERVICE_REQUEST method execution ..7");
            logger.info("MAKE_SERVICE_REQUEST method execution ..7:1__response.."+
                    responseEntity.getStatusCode());

            if (responseEntity.getStatusCode().is2xxSuccessful()){
                // request was successfully processed by the ESb
                JsonObject responseObject = new JsonObject(Objects.requireNonNull(responseEntity.getBody()));

                store.setJsonObject(responseObject.toString());
                store.setStore("response");
                store.setStoreId(legBody.getStepId());

                dataStoreList.add(store);
                // update redis with the new data set
                transactionRecorder.setDataStoreList(dataStoreList);

                redisProcessor.createTransactionRecorder(transactionRecorder);

                return responseEntity;
            }else{
                return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }else{
            return new ResponseEntity<String>(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
    }

    /**
     * most of the data has to be stored using a sequence
     * __stepId__dataStoredRefrence__key
     * dataStoredRefrence can either be clientRequest, request, or response
     * the key the json key that we will use to pull the data
     * @param request
     * @return
     */
    @Override
    public String[] DATA_PARAMETERS(String request) {
        return request.split("__");
    }

    /***
     * once the array has been build, it should contain four values
     * @param parameters
     * @return
     */
    @Override
    public String RETRIEVE_DATA_STORE(String[] parameters,List<DataStore> dataStoreList) {

      //  TransactionRecorder transactionRecorder = redisProcessor.findTransactionRecord(transactionId);

        if (dataStoreList.size()>0){

            String stepId = parameters[1];
            String storeName = parameters[2];
            String key = parameters[3];

            logger.info("key..."+key);
            String value = "";

            for (DataStore dataStore : dataStoreList){
                JsonObject storeObject = new JsonObject(dataStore.getJsonObject());

                if (dataStore.getStoreId()==Integer.parseInt(stepId) &&
                        storeName.equals(dataStore.getStore())){

                    value = String.valueOf(storeObject.getValue(key));
                }
            }
            return value;
        }else {
            // TODO :: THROW EXCEPTIONS AT THIS POINT AND CATCH THEM AT THE POINT OF PROCESSIN
            return "";
        }


    }

    @Override
    public int GET_HANDLER_ID(List<ResponseHandler> responseHandlers, long transactionId, RedisProcessor redisProcessor) {
        TransactionRecorder transactionRecorder = redisProcessor.findTransactionRecord(transactionId);

        int id =0;
        logger.info("response handler function execution");
        /**
         * we look through all the response possibilites present in the list of response
         * of the template
         *
         * for each response we loop through the routing factors present and set by the developer in
         * order to establish the next leg id
         */
        for (ResponseHandler responseHandler : responseHandlers){

            boolean allMatches = true;

            for (Factors factors : responseHandler.getRoutingFactors()){
                StringBuilder builder = new StringBuilder();
                builder.append(factors.getDataSource()).append("__").append(factors.getData());
                System.out.println(builder.toString());

                String[] parameters =DATA_PARAMETERS(builder.toString());
                String data = RETRIEVE_DATA_STORE(parameters,transactionRecorder.getDataStoreList());
                System.out.println("data.."+data+" .. value.."+factors.getValue());
                if (!data.equals(factors.getValue())) allMatches = false;
            }
            if (allMatches) id = responseHandler.getHandlerId();
        }

        return id;
    }

    @Override
    public ResponseHandler GET_RESPONSE_HANDLER(int handlerId, List<ResponseHandler> handlers) {
        logger.info("we got to get response handler...");
        ResponseHandler responseHandler = new ResponseHandler();

        for (ResponseHandler r : handlers){
            if (handlerId==r.getHandlerId()) responseHandler = r;
        }
        return responseHandler;
    }

    @Override
    public JsonObject CREATE_LEG_RESPONSE(LegConfigurationManager legConfigurationManager) {
        JsonObject jsonObject = new JsonObject();

        List<FixedFields> fixedFieldsList = legConfigurationManager.getFixedFields();

        for (FixedFields fixedFields: fixedFieldsList){
            jsonObject.put(fixedFields.getField(),fixedFields.getValue());
        }

        //TODO ADD_CUSTOMIZED RESPONSE AT THIS POINT
        return jsonObject;
    }


}
