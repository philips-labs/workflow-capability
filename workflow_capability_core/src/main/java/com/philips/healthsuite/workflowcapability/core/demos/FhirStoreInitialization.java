package com.philips.healthsuite.workflowcapability.core.demos;

import com.philips.healthsuite.workflowcapability.core.WfcServiceApplication;
import com.philips.healthsuite.workflowcapability.core.fhirresources.FhirDataResources;

import java.util.logging.Logger;

import org.apache.jena.base.Sys;
import org.hl7.fhir.r4.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;


/**
 * Adds required subscriptions.
 * - Task
 * - CarePlan
 */
@Component
public class FhirStoreInitialization {
    @Autowired
    private Environment env;
    private String wfcUrl;
    private String fhirUrl;
    private FhirDataResources fhirDataResources;

    Logger logger =  Logger.getLogger(WfcServiceApplication.class.getName());
    public void run() {

        logger.info("Running FhirStoreInitialization");
        this.wfcUrl = this.env.getProperty("config.wfcUrl");
        this.fhirUrl = this.env.getProperty("config.fhirUrl");
        this.fhirDataResources = new FhirDataResources(this.fhirUrl + "/fhir");

        addTaskSubscription();
        addCarePlanSubscription();
        // addObservationValueChangeSubscription();
    }


    public void addTaskSubscription() {
        logger.info("Adding Task Subscription");
        Subscription taskSubscription = new Subscription();
        taskSubscription.setStatus(Subscription.SubscriptionStatus.REQUESTED);
        taskSubscription.setReason("Trigger when a Task is completed");
        taskSubscription.setCriteria("Task?status=completed");
        taskSubscription.setChannel(new Subscription.SubscriptionChannelComponent()
                .setType(Subscription.SubscriptionChannelType.RESTHOOK)
                .setEndpoint(wfcUrl + "/OnTaskChange"));
        fhirDataResources.addResource(taskSubscription);
    }

    // public void addObservationValueChangeSubscription() {
    //     Subscription observationValueChangeSubscription = new Subscription();
    //     observationValueChangeSubscription.setStatus(Subscription.SubscriptionStatus.REQUESTED);
    //     observationValueChangeSubscription.setReason("Trigger when the value of an Observation changes and is greater than zero");
    //     observationValueChangeSubscription.setCriteria("Observation?status=final");
    
    //     // Setting up the channel as per the previous example
    //     observationValueChangeSubscription.setChannel(new Subscription.SubscriptionChannelComponent()
    //             .setType(Subscription.SubscriptionChannelType.RESTHOOK)
    //             .setEndpoint(wfcUrl + "/OnObservationValueChange"));
    
    //     fhirDataResources.addResource(observationValueChangeSubscription);
    // }
    
    public void addCarePlanSubscription() {
        logger.info("Adding CarePlan Subscription");
        Subscription carePlanSubscription = new Subscription();
        carePlanSubscription.setStatus(Subscription.SubscriptionStatus.REQUESTED);
        carePlanSubscription.setReason("Trigger when a new CarePlan is created");
        carePlanSubscription.setCriteria("CarePlan?category=WorkflowCapability");
        carePlanSubscription.setChannel(new Subscription.SubscriptionChannelComponent()
                .setType(Subscription.SubscriptionChannelType.RESTHOOK)
                .setEndpoint(wfcUrl + "/OnCarePlanChange"));
        fhirDataResources.addResource(carePlanSubscription);
    }
}
