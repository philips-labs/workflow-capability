package com.philips.healthsuite.workflowcapability.core.demos;

import com.philips.healthsuite.workflowcapability.core.fhirresources.FhirDataResources;

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


    public void run() {
        System.out.println("Running FhirStoreInitialization");
        this.wfcUrl = this.env.getProperty("config.wfcUrl");
        this.fhirUrl = this.env.getProperty("config.fhirUrl");
        this.fhirDataResources = new FhirDataResources(this.fhirUrl + "/fhir");

        addTaskSubscription();
        addCarePlanSubscription();
    }


    public void addTaskSubscription() {
        System.out.println("Adding Task Subscription");
        Subscription taskSubscription = new Subscription();
        taskSubscription.setStatus(Subscription.SubscriptionStatus.REQUESTED);
        taskSubscription.setReason("Trigger when a Task is completed");
        taskSubscription.setCriteria("Task?status=completed");
        taskSubscription.setChannel(new Subscription.SubscriptionChannelComponent()
                .setType(Subscription.SubscriptionChannelType.RESTHOOK)
                .setEndpoint(wfcUrl + "/OnTaskChange"));
        fhirDataResources.addResource(taskSubscription);
    }


    public void addCarePlanSubscription() {
        System.out.println("Adding CarePlan Subscription");
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
