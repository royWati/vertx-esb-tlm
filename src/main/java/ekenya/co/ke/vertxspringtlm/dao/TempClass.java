package ekenya.co.ke.vertxspringtlm.dao;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public interface TempClass {

    JsonObject createRequestBody(String sp_name, JsonObject portalRequest);

    JsonElement readConfigFiles(String fileName);
}
