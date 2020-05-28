package ekenya.co.ke.vertxspringtlm.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.vertx.core.json.Json;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Logger;

public class TempClassImpl  {
    private final static Logger logger = Logger.getLogger(TempClassImpl.class.getName());
  //  @Override
    public static JsonObject createRequestBody(String sp_name, JsonObject portalRequest) {
        // read from file
        String fileName = "configs/sp-list.json";
        JsonElement getSpList = readConfigFiles(fileName);

        logger.info("element : "+getSpList);
        logger.info("sp name : "+sp_name);

        JsonArray sp_array = getSpList.getAsJsonArray();

        JsonObject templateObject = new JsonObject();

        for (JsonElement element : sp_array){
            JsonObject object = element.getAsJsonObject();

            if (object.get("procedureName").getAsString().equals(sp_name)){
                templateObject = object;
                break;
            }
        }

        logger.info("tempalate object : "+templateObject);

        /**
         * @TemplateObject
         *
         * {
         *     "procedureName": "SP_TEST",
         *     "parameters": [
         *       {
         *         "originatorField": "my_name",
         *         "destinationField": "IV_FIELD2"
         *       },
         *       {
         *         "originatorField": "your_name",
         *         "destinationField": "IV_FIELD3"
         *       }
         *     ]
         *   }
         */

        JsonObject transactionDetails = portalRequest.getAsJsonObject("data").getAsJsonObject("transaction_details");

        JsonObject requestObject = new JsonObject();

        requestObject.addProperty("procedureName", sp_name);

        JsonObject parameterObject = new JsonObject();

        JsonArray parameters = templateObject.getAsJsonArray("parameters");

        parameters.forEach(jsonElement -> {
            JsonObject object = (JsonObject) jsonElement;

            String originatorField = object.get("originatorField").getAsString();

            String value = transactionDetails.get(originatorField).getAsString();

            parameterObject.addProperty(object.get("destinationField").getAsString(),value);

        });

        requestObject.add("parameters",parameterObject);


        return requestObject;
    }

 //   @Override
    public static JsonElement readConfigFiles(String fileName) {

        // file directory for the sp list

        JsonElement jsonElement = null;
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
            JsonParser jsonParser = new JsonParser();
            jsonElement =jsonParser.parse(new InputStreamReader(Objects.requireNonNull(is),
                    StandardCharsets.UTF_8));
        }catch (Exception e ){
            logger.info("failed to load file system "+fileName);
        }finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    logger.info(e.getMessage());
                }
            }
        }
        return jsonElement;
    }


    public static void main(String[] args){

        String portalRequest = "{\n" +
                "  \"txntimestamp\": \"68783049845859\",\n" +
                "  \"xref\": \"0\",\n" +
                "  \"data\": {\n" +
                "    \"transaction_details\": {\n" +
                "      \"first_name\": \"john\",\n" +
                "      \"middle_name\": \"njoroge\",\n" +
                "      \"last_name\": \"karu\",\n" +
                "      \"dateofbirth\": \"11041992\",\n" +
                "      \"mobile_number\": \"254704113452\",\n" +
                "      \"password_hash\": \"sdsdwedsdvdvvdfsadDFDEDF \",\n" +
                "      \"language\": \"en\",\n" +
                "      \"channel\": \"APP\",\n" +
                "      \"gender\": \"f\",\n" +
                "      \"currency\": \"kenya\",\n" +
                "      \"branchcode\": \"Lower\",\n" +
                "      \"hascoreaccount\": 1,\n" +
                "      \"txnlimitamount\": \"Nairobi Kenya\",\n" +
                "      \"customerid\": \"customer\",\n" +
                "      \"email\": \"infojohnka@gmail.com\",\n" +
                "      \"id_number\": \"29976628\",\n" +
                "      \"imsi\": \"3456453456\",\n" +
                "      \"imei\": \"cdsfewevqfeqre34ewd3\",\n" +
                "      \"approved\": 1,\n" +
                "      \"partialregistration\": 1,\n" +
                "      \"phonenumber\": \"254704113452\",\n" +
                "      \"accounttype\": \"F\",\n" +
                "      \"accountdescription\": \"n/a\",\n" +
                "      \"created_by\": \"\",\n" +
                "      \"accountstatus\": \"active\",\n" +
                "      \"req_type\": \"register\",\n" +
                "      \"transaction_type\": \"CUSTOMERS\"\n" +
                "    },\n" +
                "    \"channel_details\": {\n" +
                "      \"channel_key\": \"123456\",\n" +
                "      \"host\": \"127.0.0.1\",\n" +
                "      \"geolocation\": \"android kit kat\",\n" +
                "      \"user_agent_version\": \"android kit kat\",\n" +
                "      \"user_agent\": \"android\",\n" +
                "      \"client_id\": \"EKENYA\",\n" +
                "      \"channel\": \"WEB\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        String dbResponse = "{\n" +
                "  \"status\": 200,\n" +
                "  \"message\": \"database execution was successful\",\n" +
                "  \"data\": [\n" +
                "    {\n" +
                "      \"name\": \"john karu\",\n" +
                "      \"mobile\": \"254752525251\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        String sp_name = "SP_TEST";

        JsonObject portalObject = new JsonParser().parse(portalRequest).getAsJsonObject();
        JsonObject dbObject = new JsonParser().parse(dbResponse).getAsJsonObject();

        JsonObject requestObject = createResponse(portalObject, dbObject);

        logger.info(requestObject.toString());
    }

    public static JsonObject createResponse(JsonObject requestBody, JsonObject responseFromDb){

        // load my generic template
        JsonElement jsonElement = readConfigFiles("configs/generic-response.json");

        JsonObject genericObject = jsonElement.getAsJsonObject();

        genericObject.addProperty("xref",requestBody.get("xref").getAsString());
        genericObject.addProperty("txntimestamp",requestBody.get("txntimestamp").getAsString());

        // check from the database response inorder to build the object

        int status = responseFromDb.get("status").getAsInt();
        String message = responseFromDb.get("message").getAsString();

        JsonArray data = new JsonArray();
        try {
             data = responseFromDb.getAsJsonArray("data");
        }catch (Exception e){
            logger.info("empty array");
        }
        JsonObject payload = genericObject.getAsJsonObject("payload");
        payload.addProperty("status",status);
        payload.addProperty("message",message);
        payload.add("data",data);

        genericObject.add("payload",payload);

        JsonObject requestData = requestBody.getAsJsonObject("data");

        logger.info(requestData.toString());

        requestData.add("channel_details",requestData.getAsJsonObject("channel_details"));
        requestData.add("transaction_details",requestData.getAsJsonObject("transaction_details"));

        genericObject.add("data",requestData);

        return genericObject;
    }
}

