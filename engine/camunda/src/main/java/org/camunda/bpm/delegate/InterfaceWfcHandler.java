package org.camunda.bpm.delegate;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.instance.DataObjectReferenceImpl;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.Documentation;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.Message;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;
import org.camunda.bpm.model.bpmn.instance.ReceiveTask;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

/*
* VariableAccessor functional interface is providing a flexible way to access process variables from both DelegateTask and DelegateExecution contexts in this program. 
* It enhances code reusability and flexibility, allowing the same query processing logic to be applied in various parts of the program without duplicating code.
*/

@FunctionalInterface
interface VariableAccessor {
    Object getVariable(String name);
}

/**
 * In general, This class is used to handle the interface between the workflow capability and the workflow engine:
 * It is used to handle the boundary message events and the data object reference in the workflow engine
 * It is also used to send user tasks to the wfc, so that it will be created in the FHIR store.
 *
 */

public class InterfaceWfcHandler {
    private final Logger logger = Logger.getLogger(InterfaceWfcHandler.class.getName());
    private final Properties properties = new Properties();

    /*
     * loading the application property
     */
    public InterfaceWfcHandler() {
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
        } catch (IOException e) {
            logger.warning("Failed to load application properties: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * This method is used to process the query and replace the process variables
     * with the actual values
     * @param query the query to be processed
     * @param variableAccessor the variableAccessor functional interface that allows
     * access to process variables
    */

    private String processQuery(String query, VariableAccessor variableAccessor) {
        StringBuffer sb = new StringBuffer();
        Pattern pattern = Pattern.compile("\\$\\((\\w+)\\)");
        Matcher matcher = pattern.matcher(query);
        while (matcher.find()) {
            String variableName = matcher.group(1);
            logger.info("The required variable is: " + variableName);
            String replacement;
            if (variableName.equals("NOW") || variableName.startsWith("MOMENT")) {
                logger.info("The moment in variable is: " + variableName);
                replacement = "$(" + variableName + ")";
            } else {
                // replacement = (String) delegateTask.getVariable(variableName);
                replacement = (String) variableAccessor.getVariable(variableName);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /*
     * This method is used to handle the boundary message events
     *
     * @param messageBoundaryEvent the messageBoundaryEvent attached to the user task
     * @param delegateTask the delegateTask object
     *
     */

    public void handleBoundaryEvent(BoundaryEvent messageBoundaryEvent, DelegateTask delegateTask) {
        boolean isInterupting = messageBoundaryEvent.cancelActivity();
        MessageEventDefinition messageEventDefinition = (MessageEventDefinition) messageBoundaryEvent
                .getEventDefinitions().iterator().next();
        Message message = messageEventDefinition.getMessage();
        String messageName = message != null ? message.getName() : "No message";

        Collection<Documentation> documentations = messageBoundaryEvent.getDocumentations();
        String documentationText = !documentations.isEmpty() ? documentations.iterator().next().getTextContent()
                : "No documentation";
        String variableName = messageBoundaryEvent.getName();
        VariableAccessor accessor = varName -> delegateTask.getVariable(varName);
        String query = processQuery(documentationText, accessor);
        requestData(query, delegateTask.getProcessInstanceId(), messageName, variableName, delegateTask.getId(), isInterupting);
    }

    /*
     * This method is used to handle the data object reference in this implemetation
     * associated with receieve task
     * @param delegateExecution the delegateExecution object
     */

    public void dataObjectReferenceImpl(DelegateExecution delegateExecution) {
        VariableAccessor accessor = varName -> delegateExecution.getVariable(varName);
        String message = "";
        ReceiveTask receiveTask = null;
        receiveTask = delegateExecution.getProcessEngine().getRepositoryService()
                .getBpmnModelInstance(delegateExecution.getProcessDefinitionId())
                .getModelElementById(delegateExecution.getCurrentActivityId());
        message = receiveTask.getMessage().getName();
        String variableName = "";
        String query = "";
        BpmnModelInstance bpmModel = delegateExecution.getProcessEngine().getRepositoryService()
                .getBpmnModelInstance(delegateExecution.getProcessDefinitionId());
        receiveTask = bpmModel.getModelElementById(delegateExecution.getCurrentActivityId());
        // Get documentation from dataReference, NOTE: Assumed only 1 dataRef with a
        // single documentation is found.
        for (DataInputAssociation dataInputAssociation : receiveTask.getDataInputAssociations()) {
            for (ItemAwareElement source : dataInputAssociation.getSources()) {
                DataObjectReferenceImpl dataReference = bpmModel.getModelElementById(source.getId());
                variableName = dataReference.getName();
                for (Documentation documentation : dataReference.getDocumentations()) {
                    query = documentation.getRawTextContent();
                }
            }
        }
        query = processQuery(query, accessor);
        /*
         * The task identifier in this case is, considered as the taskidentifier of the receive task,
         * it is because the receive task is not registered in the database in this prototype.
        */
        requestData(query, delegateExecution.getProcessInstanceId(), message, variableName, "receiveTask", false);
    }

    /*
     * this method is used notfify the wfc to create a user task in the FHIR
     * @param delegateTask the delegateTask object from  User Task
     */

    public boolean createUserTask(DelegateTask delegateTask) {
        try {
            HttpResponse<JsonNode> httpResponse = Unirest
                    .post(properties.getProperty("wfc.url") + "/RequestUserTask/" + delegateTask.getProcessInstanceId()
                            + "/" + delegateTask.getTaskDefinitionKey() + "/" + delegateTask.getId())
                    .asJson();
            if (httpResponse.getStatus() != 200) {
                logger.warning("wfc Failed to create UserTask: " + httpResponse.getStatusText());
                return false;
            } else
                return true;
        } catch (Exception e) {
            logger.severe("Engine Failed to create UserTask: " + e.getMessage());
            return false;
        }
    }

    /*
     * This method is used to send the request to the wfc to request data from the FHIR via wfc service 
     * @param query the FHIR-query to be sent to the WFC service
     * @param processInstanceId: the process instance id of the process
     * @param messageName: the message name
     * @param variableName: the name (for boundary message event or data object
     * reference)
     * @param taskIdentifier: the task identifier
     *
     */

    private void requestData(String query, String processInstanceId, String messageName, String variableName,
            String taskIdentifier, boolean isInterupting) {
        //// Building the complete URL for the request.
        String url = properties.getProperty("wfc.url") + "/RequestObservationValue/"
                + processInstanceId + "/" + messageName + "/" + variableName + "/"
                + taskIdentifier + "/" + isInterupting;
        // Sending the request to the WFC service and Unirest is used to send a POST
        // request to the WFC service.
        Unirest.post(url).body(query).asJson();
    }
}