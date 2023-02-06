package org.camunda.bpm.delegate;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;

import java.io.IOException;
import java.util.Properties;

public class FinishWorkflow implements ExecutionListener {

    @Override
    public void notify(DelegateExecution delegateExecution) throws Exception {

        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
            throw new Exception();
        }

        HttpResponse<JsonNode> httpResponse = Unirest.post(properties.getProperty("wfc.url") +
                "/FinishWorkflow/" + delegateExecution.getProcessInstanceId()).asJson();

    }
}
