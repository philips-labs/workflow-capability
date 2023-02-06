package org.camunda.bpm.delegate;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;

import java.io.IOException;
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

        HttpResponse<JsonNode> httpResponse = Unirest.post(properties.getProperty("wfc.url") +
                "/RequestUserTask/" + delegateTask.getProcessInstanceId() + "/" +
                delegateTask.getTaskDefinitionKey() + "/" + delegateTask.getId()).asJson();

    }
}