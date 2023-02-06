package com.philips.healthsuite.workflowcapability.core.knowledgemodelmanager;

import ca.uhn.fhir.rest.api.MethodOutcome;
import com.philips.healthsuite.workflowcapability.core.fhirresources.FhirDataResources;
import com.philips.healthsuite.workflowcapability.core.identification.PatientIdAutogenerator;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;

import java.util.Calendar;
import java.util.List;
import java.util.Objects;

/**
 * Responsible for creating new patients and adding them to FHIR store
 *
 * @return
 */
public class PatientCreator {
    private final FhirDataResources fhirDataResources;


    public PatientCreator(String baseURL) {
        this.fhirDataResources = new FhirDataResources(baseURL);
    }


    /**
     * Creates a new patient and adds them to FHIR store
     *
     * @return
     */
    public MethodOutcome createNewPatient(
            final HumanName humanName,
            final Enumerations.AdministrativeGender gender,
            final Calendar dateOfBirth,
            final List<Address> addresses,
            final List<ContactPoint> meansOfCommunication
    ) {
        Objects.requireNonNull(humanName);
        Objects.requireNonNull(gender);
        Objects.requireNonNull(dateOfBirth);
        Objects.requireNonNull(addresses);
        Objects.requireNonNull(meansOfCommunication);

        checkHumanName(humanName);

        assert addresses.size() > 0;
        assert meansOfCommunication.size() > 0;

        CodeableConcept type = new CodeableConcept();
        type.addCoding()
                .setSystem("http://hl7.org/fhir/v2/0203")
//                .setDisplay(humanName.getGiven().get(0).getValue())
                .setCode("MR");

        Patient patient = new Patient();
        patient.addIdentifier(
                        new Identifier()
                                .setType(type)
                                .setSystem("http://www.philips.com/fhir/identifier-type/MR")
                                .setValue(PatientIdAutogenerator.generatePatientId())
                                .setAssigner(
                                        new Reference().setReference("Organization/Philips.CIS.ADT")
                                )
                )
                .addName(humanName)
                .setGender(gender)
                .setAddress(addresses)
                .setTelecom(meansOfCommunication)
                .setBirthDate(dateOfBirth.getTime());
        MethodOutcome patientOutcome = fhirDataResources.addResource(patient);
        return patientOutcome;
    }


    /**
     * Checks if names are not blank.
     *
     * @param humanName
     */
    private void checkHumanName(HumanName humanName) {
        humanName.getGiven().forEach(name -> StringUtils.isNotBlank(name.toString()));
        StringUtils.isNotBlank(humanName.getFamily());
    }
}
