package com.philips.healthsuite.workflowcapability.core.knowledgemodelmanager;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.StringType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helps convert BPMN models to FHIR models.
 */
public class BPMNElementExtractor {
    private final Document doc;


    public BPMNElementExtractor(File aBPMDefinition) throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = factory.newDocumentBuilder();
        this.doc = dBuilder.parse(aBPMDefinition);
        this.doc.getDocumentElement().normalize();

    }


    /**
     * Method that gets the Name and Identifier from the BPMN model
     *
     * @return ClinicalProcess Object containing BPMN model info
     */
    public PlanDefinition getDiagramInfo() {
        NodeList nList = doc.getElementsByTagName("bpmn:process");
        Node nNode = nList.item(0);
        PlanDefinition planDefinition = new PlanDefinition();
        if (nNode.getAttributes().getNamedItem("id") != null) {
            planDefinition.addIdentifier().setSystem("workflowId").setValue(nNode.getAttributes().getNamedItem("id").getNodeValue());
        }

        if (nNode.getAttributes().getNamedItem("name") != null) {
            planDefinition.setName(nNode.getAttributes().getNamedItem("name").getNodeValue());
        }
        return planDefinition;
    }


    /**
     * Method that retrieves all ReceiveTasks from the BPMN model
     *
     * @return List of ProcessTask Objects containing the BPMN model Receive Tasks
     */
    public List<PlanDefinitionActionComponent> getReceiveTasks() {

        NodeList nList = doc.getElementsByTagName("bpmn:receiveTask");
        List<PlanDefinitionActionComponent> taskList = new ArrayList<>();
        for (int i = 0; i < nList.getLength(); i++) {
            Node nNode = nList.item(i);
            PlanDefinitionActionComponent actionReceive = new PlanDefinitionActionComponent();
            String taskID = nNode.getAttributes().getNamedItem("id") != null ? nNode.getAttributes().getNamedItem("id").getNodeValue() : "";
            String taskName = nNode.getAttributes().getNamedItem("name") != null ? nNode.getAttributes().getNamedItem("name").getNodeValue() : "";

            List<PlanDefinition.PlanDefinitionActionDynamicValueComponent> dynamicValues = new ArrayList<>();
            ArrayList<String> queries = getDataAssociations(nNode);
            for (String query : queries) {
                PlanDefinition.PlanDefinitionActionDynamicValueComponent dynamicValue = new PlanDefinition.PlanDefinitionActionDynamicValueComponent();
                dynamicValue.setPathElement(new StringType(query));
                dynamicValues.add(dynamicValue);
            }

            actionReceive.setId(taskID);
            actionReceive.setTitle(taskName);
            actionReceive.setDynamicValue(dynamicValues);
            CodeableConcept type = new CodeableConcept();
            type.addCoding().setCode("receiveTask").setSystem("taskType");

            actionReceive.getCode().add(type);

            taskList.add(actionReceive);
        }

        return taskList;
    }


    /**
     * Method that retrieves all documentation (descriptions) from dataAssociations of a Node
     *
     * @param nNode Node to check dataAssociations for
     * @return List of dataAssociations
     */
    private ArrayList<String> getDataAssociations(Node nNode) {

        NodeList childNodes = nNode.getChildNodes();
        ArrayList<String> dataReferences = new ArrayList<>();
        ArrayList<String> descriptions = new ArrayList<>();

        // Get Data Reference Values
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeName() == "bpmn:dataInputAssociation") {
                NodeList dataAssociations = childNode.getChildNodes();
                for (int j = 0; j < dataAssociations.getLength(); j++) {
                    if (dataAssociations.item(j).getNodeName() == "bpmn:sourceRef") {
                        Node source = dataAssociations.item(j);
                        dataReferences.add(source.getTextContent());
                    }
                }
            }
        }

        // Get description of data References
        NodeList nList = doc.getElementsByTagName("bpmn:dataObjectReference");
        for (int i = 0; i < nList.getLength(); i++) {
            for (String reference : dataReferences) {
                if (reference.equals(nList.item(i).getAttributes().getNamedItem("id").getTextContent())) {
                    descriptions.add(nList.item(i).getTextContent().trim());
                    break;
                }
            }
        }
        return descriptions;
    }


    /**
     * Method that retrieves all UserTasks from the BPMN model
     *
     * @return List of ProcessTask Objects containing the BPMN model User Tasks
     */
    public List<PlanDefinitionActionComponent> getUserTasks() {
        NodeList nList = doc.getElementsByTagName("bpmn:userTask");
        List<PlanDefinitionActionComponent> taskList = new ArrayList<>();
        for (int i = 0; i < nList.getLength(); i++) {
            Node nNode = nList.item(i);
            PlanDefinitionActionComponent userTask = new PlanDefinitionActionComponent();
            String taskID = nNode.getAttributes().getNamedItem("id") != null ? nNode.getAttributes().getNamedItem("id").getNodeValue() : "";
            String taskName = nNode.getAttributes().getNamedItem("name") != null ? nNode.getAttributes().getNamedItem("name").getNodeValue() : "";
            NodeList nNodeChildren = nNode.getChildNodes();
            String taskDescription = "";
            for (int j = 0; j < nNodeChildren.getLength(); j++) {
                if (nNodeChildren.item(j).getNodeName() == "bpmn:documentation")
                    taskDescription = nNodeChildren.item(j).getTextContent();
            }
            userTask.setId(taskID);
            userTask.setTitle(taskName);
            userTask.setDescription(taskDescription);
            CodeableConcept type = new CodeableConcept();
            type.addCoding().setCode("userTask").setSystem("taskType");

            userTask.getCode().add(type);
            taskList.add(userTask);
        }

        return taskList;
    }


}
