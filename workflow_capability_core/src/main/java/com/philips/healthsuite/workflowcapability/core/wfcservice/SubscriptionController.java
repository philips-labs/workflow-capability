package com.philips.healthsuite.workflowcapability.core.wfcservice;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.philips.healthsuite.workflowcapability.core.fhirresources.FhirDataResources;
import kong.unirest.json.JSONObject;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;


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
    @RequestMapping(
            value = "/OnCarePlanChange",
            method = RequestMethod.POST)
    void receiveCarePlanSubscribe() throws IOException {
        List<CarePlan> newCarePlans = this.fhirDataResources.getNewCarePlans();
        syncCarePlans(newCarePlans);
    }


    /**
     * @param processID
     * @param returnMessage
     * @param variableName
     * @throws IOException
     */
    @RequestMapping(
            value = "/OnRequestChange/{processID}/{returnMessage}/{variableName}",
            method = RequestMethod.POST)
    void requestChangeSubscribe(@PathVariable("processID") String processID, @PathVariable("returnMessage") String returnMessage, @PathVariable("variableName") String variableName) throws IOException {
        String[] query = this.engineQueryHandler.pendingRequests.get(processID).get(returnMessage);
        this.fhirDataResources.removeResource((Resource) this.fhirDataResources.getResourceById(query[1], "Subscription"));
        informEngineAboutObservationValue(processID, returnMessage, variableName, "FHIR(GET):" + query[0]);
    }


    /**
     * Reads current list of completed FHIR-tasks and checks which of these are not marked as completed BPMN-tasks.
     * Then, it marks the latter as completed.
     *
     * @throws IOException
     */
    @RequestMapping(
            value = "/OnTaskChange",
            method = RequestMethod.POST)
    void synchronizeCompletedTasksBetweenFhirAndEngine() throws IOException {
        EngineInterfaceFactory engineInterfaceFactory = new EngineInterfaceFactory();
        EngineInterface engineInterface = engineInterfaceFactory.getEngineInterface("CAMUNDA");
        List<String> completedTasksIds = this.fhirDataResources.getRecentlyCompletedTasksIds(); // Tasks marked in FHIR as completed

        for (String completedTaskIdentifier : completedTasksIds) {
            if (!this.taskIdentifiersAlreadySignalledToBpmnEngineAsCompleted.contains(completedTaskIdentifier)) {
                engineInterface.completeTask(completedTaskIdentifier);
                this.taskIdentifiersAlreadySignalledToBpmnEngineAsCompleted.add(completedTaskIdentifier);
//                this.fhirDataResources.deleteTaskFromFhirStore(completedTask);
            }
        }
    }

    @RequestMapping(
            value = "/MedicationStatementChange/MedicationStatement/{ID}",
            method = RequestMethod.PUT,
            consumes = "application/fhir+json;charset=UTF-8"
    )
    public ResponseEntity<String> synchronizeMedicationStatementVariables(@PathVariable("ID") String ID, @RequestBody(required = false) String rawJson) throws IOException {
        IParser parser = ctx.newJsonParser();
        MedicationStatement medicationStatement = parser.parseResource(MedicationStatement.class, rawJson);
        if (medicationStatement != null && medicationStatement.getSubject() != null) {
            String patientReference = medicationStatement.getSubject().getReference();

            EngineInterfaceFactory engineInterfaceFactory = new EngineInterfaceFactory();
            EngineInterface engineInterface = engineInterfaceFactory.getEngineInterface("CAMUNDA");
            String patientId = patientReference.replace("Patient/", "");
            Map<String, Integer> medicationStatements = this.fhirDataResources.getMedicationStatementDoseSums(patientId);
            System.out.println(patientId);
            for (Map.Entry<String, Integer> entry : medicationStatements.entrySet()) {
                String key = entry.getKey();
                Integer value = entry.getValue();
                CarePlan carePlan = fhirDataResources.getCarePlanByPatientId(patientId);
                String wfInstanceIdentifier = null;
                if (carePlan != null && carePlan.hasIdentifier()) {
                    for (Identifier identifier : carePlan.getIdentifier()) {
                        if ("wfEngine".equals(identifier.getSystem())) {
                            wfInstanceIdentifier = identifier.getValue();
                        }
                    }
                }
                if (wfInstanceIdentifier != null) {
                    String variableName = key + "Total";
                    engineInterface.sendVariable(wfInstanceIdentifier, variableName, value);
                }


            }
        }
        String serialized = parser.encodeResourceToString(medicationStatement);
        return ResponseEntity.ok(serialized);
    }


    /**
     * Service for marking a FHIR CarePlan as completed.
     *
     * @param carePlanInstanceID
     */
    @RequestMapping(
            value = "/FinishWorkflow/{carePlanInstanceID}",
            method = RequestMethod.POST)
    void finishWorkflow(@PathVariable("carePlanInstanceID") String carePlanInstanceID) {
        this.fhirDataResources.markCarePlanAsCompleted(carePlanInstanceID);
//        this.fhirDataResources.removeResource(fhirDataResources.getCarePlanByEngineId(carePlanInstanceID));
    }


    /**
     * @param carePlanInstanceID
     * @param taskID
     * @param taskIdentifier
     * @throws IOException
     */
    @RequestMapping(
            value = "/RequestReceiveTask/{carePlanInstanceID}/{taskID}/{taskIdentifier}",
            method = RequestMethod.POST)
    @Async
    void subscribeNewReceiveTask(@PathVariable("carePlanInstanceID") String carePlanInstanceID, @PathVariable("taskID") String taskID, @PathVariable("taskIdentifier") String taskIdentifier) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    fhirDataResources.createTask(taskIdentifier, carePlanInstanceID, taskID, null,"in-progress", "DefaultTask");
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
     * @param query
     * @throws IOException
     */
    @RequestMapping(
            value = "/RequestObservationValue/{processID}/{returnMessage}/{variableName}",
            method = RequestMethod.POST)
    @Async
    void informEngineAboutObservationValue(@PathVariable("processID") String processID, @PathVariable("returnMessage") String returnMessage, @PathVariable("variableName") String variableName, @RequestBody String query) throws IOException {
        Resource resource = this.engineQueryHandler.getFhirResource(query, returnMessage, processID, variableName);
        if (resource != null) {
            IParser parser = this.ctx.newJsonParser();
            String resourceString = parser.encodeResourceToString(resource);
            EngineInterface engineInterface = new EngineInterfaceFactory().getEngineInterface("CAMUNDA");
            engineInterface.sendMessage(returnMessage, processID, variableName, resourceString);
        }
    }


    /**
     * @param carePlanInstanceID
     * @param taskIdentifier
     * @param taskID
     */
    @RequestMapping(
            value = "/RequestUserTask/{carePlanInstanceID}/{taskID}/{taskIdentifier}",
            method = RequestMethod.POST)
    void subscribeNewUserTask(
            @PathVariable("carePlanInstanceID") String carePlanInstanceID,
            @PathVariable("taskIdentifier") String taskIdentifier,
            @PathVariable("taskID") String taskID,
            @RequestBody String rawPayload) throws InterruptedException {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject json = new JSONObject(rawPayload);
                    String taskName = json.optString("taskName", null);
                    String apiType = json.optString("apiType", null);
                    String taskType = "DefaultTask";
                    String taskStatus = "received";
                    if (!apiType.isEmpty()) {
                        taskType = apiType;
                        taskStatus = "in-progress";
                    }
                    fhirDataResources.createTask(taskIdentifier, carePlanInstanceID, taskID, taskName, taskStatus, taskType);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /**
     * @param carePlanInstanceID
     * @param taskIdentifier
     * @param taskID
     * @param implementation
     */
    @RequestMapping(
            value = "/RequestServiceTask/{carePlanInstanceID}/{taskID}/{taskIdentifier}/{implementation}",
            method = RequestMethod.POST)
    void subscribeNewServiceTask(
            @PathVariable("carePlanInstanceID") String carePlanInstanceID,
            @PathVariable("taskIdentifier") String taskIdentifier,
            @PathVariable("taskID") String taskID,
            @PathVariable("implementation") String implementation) throws InterruptedException {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    fhirDataResources.createTask(taskIdentifier, carePlanInstanceID, taskID, null,"in-progress", implementation);
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
            String WorkflowID = newCarePlan.getIdentifier().get(0).getValue();
            String carePlanInstanceID = engineInterface.instantiateWorkflow(WorkflowID, newCarePlan.getSubject().getReference());
            this.fhirDataResources.startCarePlan(newCarePlan, carePlanInstanceID);
        }
    }
}