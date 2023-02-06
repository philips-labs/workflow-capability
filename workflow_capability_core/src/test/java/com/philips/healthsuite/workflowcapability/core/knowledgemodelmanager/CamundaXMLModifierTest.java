package com.philips.healthsuite.workflowcapability.core.knowledgemodelmanager;

import com.philips.healthsuite.workflowcapability.core.knowledgemodelmanager.camundaInterface.CamundaXMLModifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CamundaXMLModifierTest {

    CamundaXMLModifier xmlModifier;

    @BeforeEach
    void setUp() throws ParserConfigurationException, IOException, SAXException {
        File xmlFile = new File("src/test/resources/test_case_diagram.bpmn");
        xmlModifier = new CamundaXMLModifier(xmlFile);
    }

    @Test
    @DisplayName("Ensure Start Event is fetched from the Test BPMN Model")
    void getStartEvent() {
        Node startEvent = xmlModifier.getStartEvent();
        assertEquals("bpmn:startEvent", startEvent.getNodeName());
        assertEquals("StartEvent_1", startEvent.getAttributes().getNamedItem("id").getNodeValue());
    }
}