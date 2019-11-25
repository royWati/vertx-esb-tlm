package ekenya.co.ke.vertxspringtlm.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import ekenya.co.ke.vertxspringtlm.dao.LegConfigurationManager;
import ekenya.co.ke.vertxspringtlm.dao.LegManager;
import ekenya.co.ke.vertxspringtlm.dao.wrapper.CustomResponse;
import ekenya.co.ke.vertxspringtlm.dao.wrapper.LegWrapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
/**
 * @Version 1.0
 */
@Component
public class TemplateConfigurationImpl extends AbstractVerticle implements TemplateConfiguration {

    private final static Logger logger = Logger.getLogger(TemplateConfigurationImpl.class.getName());

    public static final String CREATE_TLM_TEMPLATE = "CREATE-TLM-TEMPLATE";


    /**
     * If your verticle does a simple, synchronous start-up then override this method and put your start-up
     * code in here.
     *
     * @throws Exception
     */
    @Override
    public void start() throws Exception {
        super.start();

        vertx.eventBus().consumer(CREATE_TLM_TEMPLATE,event -> {
            String s = event.body().toString();
            LegWrapper legWrapper = new Gson().fromJson(s,LegWrapper.class);

            Future<JsonObject> future = createTemplate(legWrapper);

            future.setHandler(event1 -> {

                if (future.isComplete()){
                    event.reply(event1.result());
                }else{
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.put("legStatus",500);
                    jsonObject.put("legMessage","service currently unavailable");
                }
            });
        });
    }

    @Value("${templates.location}")
    private String templateLocation;
    @Value("${templates.leg-manager-file}")
    private String legManagerFile;


    @Override
    public Future<JsonObject> createTemplate(LegWrapper legWrapper) {
        Future<JsonObject> future = Future.future();

        CustomResponse response = new CustomResponse();
        response.setLegMessage("service updated successfully");
        response.setLegStatus(HttpStatus.OK.value());


        if (!LegPresent(legWrapper.getLegManager().getLegname())){
            createLegService(legWrapper.getLegManager());
            response.setLegMessage("service leg created successfully");
        }
        String dataFile = legWrapper.getLegManager().getLegname()+"-data.json";
        createDataTemplate(legWrapper.getConfiguration(),dataFile);

        JsonObject jsonObject = JsonObject.mapFrom(response);
        future.complete(jsonObject);

        return future;
    }

    @Override
    public boolean LegPresent(String legname) {
        List<LegManager> list = allExistingLegs();

        boolean found = false;

        for(LegManager legManager : list){
            if (legManager.getLegname().toLowerCase().equals(legname)) found = true;
        }
        return found;
    }

    @Override
    public List<LegManager> allExistingLegs() {



        InputStream is = null;
        List<LegManager> data =new ArrayList<>();

        try {


            is = new FileInputStream(templateLocation+legManagerFile);
            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement = jsonParser.parse(new InputStreamReader(Objects.requireNonNull(is), StandardCharsets.UTF_8));

            Gson gson = new Gson();
            data = gson.fromJson(jsonElement, new TypeToken<List<LegManager>>() {
            }.getType());

        } catch (FileNotFoundException e) {
            logger.info(e.getMessage());
        }finally {
            if (is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    logger.info(e.getMessage());
                }
            }
        }
        return data;
    }

    @Override
    public void createLegService(LegManager legManager) {
        File file = new File(templateLocation);

        if (!file.exists()){
            if (file.mkdir()){
                System.out.println("folder created");
                String servicesFile ="[]";
                FileWriter fileWriter = null;
                try {
                    fileWriter= new FileWriter(templateLocation+legManagerFile);
                    fileWriter.write(servicesFile);
                    fileWriter.flush();
                    logger.info("leg-managers.json file created");
                } catch (Exception e) {
                    logger.info(e.getMessage());
                }finally {
                    if (fileWriter != null){
                        try {
                            fileWriter.close();
                        } catch (IOException e) {
                            logger.info(e.getMessage());
                        }
                    }
                }
            }
        }

        List<LegManager> list = allExistingLegs();
        list.add(legManager);
        FileWriter fileWriter = null;
        try {
            String getListAsString = new ObjectMapper().writeValueAsString(list);
            fileWriter= new FileWriter(templateLocation+legManagerFile);
            System.out.println(getListAsString);
            fileWriter.write(getListAsString);
            fileWriter.flush();
            logger.info("leg manager updated");
        } catch (Exception e) {
            logger.info(e.getMessage());
        }finally {
            if(fileWriter !=null){
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    logger.info(e.getMessage());
                }
            }
        }
    }

    @Override
    public void createDataTemplate(LegConfigurationManager legConfigurationManager, String fileName) {


        String storage = templateLocation+"/"+fileName;
        FileWriter fileWriter = null;
        try {
            String dataTemplate = new ObjectMapper().writeValueAsString(legConfigurationManager);

            logger.info(dataTemplate);
            fileWriter = new FileWriter(storage);
            fileWriter.write(dataTemplate);
            fileWriter.flush();

            logger.info("data template created");
        } catch (Exception e) {
            logger.info(e.getMessage());
        }finally {
            if (fileWriter !=null){
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    logger.info(e.getMessage());
                }
            }
        }
    }
}
