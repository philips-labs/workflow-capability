package com.philips.healthsuite.workflowcapability.core.wfcservice;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.philips.healthsuite.workflowcapability.core.fhirresources.FhirDataResources;

import org.apache.jena.base.Sys;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TODO: Add description
 */
@RestController
public class SubscriptionController {
    private FhirDataResources fhirDataResources;
    private FhirContext ctx = FhirContext.forR4();
    private EngineQueryHandler engineQueryHandler;
    @Value("${config.fhirUrl}")
    private String fhirUrl;
    private List<String> taskIdentifiersAlreadySignalledToBpmnEngineAsCompleted = new ArrayList<>();

    /**
     * @throws IOException
     */
    SubscriptionController() throws IOException {
        this.engineQueryHandler = new EngineQueryHandler();
    }

    /**
     *
     */
    @PostConstruct
    public void init() {
        this.fhirDataResources = new FhirDataResources(fhirUrl + "/fhir");
    }

    /**
     * @throws IOException
     */
    @RequestMapping(value = "/OnCarePlanChange", method = RequestMethod.POST)
    void receiveCarePlanSubscribe() throws IOException {
        List<CarePlan> newCarePlans = this.fhirDataResources.getNewCarePlans();
        syncCarePlans(newCarePlans);
    }

    /**
     * @param processID
     * @param returnMessage
     * @param variableName
     * @param taskIdentifier
     * @throws IOException
     * @throws InterruptedException 
     */
    @RequestMapping(value = "/OnRequestChange/{processID}/{returnMessage}/{variableName}/{taskIdentifier}", method = RequestMethod.POST)
    void requestChangeSubscribe(@PathVariable("processID") String processID,
            @PathVariable("returnMessage") String returnMessage,
            @PathVariable("variableName") String variableName,
            @PathVariable("taskIdentifier") String taskIdentifier) throws IOException, InterruptedException {

        String[] query = this.engineQueryHandler.pendingRequests.get(processID).get(returnMessage);
        Resource resource = this.engineQueryHandler.getFhirResource("FHIR(GET):" + query[0], returnMessage, processID,
                variableName, taskIdentifier);
        this.fhirDataResources
                .removeResource((Resource) this.fhirDataResources.getResourceById(query[1], "Subscription"));

        if (resource != null) {

            IParser parser = this.ctx.newJsonParser();
            String resourceString = parser.encodeResourceToString(resource);
            EngineInterface engineInterface = new EngineInterfaceFactory().getEngineInterface("CAMUNDA");
            engineInterface.sendMessage(returnMessage, processID, variableName, resourceString);
            fhirDataResources.completeTaskById(taskIdentifier);

        }
        else {
        
        }
    }

    /**
     * Reads current list of completed FHIR-tasks and checks which of these are not
     * marked as completed BPMN-tasks.
     * Then, it marks the latter as completed.
     *
     * @throws IOException
     */
    @RequestMapping(value = "/OnTaskChange", method = RequestMethod.POST)
    void synchronizeCompletedTasksBetweenFhirAndEngine() throws IOException {
        EngineInterfaceFactory engineInterfaceFactory = new EngineInterfaceFactory();
        EngineInterface engineInterface = engineInterfaceFactory.getEngineInterface("CAMUNDA");
        List<String> completedTasksIds = this.fhirDataResources.getRecentlyCompletedTasksIds(); // Tasks marked in FHIR
        for (String completedTaskIdentifier : completedTasksIds) {
            if (!this.taskIdentifiersAlreadySignalledToBpmnEngineAsCompleted.contains(completedTaskIdentifier)) {
                engineInterface.completeTask(completedTaskIdentifier);
                this.taskIdentifiersAlreadySignalledToBpmnEngineAsCompleted.add(completedTaskIdentifier);
                // this.fhirDataResources.deleteTaskFromFhirStore(completedTask);
            }
        }
    }

    /**
     * Service for marking a FHIR CarePlan as completed.
     *
     * @param carePlanInstanceID
     */
    @RequestMapping(value = "/FinishWorkflow/{carePlanInstanceID}", method = RequestMethod.POST)
    void finishWorkflow(@PathVariable("carePlanInstanceID") String carePlanInstanceID) {
        this.fhirDataResources.markCarePlanAsCompleted(carePlanInstanceID);
        // this.fhirDataResources.removeResource(fhirDataResources.getCarePlanByEngineId(carePlanInstanceID));
    }

    /**
     * @param carePlanInstanceID
     * @param taskID
     * @param taskIdentifier
     * @throws IOException
     */
    @RequestMapping(value = "/RequestReceiveTask/{carePlanInstanceID}/{taskID}/{taskIdentifier}", method = RequestMethod.POST)
    @Async
    void subscribeNewReceiveTask(@PathVariable("carePlanInstanceID") String carePlanInstanceID,
            @PathVariable("taskID") String taskID, @PathVariable("taskIdentifier") String taskIdentifier) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    fhirDataResources.createTask(taskIdentifier, carePlanInstanceID, taskID, "received");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * @param processID
     * @param returnMessage
     * @param variableName
     * @param taskIdentifier
     * @param query
     * @throws IOException
     */
    @RequestMapping(value = "/RequestObservationValue/{processID}/{returnMessage}/{variableName}/{taskIdentifier}", method = RequestMethod.POST)
    @Async

    public void informEngineAboutObservationValue(@PathVariable("processID") String processID,
            @PathVariable("returnMessage") String returnMessage,
            @PathVariable("variableName") String variableName,
            @PathVariable("taskIdentifier") String taskIdentifier,
            @RequestBody String query) {
        final ReentrantLock lock = new ReentrantLock();
        try {
            lock.lock();
            Resource resource = this.engineQueryHandler.getFhirResource(query, returnMessage, processID, variableName,
                    taskIdentifier);
            if (resource != null) {
                IParser parser = this.ctx.newJsonParser();
                String resourceString = parser.encodeResourceToString(resource);
                EngineInterface engineInterface = new EngineInterfaceFactory().getEngineInterface("CAMUNDA");
                engineInterface.sendMessage(returnMessage, processID, variableName, resourceString);
                fhirDataResources.completeTaskById(taskIdentifier);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param carePlanInstanceID
     * @param taskIdentifier
     * @param taskID
     */
    @RequestMapping(value = "/RequestUserTask/{carePlanInstanceID}/{taskID}/{taskIdentifier}", method = RequestMethod.POST)
    void subscribeNewUserTask(
            @PathVariable("carePlanInstanceID") String carePlanInstanceID,
            @PathVariable("taskIdentifier") String taskIdentifier,
            @PathVariable("taskID") String taskID) throws InterruptedException {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    fhirDataResources.createTask(taskIdentifier, carePlanInstanceID, taskID, "received");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /**
     * @param newCarePlans
     * @throws IOException
     */
    void syncCarePlans(List<CarePlan> newCarePlans) throws IOException {
        EngineInterfaceFactory engineInterfaceFactory = new EngineInterfaceFactory();
        EngineInterface engineInterface = engineInterfaceFactory.getEngineInterface("CAMUNDA");
        for (CarePlan newCarePlan : newCarePlans) {
            String carePlanInstanceID = engineInterface.instantiateWorkflow(
                    newCarePlan.getIdentifier().get(0).getValue(), newCarePlan.getSubject().getReference());
            this.fhirDataResources.startCarePlan(newCarePlan, carePlanInstanceID);
        }
    }
}