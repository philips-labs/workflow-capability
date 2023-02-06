package com.philips.healthsuite.workflowcapability.core.demos;

import com.philips.healthsuite.workflowcapability.core.identification.PatientIdAutogenerator;
import org.hl7.fhir.r4.model.*;

import java.util.Calendar;
import java.util.List;

class DemoPatientCreator {


    protected DemoPatientCreator() {
    }


    /**
     * Creates a demo patient and adds them in FHIR Store.
     *
     * @return
     */
    protected Patient createPatient(String givenName, String familyName, int birthDay) {
        Patient patient = new Patient();
        Calendar dateOfBirth = Calendar.getInstance();
        dateOfBirth.set(1972, Calendar.AUGUST, birthDay, 0, 0, 0);

        Address address = new Address()
                .setUse(Address.AddressUse.HOME)
                .setType(Address.AddressType.BOTH)
                .setText("Groene Loper 5, Eindhoven, 5612 AE")
                .setLine(List.of(new StringType("Groene Loper 5")))
                .setCity("Eindhoven")
                .setDistrict("Noord-Brabant")
                .setState("Netherlands")
                .setPeriod(new Period().setStart(dateOfBirth.getTime()));

        ContactPoint meansOfCommunication = new ContactPoint()
                .setSystem(ContactPoint.ContactPointSystem.PHONE)
                .setValue("+31 6 12345678")
                .setUse(ContactPoint.ContactPointUse.HOME)
                .setRank(1);

        CodeableConcept type = new CodeableConcept();
        type.addCoding().setSystem("http://hl7.org/fhir/v2/0203").setCode("WorkFlowTest").setDisplay("dummyDisplay");
        patient.addIdentifier(new Identifier()
                        .setType(type)
                        .setSystem("http://www.philips.com/fhir/identifier-type/MR")
                        .setValue(PatientIdAutogenerator.generatePatientId())
                        .setAssigner(new Reference().setReference("Organization/Philips.CIS.ADT"))
                )
                .addName(new HumanName().setUse(HumanName.NameUse.USUAL).addGiven(givenName).setFamily(familyName))
                .setGender(Enumerations.AdministrativeGender.MALE)
                .setAddress(List.of(address))
                .setTelecom(List.of(meansOfCommunication))
                .setBirthDate(dateOfBirth.getTime());

        return patient;
    }
}
