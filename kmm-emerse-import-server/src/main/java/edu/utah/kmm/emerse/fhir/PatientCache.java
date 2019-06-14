package edu.utah.kmm.emerse.fhir;

import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PatientCache {

    private Map<String, Patient> patientFromId = new ConcurrentHashMap<>();

    private Map<String, Patient> patientFromMrn = new ConcurrentHashMap<>();

    @Autowired
    private FhirService fhirService;

    public Patient patientFromId(String id) {
        if (patientFromId.containsKey(id)) {
            return patientFromId.get(id);
        } else {
            return fetchPatientById(id);
        }
    }

    private synchronized Patient fetchPatientById(String id) {
        if (patientFromId.containsKey(id)) {
            return patientFromId.get(id);
        } else {
            return addPatientById(fhirService.getPatientById(id), id);
        }
    }


    private Patient addPatientById(Patient patient, String id) {
        patientFromId.put(id, patient);
        String mrn = patient == null ? null : fhirService.getMRN(patient);

        if (mrn != null) {
            patientFromMrn.put(mrn, patient);
        }

        return patient;
    }

    public Patient patientFromMrn(String mrn) {
        if (patientFromMrn.containsKey(mrn)) {
            return patientFromMrn.get(mrn);
        } else {
            return fetchPatientByMrn(mrn);
        }
    }

    private synchronized Patient fetchPatientByMrn(String mrn) {
        if (patientFromMrn.containsKey(mrn)) {
            return patientFromMrn.get(mrn);
        } else {
            return addPatientByMrn(fhirService.getPatientByMrn(mrn), mrn);
        }
    }

    private Patient addPatientByMrn(Patient patient, String mrn) {
        patientFromMrn.put(mrn, patient);
        String id = patient == null ? null : patient.getId();

        if (id != null) {
            patientFromId.put(id, patient);
        }

        return patient;
    }

}
