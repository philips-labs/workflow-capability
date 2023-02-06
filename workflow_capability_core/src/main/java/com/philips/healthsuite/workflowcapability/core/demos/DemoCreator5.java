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
 * This demo can be used with sepsis v2.
 * <p>
 * This patient goes to surgery because of bad CT results.
 */
@Component
@PropertySource("classpath:application.properties")
public class DemoCreator5 {
    @Value("${config.fhirUrl}")
    private String fhirUrl;
    private FhirDataResources fhirDataResources;


    /**
     * Fills the FHIR Store with dummy data, such that the prototype can be run.
     */
    public void run() {
        this.fhirDataResources = new FhirDataResources(this.fhirUrl + "/fhir");

        DemoPatientCreator demoPatientCreator = new DemoPatientCreator();
        Patient patient = demoPatientCreator.createPatient("Joe", "the Fifth", 5);
        MethodOutcome patientOutcome = this.fhirDataResources.addResource(patient);

        addCtScanObservation(patientOutcome);
        addMriScanObservation(patientOutcome);
        addBloodTestObservation(patientOutcome);
        addBodyTemperatureObservation(patientOutcome);
        addHeartRateObservation(patientOutcome);
        addRespiratoryRateObservation(patientOutcome);
        addAlcoholObservation(patientOutcome);
    }


    /**
     * To add alcohol level to the fhir store
     */
    private void addAlcoholObservation(MethodOutcome patientOutcome) {
        Observation alcoholObservation = new Observation();
        CodeableConcept alcoholCategory = new CodeableConcept();
        alcoholCategory.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category") // URI that identifies this terminology.
                .setCode("laboratory-results")
                .setDisplay("laboratory-results");
        alcoholObservation.addCategory(alcoholCategory);
        CodeableConcept alcoholCode = new CodeableConcept();
        alcoholCode.addCoding()
                .setSystem("http://loinc.org")
                .setCode("11331-6")
                .setDisplay("Alcohol [#/volume] in Blood");
        alcoholObservation.setCode(alcoholCode);
        alcoholObservation.setSubject(new Reference().setReference("Patient/" + patientOutcome.getId().getIdPart()));
        alcoholObservation.setValue(new Quantity()
                .setValue(9)
                .setUnit("{}")
                .setSystem("http://unitsofmeasure.org")
                .setCode("{}"));
        fhirDataResources.addResource(alcoholObservation);
    }


    /**
     * To add blood result to the fhir store
     */
    private void addBloodTestObservation(MethodOutcome patientOutcome) {
        Observation bloodObservation = new Observation();
        CodeableConcept bloodCategory = new CodeableConcept();
        bloodCategory.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category") // URI that identifies this terminology.
                .setCode("laboratory-results")
                .setDisplay("laboratory-results");
        bloodObservation.addCategory(bloodCategory);
        CodeableConcept bldCode = new CodeableConcept();
        bldCode.addCoding()
                .setSystem("http://loinc.org")
                .setCode("85354-9")
                .setDisplay("Blood quality");
        bloodObservation.setCode(bldCode);
        bloodObservation.setSubject(new Reference().setReference("Patient/" + patientOutcome.getId().getIdPart()));
        bloodObservation.setValue(new Quantity()
                .setValue(15)
                .setUnit("{}")
                .setSystem("http://unitsofmeasure.org")
                .setCode("{}"));
        fhirDataResources.addResource(bloodObservation);
    }


    /**
     * To add CT scan result to the fhir store
     */
    private void addCtScanObservation(MethodOutcome patientOutcome) {
        Observation ctObservation = new Observation();
        CodeableConcept ctCategory = new CodeableConcept();
        ctCategory.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("Imaging-results")
                .setDisplay("Imaging-results");
        ctObservation.addCategory(ctCategory);
        CodeableConcept CTcode = new CodeableConcept();
        CTcode.addCoding()
                .setSystem("http://loinc.org")
                .setCode("29252-4")
                .setDisplay("CT image of patient");
        ctObservation.setCode(CTcode);
        ctObservation.setSubject(new Reference().setReference("Patient/" + patientOutcome.getId().getIdPart()));
        ctObservation.setValue(new StringType()
                .setValue("big")
        );
        fhirDataResources.addResource(ctObservation);
    }


    /**
     * To add MRI result to the fhir store
     */
    private void addMriScanObservation(MethodOutcome patientOutcome) {
        Observation mriObservation = new Observation();
        CodeableConcept mriCategory = new CodeableConcept();
        mriCategory.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("Imaging-results")
                .setDisplay("Imaging-results");
        mriObservation.addCategory(mriCategory);
        CodeableConcept mriCode = new CodeableConcept();
        mriCode.addCoding()
                .setSystem("http://loinc.org")
                .setCode("24629-8")
                .setDisplay("MRI image of patient");
        mriObservation.setCode(mriCode);
        mriObservation.setSubject(new Reference().setReference("Patient/" + patientOutcome.getId().getIdPart()));
        mriObservation.setValue(new StringType()
                .setValue("small")
        );
        fhirDataResources.addResource(mriObservation);
    }


    /**
     * To add Temperatature value of a patient to the fhir store
     */
    public void addBodyTemperatureObservation(MethodOutcome patientOutcome) {
        Observation TObservation = new Observation();
        CodeableConcept Tcategory = new CodeableConcept();
        Tcategory.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("vital-signs")
                .setDisplay("vital-signs");
        TObservation.addCategory(Tcategory);
        CodeableConcept code = new CodeableConcept();
        code.addCoding()
                .setSystem("http://loinc.org")
                .setCode("8310-5")
                .setDisplay("Temperature value [degrees fahrenheit] of a patient");
        TObservation.setCode(code);
        TObservation.setSubject(new Reference().setReference("Patient/" + patientOutcome.getId().getIdPart()));
        TObservation.setValue(new Quantity()
                .setValue(90)
                .setUnit("{s}")
                .setSystem("http://unitsofmeasure.org")
                .setCode("{s}"));
        fhirDataResources.addResource(TObservation);
    }


    /**
     * To add Heart rate of a patient to the fhir store
     */
    public void addHeartRateObservation(MethodOutcome patientOutcome) {
        Observation HRObservation = new Observation();
        CodeableConcept HRcategory = new CodeableConcept();
        HRcategory.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("vital-signs")
                .setDisplay("vital-signs");
        HRObservation.addCategory(HRcategory);
        CodeableConcept code = new CodeableConcept();
        code.addCoding()
                .setSystem("http://loinc.org")
                .setCode("8867-4")
                .setDisplay("Heart rate value of a patient");
        HRObservation.setCode(code);
        HRObservation.setSubject(new Reference().setReference("Patient/" + patientOutcome.getId().getIdPart()));
        HRObservation.setValue(new Quantity()
                .setValue(100)
                .setUnit("{s}")
                .setSystem("http://unitsofmeasure.org")
                .setCode("{s}"));
        fhirDataResources.addResource(HRObservation);
    }


    /**
     * To add respiratory rate  of a patient to the fhir store
     */
    public void addRespiratoryRateObservation(MethodOutcome patientOutcome) {
        Observation RRObservation = new Observation();
        CodeableConcept RRcategory = new CodeableConcept();
        RRcategory.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("vital-signs")
                .setDisplay("vital-signs");
        RRObservation.addCategory(RRcategory);
        CodeableConcept code = new CodeableConcept();
        code.addCoding()
                .setSystem("http://loinc.org")
                .setCode("9279-1")
                .setDisplay("Respiratory rate value of a patient");
        RRObservation.setCode(code);
        RRObservation.setSubject(new Reference().setReference("Patient/" + patientOutcome.getId().getIdPart()));
        RRObservation.setValue(new Quantity()
                .setValue(25)
                .setUnit("{s}")
                .setSystem("http://unitsofmeasure.org")
                .setCode("{s}"));
        fhirDataResources.addResource(RRObservation);
    }
}
