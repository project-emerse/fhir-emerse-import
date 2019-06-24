package edu.utah.kmm.emerse.fhir;

public class PatientLookupRegistry extends BaseRegistry<IPatientLookup> {

    public PatientLookupRegistry(IPatientLookup... lookups) {
        super(IPatientLookup.class, lookups);
    }

    @Override
    protected String getName(IPatientLookup entry) {
        return entry.getName();
    }
}
