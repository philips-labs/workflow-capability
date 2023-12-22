package com.philips.healthsuite.workflowcapability.core.knowledgemodelmanager;

import ca.uhn.fhir.rest.api.MethodOutcome;
import com.philips.healthsuite.workflowcapability.core.wfcservice.EngineInterface;
import com.philips.healthsuite.workflowcapability.core.knowledgemodelmanager.camundaInterface.CamundaXMLModifier;
import com.philips.healthsuite.workflowcapability.core.wfcservice.EngineInterfaceFactory;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

@RestController
@PropertySource("classpath:application.properties")
public class KnowledgeModelController {
    EngineInterfaceFactory engineInterfaceFactory;

    @Value("${config.fhirUrl}")
    String fhirUrl;


    KnowledgeModelController() {
        this.engineInterfaceFactory = new EngineInterfaceFactory();
    }


    /**
     * Deploys a BPMN model in Camunda.
     *
     * @param file
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    @RequestMapping(
            value = "/DeployBpmn",
            method = RequestMethod.POST)
    String deployBpmnDiagram(@RequestParam("file") MultipartFile file) throws IOException, ParserConfigurationException, SAXException {
        File bpmnModel = convertMultiFileToFile(file);

        try {
            String fhirPlanDefinitionName = createFHIRObjects(bpmnModel);
            File modifiedFileName = addCamundaTags(bpmnModel);
            deployModel(modifiedFileName, fhirPlanDefinitionName);
            return "Model successfully deployed";
        } catch (IOException e) {
            return e.toString();
        } catch (SAXException e) {
            return e.toString();
        } catch (ParserConfigurationException e) {
            return e.toString();
        }
    }


    /**
     * Deploys a DMN model in Camunda.
     *
     * @param file
     * @return
     * @throws IOException
     */
    @RequestMapping(
            value = "/DeployDmn",
            method = RequestMethod.POST)
    String deployDmnDiagram(@RequestParam("file") MultipartFile file) throws IOException {
        File dmnModel = convertMultiFileToFile(file);

        // Transforming input model file to XML, saving, and deploying it.
        try {
            // Transforming to XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = factory.newDocumentBuilder();
            Document doc = dBuilder.parse(dmnModel);
            doc.getDocumentElement().normalize();
            Source input = new DOMSource(doc);

            // Saving model
            String wholeFileName = dmnModel.getName();
            String fileName = wholeFileName.substring(0, wholeFileName.length() - 3);
            File newFile = new File(".modified_models\\" + fileName + "_modified.dmn");
            Result output = new StreamResult(newFile);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(input, output);

            // Deploying model
            deployModel(newFile, newFile.getName());
            return "Model successfully deployed";
        } catch (Exception e) {
            e.printStackTrace();
            return "Problem with deploying model";
        }

//        File newFile = new File("src\\main\\resources\\" + dmnModel.getName() + "_modified.dmn");
//        boolean success = dmnModel.renameTo(newFile);
//        deployModel(newFile, newFile.getName());
//        if(success) {
//            return "Model successfully deployed";
//        } else {
//            return "Problem with deploying model";
//        }
    }


    /**
     * This service creates a new patient
     *
     * @param givenName
     * @return
     */
    @RequestMapping(
            value = "/CreateNewPatient",
            method = RequestMethod.GET)
    // TODO: Change to POST
    String createNewPatient(
            @RequestParam("givenName") String givenName,
            @RequestParam("familyName") String familyName,
            @RequestParam("gender") String gender,
            @RequestParam("streetName") String streetName,
            @RequestParam("streetNumber") String streetNumber,
            @RequestParam("city") String city,
            @RequestParam("postalCode") String postalCode,
            @RequestParam("district") String district,
            @RequestParam("state") String state,
            @RequestParam("yearOfBirth") int yearOfBirth,
            @RequestParam("monthOfBirth") int monthOfBirth,
            @RequestParam("dayOfBirth") int dayOfBirth,
            @RequestParam("phoneNumber") String phoneNumber
    ) {

        Address address;
        ContactPoint meansOfCommunication;
        HumanName humanName;
        Calendar dateOfBirth;
        Enumerations.AdministrativeGender genderParsed;

        try {
            dateOfBirth = Calendar.getInstance();
            dateOfBirth.set(yearOfBirth, monthOfBirth, dayOfBirth, 0, 0, 0);

            address = new Address()
                    .setUse(Address.AddressUse.HOME)
                    .setType(Address.AddressType.BOTH)
                    .setText(streetName + " " + streetNumber + ", " + city + ", " + postalCode)
                    .setLine(List.of(new StringType(streetName + " " + streetNumber)))
                    .setCity(city)
                    .setDistrict(district)
                    .setState(state)
                    .setPeriod(new Period().setStart(dateOfBirth.getTime()));

            meansOfCommunication = new ContactPoint()
                    .setSystem(ContactPoint.ContactPointSystem.PHONE)
                    .setValue(phoneNumber)
                    .setUse(ContactPoint.ContactPointUse.HOME)
                    .setRank(1);

            humanName = new HumanName().addGiven(givenName).setFamily(familyName).setUse(HumanName.NameUse.USUAL);


            switch (gender) {
                case ("male"):
                    genderParsed = Enumerations.AdministrativeGender.MALE;
                    break;
                case ("female"):
                    genderParsed = Enumerations.AdministrativeGender.FEMALE;
                    break;
                default:
                    throw new UnrecognizedOptionException("Unmatched gender: " + gender);
            }
        } catch (Exception e) {
            System.err.println("Error while parsing input parameters for patient creation: " + e);
            return "Error while parsing input parameters for patient creation.";
        }

        try {
            PatientCreator patientCreator = new PatientCreator(fhirUrl + "/fhir");
            MethodOutcome patientCreationOutcome = patientCreator.createNewPatient(
                    humanName, genderParsed, dateOfBirth,
                    List.of(address), List.of(meansOfCommunication)
            );
        } catch (Exception e) {
            System.err.println("Exception during creating patient: " + e.toString());
            return "Problem with creating new patient.";
        }

        return "Patient created successfully.";
    }


    /**
     * Converts MultiPartFile to File. Also adds 'model' as prefix and 'tmp' as suffix.
     *
     * @param multipartFile
     * @return
     * @throws IOException
     */
    private File convertMultiFileToFile(MultipartFile multipartFile) throws IOException {
        File file = File.createTempFile("model", "tmp");
        multipartFile.transferTo(file);
        return file;
    }


    /**
     * @param xmlFile
     * @return
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private File addCamundaTags(File xmlFile) throws IOException, SAXException, ParserConfigurationException {
        CamundaXMLModifier modifier = new CamundaXMLModifier(xmlFile);
        modifier.addStartEventListener();
        modifier.addUserTaskListener();
        modifier.addServiceTaskListener();
        modifier.addReceiveTaskListener();
        modifier.addEndEventListener();
        return modifier.saveBPMNModel();
    }


    /**
     * Gets a BPMN XML file, ..., and returns the plan definition name.
     *
     * @param bpmnXmlFile
     * @return
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private String createFHIRObjects(File bpmnXmlFile) throws IOException, SAXException, ParserConfigurationException {
        FhirObjectCreator objectCreator = new FhirObjectCreator(fhirUrl + "/fhir");
        BPMNElementExtractor bpmnExtractorForProvidedModel = new BPMNElementExtractor(bpmnXmlFile);
        PlanDefinition planDefinition = bpmnExtractorForProvidedModel.getDiagramInfo();
        String planID = objectCreator.createPlanDefinition(planDefinition);
        List<PlanDefinitionActionComponent> allUserTasks = bpmnExtractorForProvidedModel.getUserTasks();
//        List<PlanDefinitionActionComponent> allReceiveTasks = bpmnExtractorForProvidedModel.getReceiveTasks();
        objectCreator.addAllActionToPlan(planID, allUserTasks);
//        objectCreator.addAllActionToPlan(planID, allReceiveTasks); // This adds Receive tasks in the PlanDefinition. Comment this line if it introduces issues.
        return planDefinition.getName();
    }


    /**
     * Deploy model in Camunda.
     *
     * @param model
     * @param modelName
     * @throws IOException
     */
    private void deployModel(File model, String modelName) throws IOException {
        EngineInterface engineInterface = engineInterfaceFactory.getEngineInterface("CAMUNDA");
        engineInterface.deployModel(model, modelName);
    }
}
