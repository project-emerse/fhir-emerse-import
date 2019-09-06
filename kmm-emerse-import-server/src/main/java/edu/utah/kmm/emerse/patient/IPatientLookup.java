package edu.utah.kmm.emerse.patient;

import edu.utah.kmm.emerse.fhir.IInitializable;
import org.hl7.fhir.dstu3.model.Patient;

public interface IPatientLookup extends IInitializable {

    String getName();

    Patient lookupByMRN(String mrn);

}
