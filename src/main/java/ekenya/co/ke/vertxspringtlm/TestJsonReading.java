package ekenya.co.ke.vertxspringtlm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.javafx.logging.Logger;
import io.vertx.core.json.Json;

import java.util.StringJoiner;

/**
 * @Author munialo.roy@ekenya.co.ke
 */
public class TestJsonReading {

    public static void main(String... args){

        String NAME = "Dennis";
        String CURRENCY = "KES";
        String AMOUNT = "200,000";
        String jsonObject = "{\"NAME\":\"Dennis\",\"CURRENCY\":\"KES\",\"AMOUNT\":\"200,000\"}";

        JsonObject object = new JsonParser().parse(jsonObject).getAsJsonObject();

        System.out.println(object.toString());

        String message = "Dear $_NAME , your current balance is $_CURRENCY $_AMOUNT";

        StringJoiner b = new StringJoiner(" ","","");

        String [] splitMessage = message.split(" ");

        for (String s : splitMessage){


            if (s.contains("$_")){
                String[] splitString = s.split("_");

                String value = object.get(splitString[1]).getAsString();

                b.add(value);
            }else{
                b.add(s);
            }
        }
        System.out.println(b.toString());

//        String json = "{\n" +
//                "    \"CI3499V_GetAuthorizedResult\": {\n" +
//                "        \"UniqueId\": \"P888888$X*38**8a$y\",\n" +
//                "        \"Tun\": {\n" +
//                "            \"TrxUserSn\": 0,\n" +
//                "            \"TrxDate\": \"0001-01-01T00:00:00\",\n" +
//                "            \"TrxUnit\": 0,\n" +
//                "            \"TunInternalSn\": 0\n" +
//                "        },\n" +
//                "        \"Result\": {\n" +
//                "            \"Type\": \"Success\",\n" +
//                "            \"Message\": \"SUCCESSFUL TRANSACTION.\",\n" +
//                "            \"StackTrace\": \"\",\n" +
//                "            \"Id\": 282\n" +
//                "        }\n" +
//                "    },\n" +
//                "    \"esbStatus\": 200,\n" +
//                "    \"esbMessage\": \"request processed successfully\",\n" +
//                "    \"esbTransactionId\": \"dcd35a48-f229-481c-acdf-0f9280b45d90\"\n" +
//                "}";
//
//
//        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
//
//        String key = "$CI3499V_GetAuthorizedResult_@_$Tun_@_TrxDate";
//
//        JsonObject activeJsonObject = jsonObject;
//
//        String[] keyArray = key.split("_@_");
//        String finalValue = keyArray[keyArray.length-1];
//
//        for (String s : keyArray){
//            if (s.contains("$")){
//                System.out.println(s);
//                String keyName = s.replace("$","");
//
//                activeJsonObject = activeJsonObject.getAsJsonObject(keyName);
//            }
//        }
//        System.out.println(activeJsonObject);
//        System.out.println(activeJsonObject.get(finalValue).getAsString());
    }
}
