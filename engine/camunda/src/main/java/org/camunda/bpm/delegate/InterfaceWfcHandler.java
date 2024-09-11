package org.camunda.bpm.delegate;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.instance.DataObjectReferenceImpl;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.Documentation;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.Message;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;
import org.camunda.bpm.model.bpmn.instance.ReceiveTask;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

/*
* VariableAccessor functional interface is providing a flexible way to access process variables from both DelegateTask and DelegateExecution contexts in this program. 
* It enhances code reusability and flexibility, allowing the same query processing logic to be applied in various parts of the program without duplicating code.
*/

@FunctionalInterface
interface VariableAccessor {
    Object getVariable(String name);
}

/**
 * In general, This class is used to handle the interface between the workflow capability and the workflow engine:
 * It is used to handle the boundary message events and the data object reference in the workflow engine
 * It is also used to send user tasks to the wfc, so that it will be created in the FHIR store.
 *
 */
public class InterfaceWfcHandler {
    private static final Logger logger = Logger.getLogger(InterfaceWfcHandler.class.getName());
    private final Properties properties = new Properties();

    /*
     * loading the application property
     */

    public InterfaceWfcHandler() {
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load application properties: {0}", e.getMessage());
            e.printStackTrace();
        }
    }
    /*
     * This method is used to process the query and replace the process variables
     * with the actual values
     * @param query the query to be processed
     * @param variableAccessor the variableAccessor functional interface that allows
     * access to process variables
    */
    private String processQuery(String query, VariableAccessor variableAccessor) {
        Pattern pattern = Pattern.compile("\\$\\((NOW|MOMENT\\((NOW|\\$\\w+|\\w+),\\s*([+-]?\\d+[hmsd])\\)|\\w+)\\)");
        StringBuffer sb = new StringBuffer();
        // Pattern pattern = Pattern.compile("\\$\\((\\w+)\\)");
        Matcher matcher = pattern.matcher(query);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        while (matcher.find()) {
            String replacement;
            String baseValue = matcher.group(1);
            String innerBaseValue = matcher.group(2);
            String innerOffset = matcher.group(3);
            String variableName = matcher.group(1);
            String formattedDateTime = "";
            Instant dateTime = null;
            if ("NOW".equals(baseValue)) {
                replacement = ZonedDateTime.now().format(dateTimeFormatter);
                dateTime = Instant.parse(replacement); 
                formattedDateTime = DateTimeFormatter.ISO_INSTANT.format(dateTime);
                replacement = formattedDateTime;
            } else if (baseValue.startsWith("MOMENT")) {
                replacement = HandleTimeExpression(innerBaseValue, innerOffset, variableAccessor, dateTimeFormatter);
                dateTime = Instant.parse(replacement); 
                formattedDateTime = DateTimeFormatter.ISO_INSTANT.format(dateTime);
                replacement = formattedDateTime;
            }
            else {
                replacement = (String) variableAccessor.getVariable(variableName);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /*
     * This method is used to handle the boundary message events
     *
     * @param messageBoundaryEvent the messageBoundaryEvent attached to the user task
     * @param delegateTask the delegateTask object
     *
     */
    public void handleBoundaryEvent(BoundaryEvent messageBoundaryEvent, DelegateTask delegateTask) {
        boolean isInterupting = messageBoundaryEvent.cancelActivity();
        MessageEventDefinition messageEventDefinition = (MessageEventDefinition) messageBoundaryEvent
                .getEventDefinitions().iterator().next();
        Message message = messageEventDefinition.getMessage();
        String messageName = message != null ? message.getName() : "No message";
        Collection<Documentation> documentations = messageBoundaryEvent.getDocumentations();
        String documentationText = !documentations.isEmpty() ? documentations.iterator().next().getTextContent()
                : "No documentation";
        String variableName = messageBoundaryEvent.getName();
        VariableAccessor accessor = varName -> delegateTask.getVariable(varName);
        String query = processQuery(documentationText, accessor);
        requestData(query, delegateTask.getProcessInstanceId(), messageName, variableName, delegateTask.getId(), isInterupting);
    }

    /*
     * This method is used to handle the data object reference in this implemetation
     * associated with receieve task
     * @param delegateExecution the delegateExecution object
     */
    public void dataObjectReferenceImpl(DelegateExecution delegateExecution) {
        VariableAccessor accessor = varName -> delegateExecution.getVariable(varName);
        String message = "";
        ReceiveTask receiveTask = null;
        receiveTask = delegateExecution.getProcessEngine().getRepositoryService()
                .getBpmnModelInstance(delegateExecution.getProcessDefinitionId())
                .getModelElementById(delegateExecution.getCurrentActivityId());
        message = receiveTask.getMessage().getName();
        String variableName = "";
        String query = "";
        try {
            BpmnModelInstance bpmModel = delegateExecution.getProcessEngine().getRepositoryService()
                    .getBpmnModelInstance(delegateExecution.getProcessDefinitionId());
            receiveTask = bpmModel.getModelElementById(delegateExecution.getCurrentActivityId());
            // Get documentation from dataReference, NOTE: Assumed only 1 dataRef with a
            // single documentation is found.
            for (DataInputAssociation dataInputAssociation : receiveTask.getDataInputAssociations()) {
                for (ItemAwareElement source : dataInputAssociation.getSources()) {
                    DataObjectReferenceImpl dataReference = bpmModel.getModelElementById(source.getId());
                    variableName = dataReference.getName();
                    for (Documentation documentation : dataReference.getDocumentations()) {
                        query = documentation.getRawTextContent();
                    }
                }
            }
        } catch (Exception e) {

            e.printStackTrace();
        }

        query = processQuery(query, accessor);
        /*
         * The task identifier in this case is, considered as the taskidentifier of the receive task,
         * it is because the receive task is not registered in the database in this prototype.
        */
        requestData(query, delegateExecution.getProcessInstanceId(), message, variableName, "receiveTask",
                false);

    }
    /*
     * this method is used notfify the wfc to create a user task in the FHIR
     * @param delegateTask the delegateTask object from  User Task
     */
    public boolean createUserTask(DelegateTask delegateTask) {

        try {
            HttpResponse<JsonNode> httpResponse = Unirest.post(properties.getProperty("wfc.url") +
                    "/RequestUserTask/" + delegateTask.getProcessInstanceId() + "/" +
                    delegateTask.getTaskDefinitionKey() + "/" + delegateTask.getId())
                    .asJson();
            if (httpResponse.getStatus() != 200) {
                logger.log(Level.WARNING, "Failed to create UserTask: {0}", httpResponse.getStatusText());
                return false;
            } else {
                logger.log(Level.INFO, "UserTask created successfully: {0} {1}", new Object[]{httpResponse.getStatus(), httpResponse.getStatus()});
                return true;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create UserTask: {0}", e.getMessage());
            return false;
        }
    }
    /*
     * This method is used to send the request to the wfc to request data from the FHIR via wfc service 
     * @param query the FHIR-query to be sent to the WFC service
     * @param processInstanceId: the process instance id of the process
     * @param messageName: the message name
     * @param variableName: the name (for boundary message event or data object
     * reference)
     * @param taskIdentifier: the task identifier
     *
     */

    private void requestData(String query, String processInstanceId, String messageName, String variableName,
        String taskIdentifier, boolean isInterupting) {
        //// Building the complete URL for the request.
        String url = properties.getProperty("wfc.url") + "/RequestObservationValue/"
            + processInstanceId + "/" + messageName + "/" + variableName + "/"
            + taskIdentifier + "/" + isInterupting;
        // Sending the request to the WFC service and Unirest is used to send a POST
        // request to the WFC service.
        Unirest.post(url).body(query).asJson();
    }

    /**
     * This method processes FHIR query expressions containing time references like "NOW", "MOMENT", or custom variables.
     * It handles parsing the base date/time, applying any specified offset, and formatting the final result.
     *
     * @param innerBaseValue The base date/time value from the expression (e.g., "NOW", "$VAR_NAME")
     * @param innerOffset  The optional offset string (e.g., "5h", "-2m") containing a value and unit
     * @param variableAccessor A way to access variables
     * @param dateTimeFormatter The desired format for the final output date/time string
     * @return The formatted date/time string after processing
     */
    private String HandleTimeExpression(String innerBaseValue, String innerOffset, VariableAccessor variableAccessor, DateTimeFormatter dateTimeFormatter) {

        // Determine the base date/time
        ZonedDateTime baseDateTime;
        if ("NOW".equals(innerBaseValue) || "$NOW".equals(innerBaseValue)) {
            baseDateTime = ZonedDateTime.now(); // Use current time
        } else {
            String variableValue = Optional.ofNullable(variableAccessor.getVariable(innerBaseValue.replace("$", "")))
                    .map(Object::toString)
                    .orElseThrow(() -> new IllegalArgumentException("Variable not found: " + innerBaseValue));
            baseDateTime = parseDate(variableValue); // Parse date from variable
        }

        // Apply offset if provided
        if (innerOffset != null) {
            int value = Integer.parseInt(innerOffset.substring(0, innerOffset.length() - 1));
            String unit = innerOffset.substring(innerOffset.length() - 1);
            baseDateTime = calculateDateTime(baseDateTime, value, unit);
        }

        // Format and return the final date/time string
        return baseDateTime.format(dateTimeFormatter);
    }

    /**
     * Parses a date/time string into a ZonedDateTime object, handling different formats.
     *
     * @param dateStr The date/time string to parse
     * @return The parsed ZonedDateTime object
     */
    private ZonedDateTime parseDate(String dateStr) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

        try {
            return ZonedDateTime.parse(dateStr, dateTimeFormatter); 
        } catch (DateTimeParseException e) {
            try {
                LocalDate localDate = LocalDate.parse(dateStr, dateFormatter); 
                return localDate.atStartOfDay(ZonedDateTime.now().getZone());
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Unsupported date format: " + dateStr);
            }
        }
    }

    /**
     * Calculates a new ZonedDateTime by adding a specified offset to the base date/time.
     *
     * @param baseDateTime The base ZonedDateTime object
     * @param value The offset value (positive or negative)
     * @param unit The unit of the offset (e.g., "h" for hours, "m" for minutes)
     * @return The new ZonedDateTime object with the applied offset
     */
    private ZonedDateTime calculateDateTime(ZonedDateTime baseDateTime, int value, String unit) {
        switch (unit) {
            case "h":
                return baseDateTime.plusHours(value);
            case "m":
                return baseDateTime.plusMinutes(value);
            case "s":
                return baseDateTime.plusSeconds(value);
            case "d":
                return baseDateTime.plusDays(value);
            default:
                throw new IllegalArgumentException("Unsupported time unit: " + unit);
        }
    }
}