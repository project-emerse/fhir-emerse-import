package edu.utah.kmm.emerse.patient;

import edu.utah.kmm.emerse.fhir.FhirService;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class DefaultPatientLookup implements IPatientLookup {

    @Value("${fhir.mrn.system}")
    private String mrnSystem;

    @Autowired
    private FhirService fhirService;

    @Override
    public String getName() {
        return "DEFAULT";
    }

    @Override
    public Patient lookupByMRN(String mrn) {
        Bundle bundle = fhirService.getGenericClient().search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(mrnSystem, mrn))
                .returnBundle(Bundle.class)
                .execute();

        return (Patient) bundle.getEntryFirstRep().getResource();
    }

}
