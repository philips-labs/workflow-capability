package com.philips.healthsuite.workflowcapability.core.wfcservice;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

import com.philips.healthsuite.workflowcapability.core.fhirresources.FhirDataResources;

import org.apache.jena.sparql.function.library.leviathan.log;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

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
    Logger logger = Logger.getLogger(SubscriptionController.class.getName());
    private List<String> tasksToUpdate = new ArrayList<>();

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
     * @param isInterrupting
     * @throws IOException
     * @throws InterruptedException
     */
    @RequestMapping(value = "/OnRequestChange/{processID}/{returnMessage}/{variableName}/{taskIdentifier}/{isInterrupting}", method = RequestMethod.POST)
    void requestChangeSubscribe(@PathVariable("processID") String processID,
            @PathVariable("returnMessage") String returnMessage,
            @PathVariable("variableName") String variableName,
            @PathVariable("taskIdentifier") String taskIdentifier,
            @PathVariable("isInterrupting") boolean isInterrupting) throws IOException, InterruptedException {
        String[] query = this.engineQueryHandler.pendingRequests.get(processID).get(returnMessage);
        logger.info("Query from subscription is: " + query[0]);
        sendDataToEngineAndUpdateTask(processID, returnMessage, variableName, taskIdentifier, isInterrupting,
                "FHIR(GET):" + query[0]);
        // this.fhirDataResources.removeResource((Resource)
        // this.fhirDataResources.getResourceById(query[1], "Subscription"));
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
     * 
     * @param processID
     * @param returnMessage
     * @param variableName
     * @param taskIdentifier
     * @param isInterrupting  // this is cancelActivity option for boundary message events true for interrupting boundary message event and false for non-interrupting boundary message event
     * @param query
     * @throws IOException
     */
    @RequestMapping(value = "/RequestObservationValue/{processID}/{returnMessage}/{variableName}/{taskIdentifier}/{isInterrupting}", method = RequestMethod.POST)
    @Async
    public void sendDataToEngineAndUpdateTask(
            @PathVariable("processID") String processID,
            @PathVariable("returnMessage") String returnMessage,
            @PathVariable("variableName") String variableName,
            @PathVariable("taskIdentifier") String taskIdentifier,
            @PathVariable("isInterrupting") boolean isInterrupting,
            @RequestBody String query) {
        try {
            Resource resource = this.engineQueryHandler.getFhirResource(query, returnMessage, processID, variableName,
                    taskIdentifier, isInterrupting);
            if (resource != null) {
                IParser parser = this.ctx.newJsonParser();
                String resourceString = parser.encodeResourceToString(resource);
                EngineInterface engineInterface = new EngineInterfaceFactory().getEngineInterface("CAMUNDA");
                boolean messageSent = engineInterface.sendMessage(returnMessage, processID, variableName,
                        resourceString);
                //check if message is sent and is interrupting to compelete the task in FHIR
                if (isInterrupting && messageSent) {
                        this.fhirDataResources.completeTaskInFHIR(taskIdentifier);
                        String[] resub = this.engineQueryHandler.pendingRequests.get(processID).get(returnMessage);
                        //check if resourse is from subscribe to remove subscribtion
                        if (resub != null) {
                            logger.info("Removed Resubscription: " + resub[0]);
                            this.fhirDataResources.removeResource(
                                    (Resource) this.fhirDataResources.getResourceById(resub[1], "Subscription"));
                        }
                }
            }
        } catch (IOException e) {
            logger.severe("Unexpected Error Occured: ");
            e.printStackTrace();
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