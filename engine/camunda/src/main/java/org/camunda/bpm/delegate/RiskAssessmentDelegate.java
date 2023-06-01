package org.camunda.bpm.delegate;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.model.bpmn.instance.Documentation;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;

import com.esotericsoftware.yamlbeans.YamlReader;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class RiskAssessmentDelegate implements JavaDelegate {

	/**
	 * Class containing the configuration for the risk assessment
	 */
	private class RiskAssessmentConfig {
		private String patient;
		private String outcome;
		private String code;

		public String getPatient() {
			return patient;
		}

		public String getOutcome() {
			return outcome;
		}

		public String getCode() {
			return code;
		}
	}

	/**
	 * Creates a RiskAssessmentConfig object from a yaml string
	 * @param yaml the yaml string
	 * @return the RiskAssessmentConfig object
	 */
	private RiskAssessmentConfig createRiskAssessmentConfig(String yaml) {
		YamlReader reader = new YamlReader(yaml);
			Map<String, Object> map = null;
			try {
				map = (Map<String, Object>) reader.read();	
			} catch (Exception e) {
				e.printStackTrace();
			}
			RiskAssessmentConfig config = new RiskAssessmentConfig();

			config.patient = (String) map.get("patient");
			config.outcome = (String) map.get("outcome");
			config.code = (String) map.get("code");

			return config;
	}

	/**
	 * Called by Camunda when a risk assessment service task is executed
	 */
    @Override
	public void execute(DelegateExecution execution) {
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
		RiskAssessmentConfig config = createRiskAssessmentConfig(sb.toString());
		JSONObject body = createRiskAssessment(config); 
		
		// Send the risk assessment to the WFC
		Unirest.post(properties.getProperty("wfc.url") + "/MakeFHIRResource/POST/RiskAssessment")
                .body(body.toString())
                .asJson();
	}

	/**
	 * Creates a RiskAssessment JSONObject from a RiskAssessmentConfig object
	 * @param config the RiskAssessmentConfig object
	 * @return the RiskAssessment JSONObject
	 */
	private JSONObject createRiskAssessment(RiskAssessmentConfig config) {
		JSONObject body = new JSONObject();
		body.put("resourceType", "RiskAssessment");
		body.put("status", "registered");
		JSONObject subject = new JSONObject();
		subject.put("reference", config.getPatient());
		body.put("subject", subject);
		JSONObject prediction = new JSONObject();
		prediction.put("rationale", config.getOutcome());
		body.put("prediction", prediction);
		JSONArray identifiers = new JSONArray();
		JSONObject codeIdentifier = new JSONObject();
		codeIdentifier.put("system", "observationType");
		codeIdentifier.put("value", config.getCode());
		identifiers.put(codeIdentifier);
		body.put("identifier", identifiers);

		return body;
	}
}
