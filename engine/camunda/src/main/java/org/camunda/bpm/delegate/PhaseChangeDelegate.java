package org.camunda.bpm.delegate;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.model.bpmn.instance.Documentation;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;

import com.esotericsoftware.yamlbeans.YamlReader;

import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class PhaseChangeDelegate implements JavaDelegate {

	/**
	 * Class containing the configuration for the risk assessment
	 */
	private class PhaseChangeConfig {
		private String code;
        private String patient;

		public String getCode() {
			return code;
		}

        public String getPatient() {
            return patient;
        }
	}
	private PhaseChangeConfig createPhaseChangeConfig(String yaml) {
		YamlReader reader = new YamlReader(yaml);
        Map<String, Object> map = null;
        try {
            map = (Map<String, Object>) reader.read();	
        } catch (Exception e) {
            e.printStackTrace();
        }

        PhaseChangeConfig config = new PhaseChangeConfig();
        config.code = (String) map.get("code");
        config.patient = (String) map.get("patient");

        return config;
	}

	/**
	 * Called by Camunda when a risk assessment service task is executed
	 */
    @Override
	public void execute(DelegateExecution execution) {
		System.out.println("PhaseChangeActivity: " + execution.getActivityInstanceId() + " for " + execution.getProcessInstanceId());
		Properties properties = new Properties();
		ServiceTask serviceTask = null;
		// Fetch the service task
        try {
            serviceTask = execution.getProcessEngine().getRepositoryService().getBpmnModelInstance(execution.getProcessDefinitionId()).getModelElementById(execution.getCurrentActivityId());
        } catch (Exception e) {
            e.printStackTrace();
        }
		// Load the config file
        try {
           properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
           e.printStackTrace();
        }

		// Get the yaml string from the Task documentation
		String data = "";
		for (Documentation documentation : serviceTask.getDocumentations()) {
			data = documentation.getRawTextContent();
		}

		// Replace the variables in the yaml string with the actual values
		StringBuffer sb = new StringBuffer();
        Matcher m = Pattern.compile("\\$\\((.*?)\\)").matcher(data);
        while (m.find()) {
            String reqVariable = m.group(1);
			System.out.println(reqVariable);
            m.appendReplacement(sb, (String) execution.getVariable(reqVariable));
        }
        m.appendTail(sb);

		// Create the risk assessment
		PhaseChangeConfig config = createPhaseChangeConfig(sb.toString());
		JSONObject body = createPhaseObservation(config); 
		
		// Send the risk assessment to the WFC
		Unirest.post(properties.getProperty("wfc.url") + "/MakeFHIRResource/POST/Observation")
                .body(body.toString())
                .asJson();
	}

	/**
	 * Creates a RiskAssessment JSONObject from a RiskAssessmentConfig object
	 * @param config the RiskAssessmentConfig object
	 * @return the RiskAssessment JSONObject
	 */
	private JSONObject createPhaseObservation(PhaseChangeConfig config) {
		JSONObject body = new JSONObject();
		body.put("resourceType", "Observation");
		body.put("status", "final");
		JSONObject subject = new JSONObject();
		subject.put("reference", config.getPatient());
		body.put("subject", subject);
		JSONObject code = new JSONObject();
		JSONArray coding = new JSONArray();
		JSONObject hemeoCode = new JSONObject();
		hemeoCode.put("system", "hemeo");
		hemeoCode.put("code", config.getCode());
		hemeoCode.put("display", "Phase Change");
		coding.put(hemeoCode);
        code.put("coding", coding);
        body.put("code", code);
        body.put("effectiveDateTime", LocalDateTime.now().toString());
        body.put("valueDateTime", LocalDateTime.now().toString());

		return body;
	}
}
