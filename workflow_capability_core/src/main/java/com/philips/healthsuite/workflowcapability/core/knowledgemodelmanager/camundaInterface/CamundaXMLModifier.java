package com.philips.healthsuite.workflowcapability.core.knowledgemodelmanager.camundaInterface;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;


public class CamundaXMLModifier {

    Document doc;
    String fileName;

    public CamundaXMLModifier(File xmlFile) throws ParserConfigurationException, SAXException, IOException {

        String wholeFileName = xmlFile.getName();
        fileName = wholeFileName.substring(0, wholeFileName.length() - 3);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = factory.newDocumentBuilder();
        this.doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        Element bpmnDefinitions = (Element) doc.getFirstChild();
        bpmnDefinitions.setAttribute("xmlns:camunda", "http://camunda.org/schema/1.0/bpmn");

    }

    /**
     * Adds the StartEventDelegate reference to the Start Event in the BPMN XML Definition
     */
    public void addStartEventListener() {

        Node startEvent = this.getStartEvent();
        addListenerToNode(startEvent, "org.camunda.bpm.delegate.StartEventDelegate", "startExecutionListener");


    }

    /**
     * Adds the StartEventDelegate reference to the User Task in the BPMN XML Definition
     */
    public void addUserTaskListener() {

        NodeList userTasks = this.getUserTasks();
        for (int i = 0; i < userTasks.getLength(); i++) {
            addListenerToNode(userTasks.item(i), "org.camunda.bpm.delegate.UserTaskEntry", "taskListener");
        }

    }

    /**
     * Adds the StartEventDelegate reference to the Receive Task in the BPMN XML Definition
     */
    public void addReceiveTaskListener() {

        NodeList receiveTasks = this.getReceiveTasks();
        for (int i = 0; i < receiveTasks.getLength(); i++) {
            Node receiveTask = receiveTasks.item(i);
            Boolean hasDataInput = false;
            // Check if the ReceiveTask is requesting Data from FHIR
            if (receiveTask.hasChildNodes()) {
                NodeList childNodes = receiveTask.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    if (childNodes.item(j).getNodeName() == "bpmn:dataInputAssociation") {
                        hasDataInput = true;
                    }
                }
            }
            if (hasDataInput) {
                addListenerToNode(receiveTask, "org.camunda.bpm.delegate.ReceiveTaskEntry", "startExecutionListener");
            }
        }

    }

    private NodeList getReceiveTasks() {
        return doc.getElementsByTagName("bpmn:receiveTask");
    }


    private NodeList getUserTasks() {
        return doc.getElementsByTagName("bpmn:userTask");
    }

    public void addListenerToNode(Node node, String classString, String type) {
        Boolean hasExtensionElements = false;
        Boolean hasDocumentation = false;
        Node extension = null;

        if (node.hasChildNodes()) {
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                if (childNodes.item(i).getNodeName().equals("bpmn:extensionElements")) {
                    hasExtensionElements = true;
                    extension = childNodes.item(i);
                }
                if (childNodes.item(i).getNodeName().equals("bpmn:documentation")) {
                    hasDocumentation = true;
                }
            }
        }
        Element listener;
        switch (type) {
            case "taskListener":
                listener = doc.createElement("camunda:taskListener");
                listener.setAttribute("event", "create");
                break;
            case "startExecutionListener":
                listener = doc.createElement("camunda:executionListener");
                listener.setAttribute("event", "start");
                break;
            case "endExecutionListener":
                listener = doc.createElement("camunda:executionListener");
                listener.setAttribute("event", "end");
                break;
            default:
                listener = doc.createElement("camunda:taskListener");
                listener.setAttribute("event", "create");

        }
        listener.setAttribute("class", classString);


        if (!hasExtensionElements) {
            extension = doc.createElement("bpmn:extensionElements");
            extension.appendChild(listener);
            if (hasDocumentation) {
                node.insertBefore(extension, node.getChildNodes().item(2));
            } else {
                node.insertBefore(extension, node.getFirstChild());
            }
        } else {
            extension.appendChild(listener);
        }
    }

    /**
     * Gets the start event from the BPMN model
     *
     * @return DOM Node containing the startEvent XML data
     */
    public Node getStartEvent() {

        NodeList nList = doc.getElementsByTagName("bpmn:startEvent");
        if (nList.getLength() > 0) {
            return nList.item(0);
        }
        return null;

    }

    /**
     * Saves the modified BPMN Model to a .bpmn file with the _modified tag
     */
    public File saveBPMNModel() {

        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            File newFile = new File(".modified_models\\" + fileName + "_modified.bpmn");
            Result output = new StreamResult(newFile);
            Source input = new DOMSource(doc);
            transformer.transform(input, output);
            return newFile;
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerFactoryConfigurationError e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addEndEventListener() {

        Node endEvent = this.getEndEvent();
        addListenerToNode(endEvent, "org.camunda.bpm.delegate.FinishWorkflow", "endExecutionListener");
    }

    private Node getEndEvent() {
        NodeList nList = doc.getElementsByTagName("bpmn:endEvent");
        if(nList.getLength() > 0) {
            for(int i = 0; i < nList.getLength(); i++) {
                NamedNodeMap attributes = nList.item(i).getAttributes();
                if(attributes != null && attributes.getNamedItem("name") != null && attributes.getNamedItem("name").getNodeValue().equalsIgnoreCase("END")) {
                    return nList.item(i);
                }
            }

            return nList.item(0);
        }
        return null;
    }
}
