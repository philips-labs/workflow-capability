package com.philips.healthsuite.workflowcapability.core.demos;

import ca.uhn.fhir.rest.api.MethodOutcome;
import com.philips.healthsuite.workflowcapability.core.fhirresources.FhirDataResources;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;


/**
 * Fills the FHIR Store with a patient and demo data for them.
 * <p>
 * This demo can be used with blood loss and sepsis v1.
 */
@Component
@PropertySource("classpath:application.properties")
public class DemoCreator3 {
    @Value("${config.fhirUrl}")
    private String fhirUrl;
    private FhirDataResources fhirDataResources;


    /**
     * Fills the FHIR Store with dummy data, such that the prototype can be run.
     */
    public void run() {
        this.fhirDataResources = new FhirDataResources(this.fhirUrl + "/fhir");

        DemoPatientCreator demoPatientCreator = new DemoPatientCreator();
        Patient patient = demoPatientCreator.createPatient("Joe", "the Third", 3);
        MethodOutcome patientOutcome = this.fhirDataResources.addResource(patient);

        addVolumeObservation(patientOutcome);
        addPTObservation(patientOutcome);
        addAPTObservation(patientOutcome);
        addINRObservation(patientOutcome);
        addPlateletsObservation(patientOutcome);
    }


    private void addPlateletsObservation(MethodOutcome patientOutcome) {
        Observation platObservation = new Observation();
        CodeableConcept platCategory = new CodeableConcept();
        platCategory.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category") // URI that identifies this terminology.
                .setCode("vital-signs")
                .setDisplay("vital-signs");
        platObservation.addCategory(platCategory);
        CodeableConcept platCode = new CodeableConcept();
        platCode.addCoding()
                .setSystem("http://loinc.org")
                .setCode("26515-7")
                .setDisplay("Platelets [#/volume] in Blood");
        platObservation.setCode(platCode);
        platObservation.setSubject(new Reference().setReference("Patient/" + patientOutcome.getId().getIdPart()));
        platObservation.setValue(new Quantity()
                .setValue(23)
                .setUnit("{}")
                .setSystem("http://unitsofmeasure.org")
                .setCode("{}"));
        fhirDataResources.addResource(platObservation);
    }

    private void addINRObservation(MethodOutcome patientOutcome) {
        Observation inrObservation = new Observation();
        CodeableConcept inrCategory = new CodeableConcept();
        inrCategory.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("vital-signs")
                .setDisplay("vital-signs");
        inrObservation.addCategory(inrCategory);
        CodeableConcept INRcode = new CodeableConcept();
        INRcode.addCoding()
                .setSystem("http://loinc.org")
                .setCode("34714-6")
                .setDisplay("Platelets [#/volume] in Blood");
        inrObservation.setCode(INRcode);
        inrObservation.setSubject(new Reference().setReference("Patient/" + patientOutcome.getId().getIdPart()));
        inrObservation.setValue(new Quantity()
                .setValue(1.4)
                .setUnit("{}")
                .setSystem("http://unitsofmeasure.org")
                .setCode("{}"));
        fhirDataResources.addResource(inrObservation);
    }

    private void addAPTObservation(MethodOutcome patientOutcome) {
        Observation aptObservation = new Observation();
        CodeableConcept aptCategory = new CodeableConcept();
        aptCategory.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("vital-signs")
                .setDisplay("vital-signs");
        aptObservation.addCategory(aptCategory);
        CodeableConcept aptCode = new CodeableConcept();
        aptCode.addCoding()
                .setSystem("http://loinc.org")
                .setCode("3173-2")
                .setDisplay("aPTT in Blood by Coagulation assay");
        aptObservation.setCode(aptCode);
        aptObservation.setSubject(new Reference().setReference("Patient/" + patientOutcome.getId().getIdPart()));
        aptObservation.setValue(new Quantity()
                .setValue(34)
                .setUnit("{s}")
                .setSystem("http://unitsofmeasure.org")
                .setCode("{s}"));
        fhirDataResources.addResource(aptObservation);
    }


    public void addVolumeObservation(MethodOutcome patientOutcome) {
        Observation volumeObservation = new Observation();
        CodeableConcept volumeCategory = new CodeableConcept();
        volumeCategory.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("vital-signs")
                .setDisplay("vital-signs");
        volumeObservation.addCategory(volumeCategory);
        CodeableConcept volumeCode = new CodeableConcept();
        volumeCode.addCoding()
                .setSystem("http://loinc.org")
                .setCode("81661-1")
                .setDisplay("Blood loss volume measured");
        volumeObservation.setCode(volumeCode);
        volumeObservation.setSubject(new Reference().setReference("Patient/" + patientOutcome.getId().getIdPart()));
        volumeObservation.setValue(new Quantity()
                .setValue(199)
                .setUnit("{s}")
                .setSystem("http://unitsofmeasure.org")
                .setCode("{s}"));
        fhirDataResources.addResource(volumeObservation);
    }

    public void addPTObservation(MethodOutcome patientOutcome) {
        Observation PTObservation = new Observation();
        CodeableConcept PTcategory = new CodeableConcept();
        PTcategory.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("vital-signs")
                .setDisplay("vital-signs");
        PTObservation.addCategory(PTcategory);
        CodeableConcept code = new CodeableConcept();
        code.addCoding()
                .setSystem("http://loinc.org")
                .setCode("5902-2")
                .setDisplay("Prothrombin time");
        PTObservation.setCode(code);
        PTObservation.setSubject(new Reference().setReference("Patient/" + patientOutcome.getId().getIdPart()));
        PTObservation.setValue(new Quantity()
                .setValue(17)
                .setUnit("{s}")
                .setSystem("http://unitsofmeasure.org")
                .setCode("{s}"));
        fhirDataResources.addResource(PTObservation);
    }
}
