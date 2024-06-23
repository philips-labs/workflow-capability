package org.camunda.bpm.delegate;

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

public class InterfaceWfcHandler {
private final Logger logger = Logger.getLogger(InterfaceWfcHandler.class.getName());

public String processQuery(String query, DelegateTask delegateTask) {
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
            replacement = (String) delegateTask.getVariable(variableName);
        }
        matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
}

public void handleBoundaryEvent(BoundaryEvent messageBoundaryEvent, DelegateTask delegateTask,
        Properties properties) {
    MessageEventDefinition messageEventDefinition = (MessageEventDefinition) messageBoundaryEvent
            .getEventDefinitions().iterator().next();
    Message message = messageEventDefinition.getMessage();
    String messageName = message != null ? message.getName() : "No message";

    Collection<Documentation> documentations = messageBoundaryEvent.getDocumentations();
    String documentationText = !documentations.isEmpty() ? documentations.iterator().next().getTextContent()
            : "No documentation";

    logger.info("Handling boundary event ID from separate component: " + messageBoundaryEvent.getId()
            + " with message: " + messageName);
    String variableName = messageBoundaryEvent.getName();
    String query = processQuery(documentationText, delegateTask);
    requestData(query, delegateTask.getProcessInstanceId(), messageName, variableName, delegateTask.getId(),
            properties);
}

public void dataObjectReferenceImpl(Properties properties, DelegateExecution delegateExecution) {

    String message = "";
    ReceiveTask receiveTask = null;
    try {
        receiveTask = delegateExecution.getProcessEngine().getRepositoryService()
                .getBpmnModelInstance(delegateExecution.getProcessDefinitionId())
                .getModelElementById(delegateExecution.getCurrentActivityId());
        message = receiveTask.getMessage().getName();
    } catch (Exception e) {
        logger.severe("No message found for current Receive Task");
    }
    String variableName = "";
    String query = "";
    try {
        BpmnModelInstance bpmModel = delegateExecution.getProcessEngine().getRepositoryService()
                .getBpmnModelInstance(delegateExecution.getProcessDefinitionId());
        receiveTask = bpmModel.getModelElementById(delegateExecution.getCurrentActivityId());
        // Get documentation from dataReference, NOTE: Assumed only 1 dataRef with a single documentation is found.
        for (DataInputAssociation dataInputAssociation : receiveTask.getDataInputAssociations()) {
            for (ItemAwareElement source : dataInputAssociation.getSources()) {
                DataObjectReferenceImpl dataReference = bpmModel.getModelElementById(source.getId());
                variableName = dataReference.getName();
                for (Documentation documentation : dataReference.getDocumentations()) {
                    query = documentation.getRawTextContent();
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }

    StringBuffer sb = new StringBuffer();
    Matcher m = Pattern.compile("\\$\\((.*?)\\)").matcher(query);
    int count = 1;
    while (m.find()) {
        String reqVariable = m.group(count);
        m.appendReplacement(sb, (String) delegateExecution.getVariable(reqVariable));
        count++;
    }
    m.appendTail(sb);
    query = sb.toString();
    //this task identifier is cosidered as the task id of the receive task, it is because the receive task is not registered in the database
    requestData(query, delegateExecution.getProcessInstanceId(), message, variableName, "receiveTask",
            properties);

}

public boolean createUserTask(Properties properties, DelegateTask delegateTask) {
    try {
        HttpResponse<JsonNode> httpResponse = Unirest.post(properties.getProperty("wfc.url") +
                "/RequestUserTask/" + delegateTask.getProcessInstanceId() + "/" +
                delegateTask.getTaskDefinitionKey() + "/" + delegateTask.getId())
                .asJson();

        if (httpResponse.getStatus() != 200) {
            logger.warning("Failed to create UserTask: " + httpResponse.getStatusText());
            return false;
        } else
             return true;
    } catch (Exception e) {
        logger.severe("Failed to create UserTask: " + e.getMessage());
        return false;
    }
}

public void requestData(String query, String delegateTask, String messageName, String variableName,
        String taskIdentifier,
        Properties properties) {
    try {
        String url = properties.getProperty("wfc.url") + "/RequestObservationValue/"
                + delegateTask + "/" + messageName + "/" + variableName + "/"
                + taskIdentifier;
        // HttpResponse<JsonNode> httpResponse = retryPostObservation(url, query);
        HttpResponse<JsonNode> httpResponse = Unirest.post(url).body(query).asJson();
        logger.info(" the response log is ==>  " + httpResponse.getStatus());
        if (httpResponse.getStatus() != 200) {
            logger.warning("Failed to post observation value: " + httpResponse.getStatusText());
        } else {
            logger.info("Observation posted successfully: " + httpResponse.getStatus() + " "
                    + httpResponse.getStatus());
        }
    } catch (Exception e) {
        logger.warning("Failed to post observation value: " + e.getMessage());
    }
}
}