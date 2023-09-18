package org.camunda.bpm.delegate;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class UserTaskEntry implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {

        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String expression = getExpressionsFromUserTask(delegateTask);
        String taskName = delegateTask.getName();

        JSONObject json = new JSONObject();
        json.put("taskName", taskName);
        json.put("apiType", expression);

        HttpResponse<JsonNode> httpResponse = Unirest.post(properties.getProperty("wfc.url") +
                        "/RequestUserTask/" + delegateTask.getProcessInstanceId() + "/" +
                        delegateTask.getTaskDefinitionKey() + "/" + delegateTask.getId())
                .body(json)
                .asJson();

    }

    /**
     * Extracts the expression of type 'expression' from the user task.
     *
     * @param delegateTask The delegate task from which to extract the expression.
     * @return The extracted expression or an empty string if not found.
     */
    private String getExpressionsFromUserTask(DelegateTask delegateTask) {
        RepositoryService repositoryService = ProcessEngines.getDefaultProcessEngine().getRepositoryService();
        BpmnModelInstance bpmnModelInstance = repositoryService.getBpmnModelInstance(delegateTask.getProcessDefinitionId());
        UserTask userTask = (UserTask) bpmnModelInstance.getModelElementById(delegateTask.getTaskDefinitionKey());

        List<CamundaExecutionListener> listeners = userTask.getExtensionElements().getElementsQuery().filterByType(CamundaExecutionListener.class).list();

        if (!listeners.isEmpty()) {
            CamundaExecutionListener executionListener = listeners.get(0);
            return executionListener.getCamundaExpression();
        }

        return "";
    }
}