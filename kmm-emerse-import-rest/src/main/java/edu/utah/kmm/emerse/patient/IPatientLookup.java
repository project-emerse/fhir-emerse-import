package edu.utah.kmm.emerse.patient;

import org.hl7.fhir.dstu3.model.Patient;

/**
 * Implemented by all patient lookup services.
 */
public interface IPatientLookup {

    String getName();

    Patient lookupByMRN(String mrn);

}
