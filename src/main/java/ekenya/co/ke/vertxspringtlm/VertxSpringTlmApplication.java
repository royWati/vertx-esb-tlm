package ekenya.co.ke.vertxspringtlm;

import ekenya.co.ke.vertxspringtlm.controller.LegManagerVerticle;
import ekenya.co.ke.vertxspringtlm.controller.TemplateGeneratorVerticle;
import ekenya.co.ke.vertxspringtlm.services.LegProcessor;
import ekenya.co.ke.vertxspringtlm.services.LegProcessorImpl;
import ekenya.co.ke.vertxspringtlm.services.TemplateConfigurationImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
// import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.PostConstruct;

/**
 * @Version 1.0
 */
@SpringBootApplication
// @EnableEurekaClient
@ComponentScan(basePackages = { "ekenya.co.ke" })
@EnableCaching
public class VertxSpringTlmApplication {

    @Autowired private LegManagerVerticle legManagerVerticle;
    @Autowired private TemplateGeneratorVerticle templateGeneratorVerticle;
    @Autowired private LegProcessorImpl legProcessor;
    @Autowired private TemplateConfigurationImpl templateConfiguration;


    public static void main(String[] args) {
        SpringApplication.run(VertxSpringTlmApplication.class, args);
    }

    @PostConstruct
    public void deployVerticles(){

        DeploymentOptions deploymentOptions = new DeploymentOptions();
        deploymentOptions.setWorker(true);
        deploymentOptions.setWorkerPoolSize(20);
    //    deploymentOptions.setInstances(5);
        deploymentOptions.setMultiThreaded(true);

        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(templateGeneratorVerticle);
        vertx.deployVerticle(legManagerVerticle);
        vertx.deployVerticle(legProcessor,deploymentOptions);
        vertx.deployVerticle(templateConfiguration);
    }

}
