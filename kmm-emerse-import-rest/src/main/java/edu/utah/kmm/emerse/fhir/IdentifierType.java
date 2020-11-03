package edu.utah.kmm.emerse.fhir;

/**
 * The type of an identifier.
 */
public enum IdentifierType {
    MRN,        // Patient medical record number
    PATID,      // Patient FHIR id
    DOCID       // DocumentReference FHIR id
}
