package org.camunda.bpm.delegate;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.UserTask;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;

public class UserTaskEntry implements TaskListener {
    InterfaceWfcHandler interfaceWfcHandler = new InterfaceWfcHandler();
    private final Logger logger = Logger.getLogger(UserTaskEntry.class.getName());

    @Override
    public void notify(DelegateTask delegateTask) {
        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));

        } catch (IOException e) {

            logger.severe("Unable to load config file: " + e.getMessage());
        }

        BpmnModelInstance bpmModel = delegateTask.getProcessEngineServices().getRepositoryService()
                .getBpmnModelInstance(delegateTask.getProcessDefinitionId());
        UserTask userTask = (UserTask) bpmModel.getModelElementById(delegateTask.getTaskDefinitionKey());
        boolean isUserTaskCreated = interfaceWfcHandler.createUserTask(properties, delegateTask);
        if (isUserTaskCreated) {
            logger.info("User Task  with no boundary event created successfully");
        }
        Collection<BoundaryEvent> boundaryEvents = bpmModel.getModelElementsByType(BoundaryEvent.class);
        logger.info("Total Boundary Events in model: " + boundaryEvents.size());

        BoundaryEvent messageBoundaryEvent = findMessageBoundaryEvent(userTask, boundaryEvents);
        if (messageBoundaryEvent != null) {
            interfaceWfcHandler.handleBoundaryEvent(messageBoundaryEvent, delegateTask, properties);
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
}