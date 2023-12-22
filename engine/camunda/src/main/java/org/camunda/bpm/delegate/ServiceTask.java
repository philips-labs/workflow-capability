package org.camunda.bpm.delegate;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;


public class ServiceTaskEntry implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
//        isTaskImplementationValid(execution);
        try {
            String apiServiceType = (String) execution.getVariable("apiType");
            execution.setVariable("detailedErrorInfo", null);
            String processInstanceId = execution.getProcessInstanceId();
            String taskDefinitionKey = execution.getCurrentActivityId();


            HttpResponse<JsonNode> httpResponse = Unirest.post("http://workflowcapabilities:5003" +
                            "/RequestServiceTask/" + processInstanceId + "/" +
                            taskDefinitionKey + "/" + processInstanceId + "/" + apiServiceType)
                    .asJson();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void isTaskImplementationValid(DelegateExecution execution) {
        BpmnModelElementInstance modelInstance = execution.getBpmnModelInstance().getModelElementById(execution.getCurrentActivityId());
        String taskName = execution.getCurrentActivityName();

        if (modelInstance instanceof ServiceTask) {
            ServiceTask serviceTask = (ServiceTask) modelInstance;
            String implementationType = serviceTask.getCamundaType();

            if (!"expression".equals(implementationType)) {
                String message = "Only expression type is supported for service tasks. Cause: " + taskName;
                execution.setVariable("errorMessage", message);
                throw new BpmnError("UNSUPPORTED_TYPE", message);
            }
        }
    }
}
