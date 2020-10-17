package edu.utah.kmm.emerse.patient;

import org.hl7.fhir.dstu3.model.Patient;

public interface IPatientLookup {

    String getName();

    Patient lookupByMRN(String mrn);

}
