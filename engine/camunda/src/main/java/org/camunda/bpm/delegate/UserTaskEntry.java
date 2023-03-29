package org.camunda.bpm.delegate;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.model.bpmn.instance.Documentation;
import org.camunda.bpm.model.bpmn.instance.UserTask;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserTaskEntry implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {

        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String description = null;

        for(Documentation docs : delegateTask.getBpmnModelElementInstance().getDocumentations()) {
            description = docs.getRawTextContent();
        }

        StringBuffer sb = new StringBuffer();
        Matcher m = Pattern.compile("\\$\\((.*?)\\)").matcher(description);
        int count = 1;
        while (m.find()) {
            String reqVariable = m.group(count);
            m.appendReplacement(sb, (String) delegateTask.getVariable(reqVariable));
            count++;
        }
        m.appendTail(sb);

        HttpResponse<JsonNode> httpResponse = Unirest.post(properties.getProperty("wfc.url") +
                "/RequestUserTask/" + delegateTask.getProcessInstanceId() + "/" +
                delegateTask.getTaskDefinitionKey() + "/" + delegateTask.getId())
                .body(sb)
                .asJson();

    }
}