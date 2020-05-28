package ekenya.co.ke.vertxspringtlm.services;


import ekenya.co.ke.vertxspringtlm.dao.DataStore;
import ekenya.co.ke.vertxspringtlm.dao.LegBody;
import ekenya.co.ke.vertxspringtlm.dao.LegConfigurationManager;
import ekenya.co.ke.vertxspringtlm.dao.LegManager;
import ekenya.co.ke.vertxspringtlm.dao.legbody.MappingFields;
import ekenya.co.ke.vertxspringtlm.dao.legbody.ResponseHandler;
import ekenya.co.ke.vertxspringtlm.dao.redis.TransactionRecorder;
import ekenya.co.ke.vertxspringtlm.dao.wrapper.CustomResponse;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * @Version 1.0
 */
public interface LegProcessor {

    Future<String> processAsyncService();
    Future<JsonObject> INITIATE_LEG_MATRIX(JsonObject requestObject,TemplateConfiguration configuration,
                                           RedisProcessor redisProcessor);



    LegManager GET_LEG_MANAGER(String legName);
    LegConfigurationManager GET_LEG_CONFIG_FILE(String fileName);
    CustomResponse VALIDATED_CLIENT_REQUEST_FIELDS(List<MappingFields> clientFields, JsonObject jsonObject);
    TransactionRecorder GENERATE_TRANSACTION_RECORD (JsonObject jsonObject,RedisProcessor redisProcessor);
    LegBody GET_LEG_BODY(List<LegBody> list, int stepPosition);
    ResponseEntity<String> MAKE_SERVICE_REQUEST(LegBody legBody,long transactionId,RedisProcessor redisProcessor);

    String[] DATA_PARAMETERS(String request);
    String RETRIEVE_DATA_STORE(String[] parameters, List<DataStore> dataStoreList);
    String RETRIEVE_DATA_STORE_V2(String[] parameters, List<DataStore> dataStoreList);
    int GET_HANDLER_ID(List<ResponseHandler> responseHandlers, long transactionId,RedisProcessor redisProcessor);

    ResponseHandler GET_RESPONSE_HANDLER(int handlerId, List<ResponseHandler> handlers);
    JsonObject CREATE_LEG_RESPONSE(LegConfigurationManager legConfigurationManager);
}
