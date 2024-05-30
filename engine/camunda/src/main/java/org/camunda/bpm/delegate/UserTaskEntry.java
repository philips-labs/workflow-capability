package org.camunda.bpm.delegate;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.Documentation;
import org.camunda.bpm.model.bpmn.instance.Message;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;
import org.camunda.bpm.model.bpmn.instance.UserTask;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserTaskEntry implements TaskListener {
    private static final Logger LOGGER = Logger.getLogger(UserTaskEntry.class.getName());

    @Override
    public void notify(DelegateTask delegateTask) {
        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
           
        } catch (IOException e) {
            LOGGER.severe("Unable to load config file: " + e.getMessage());
        }

        BpmnModelInstance bpmModel = delegateTask.getProcessEngineServices().getRepositoryService()
                .getBpmnModelInstance(delegateTask.getProcessDefinitionId());
        UserTask userTask = (UserTask) bpmModel.getModelElementById(delegateTask.getTaskDefinitionKey());

        Collection<BoundaryEvent> boundaryEvents = bpmModel.getModelElementsByType(BoundaryEvent.class);
        LOGGER.info("Total Boundary Events in model: " + boundaryEvents.size());

        BoundaryEvent messageBoundaryEvent = findMessageBoundaryEvent(userTask, boundaryEvents);
        if (messageBoundaryEvent != null) {
            boolean isUserTaskCreated = createUserTask(properties, delegateTask);
            if (isUserTaskCreated) {
                System.out.println("User Task created with boundary message event successfully");
                handleBoundaryEvent(messageBoundaryEvent, delegateTask, properties);
            }
        } else {
            boolean isUserTaskCreated = createUserTask(properties, delegateTask);
            if (isUserTaskCreated) {
                System.out.println("User Task  with no boundary event created successfully");
            }
        }
    }

    private BoundaryEvent findMessageBoundaryEvent(UserTask userTask, Collection<BoundaryEvent> boundaryEvents) {
        for (BoundaryEvent event : boundaryEvents) {
            if (event.getAttachedTo().getId().equals(userTask.getId())) {
                return event;
            }
        }
        return null;
    }

    private void handleBoundaryEvent(BoundaryEvent messageBoundaryEvent, DelegateTask delegateTask,
            Properties properties) {
        MessageEventDefinition messageEventDefinition = (MessageEventDefinition) messageBoundaryEvent
                .getEventDefinitions().iterator().next();
        Message message = messageEventDefinition.getMessage();
        String messageName = message != null ? message.getName() : "No message";

        Collection<Documentation> documentations = messageBoundaryEvent.getDocumentations();
        String documentationText = !documentations.isEmpty() ? documentations.iterator().next().getTextContent()
                : "No documentation";

        LOGGER.info("Handling boundary event ID: " + messageBoundaryEvent.getId() + " with message: " + messageName);
        String variableName = messageBoundaryEvent.getName();
        StringBuffer sb = new StringBuffer();
        Matcher match = Pattern.compile("\\$\\((.*?)\\)").matcher(documentationText);
        while (match.find()) {
            String reqVariable = match.group(1);
            match.appendReplacement(sb, Matcher.quoteReplacement((String) delegateTask.getVariable(reqVariable)));
        }
        match.appendTail(sb);
        String query = sb.toString();
        postObservation(query, delegateTask, messageName, variableName, properties);
    }

    public boolean createUserTask( Properties properties, DelegateTask delegateTask){
        
        try {
            System.out.println("Request to create UserTask" + delegateTask.getId());
            HttpResponse<JsonNode> httpResponse = Unirest.post(properties.getProperty("wfc.url") +
                    "/RequestUserTask/" + delegateTask.getProcessInstanceId() + "/" +
                    delegateTask.getTaskDefinitionKey() + "/" + delegateTask.getId())
                    .asJson();
            
            if (httpResponse.getStatus() != 200) {
                LOGGER.warning("Failed to create UserTask: " + httpResponse.getStatusText());
                return false;
            } else {
                LOGGER.info("UserTask created successfully: " + httpResponse.getStatus() + " " + httpResponse.getStatus());
                return true;
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to create UserTask: " + e.getMessage());
            return false;
        }
    }
    private void postObservation(String query, DelegateTask delegateTask, String messageName, String variableName,
            Properties properties) {
        try {
            String url = properties.getProperty("wfc.url") + "/RequestObservationValue/"
                    + delegateTask.getProcessInstanceId() + "/" + messageName + "/" + variableName + "/"
                    + delegateTask.getId();
            HttpResponse<JsonNode> httpResponse = Unirest.post(url).body(query).asJson();
            if (httpResponse.getStatus() != 200) {
                LOGGER.warning("Failed to post observation value: " + httpResponse.getStatusText());
            } else {
                LOGGER.info("Observation posted successfully: " + httpResponse.getStatus() + " "
                        + httpResponse.getStatus());
            }

        } catch (Exception e) {
            LOGGER.warning("Failed to post observation value: " + e.getMessage());
        }
    }
}
