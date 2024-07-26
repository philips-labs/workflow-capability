package org.camunda.bpm.delegate;

import java.util.Collection;
import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.ReceiveTask;

public class ReceiveTaskEntry implements JavaDelegate {
    InterfaceWfcHandler interfaceWfcHandler = new InterfaceWfcHandler();
    private static final Logger logger = Logger.getLogger(ReceiveTaskEntry.class.getName());
    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        ReceiveTask receiveTask;
        interfaceWfcHandler.dataObjectReferenceImpl(delegateExecution);
        BpmnModelInstance bpmModel = delegateExecution.getProcessEngineServices().getRepositoryService()
                .getBpmnModelInstance(delegateExecution.getProcessDefinitionId());
        receiveTask = delegateExecution.getProcessEngine().getRepositoryService()
                .getBpmnModelInstance(delegateExecution.getProcessDefinitionId())
                .getModelElementById(delegateExecution.getCurrentActivityId());
        Collection<BoundaryEvent> boundaryEvents = bpmModel.getModelElementsByType(BoundaryEvent.class);
        if (boundaryEvents.size() > 0)
            findMessageBoundaryEvent(receiveTask, boundaryEvents);
        else
            logger.info("no boundary message events");
    }
    /*
     * For further implementation of the boundary message event on receive task
     */
    private BoundaryEvent findMessageBoundaryEvent(ReceiveTask receiveTask, Collection<BoundaryEvent> boundaryEvents) {
        for (BoundaryEvent event : boundaryEvents) {
            if (event.getAttachedTo().getId().equals(receiveTask.getId())) {
                return event;
            }
        }
        return null;
    }
}