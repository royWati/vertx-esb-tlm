package ekenya.co.ke.vertxspringtlm.services;


import ekenya.co.ke.vertxspringtlm.dao.LegConfigurationManager;
import ekenya.co.ke.vertxspringtlm.dao.LegManager;
import ekenya.co.ke.vertxspringtlm.dao.wrapper.LegWrapper;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.List;
/**
 * @Version 1.0
 */
public interface TemplateConfiguration {

    Future<JsonObject> createTemplate(LegWrapper legWrapper);
    boolean LegPresent(String legname);
    List<LegManager> allExistingLegs();
    void createLegService(LegManager legManager);
    void createDataTemplate(LegConfigurationManager legConfigurationManager, String fileName);
}
