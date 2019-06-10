package edu.utah.kmm.emerse.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.utah.kmm.emerse.model.DocumentContent;
import org.hl7.fhir.dstu3.model.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * FHIR-related services.
 */
public class FhirService {

    @Autowired
    private IGenericClient fhirClient;

    private final String mrnSystem;

    public FhirService(String mrnSystem) {
        this.mrnSystem = mrnSystem;
    }

    public String getMrnSystem() {
        return mrnSystem;
    }

    public Patient getPatient(String mrn) {
        Bundle bundle = fhirClient.search()
            .forResource(Patient.class)
            .where(Patient.IDENTIFIER.exactly().systemAndCode(mrnSystem, mrn))
            .returnBundle(Bundle.class)
            .execute();

        return (Patient) bundle.getEntryFirstRep().getResource();
    }

    public Bundle getDocumentBundle(String patientId) {
        return fhirClient.search()
                .forResource(DocumentReference.class)
                .where(DocumentReference.SUBJECT.hasId(patientId))
                .returnBundle(Bundle.class)
                .execute();
    }

    public List<DocumentReference> getDocuments(String patientId) {
        List<DocumentReference> documents = new ArrayList<>();
        Bundle bundle = getDocumentBundle(patientId);

        for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
            Resource resource = entry.getResource();

            if (resource instanceof DocumentReference) {
                documents.add((DocumentReference) resource);
            }
        }

        return documents;
    }

    public DocumentContent getDocumentContent(DocumentReference documentReference) {
        if (!documentReference.getContent().isEmpty()) {
            DocumentReference.DocumentReferenceContentComponent content = documentReference.getContentFirstRep();
            Attachment attachment = content.getAttachment();

            if (!attachment.getDataElement().isEmpty()) {
                return new DocumentContent(attachment.getData(), attachment.getContentType());
            }

            if (!attachment.getUrlElement().isEmpty()) {
                Binary data = fhirClient.read().resource(Binary.class).withUrl(attachment.getUrl()).execute();
                return new DocumentContent(data.getContent(), data.getContentType());
            }
        }

        return null;
    }

    public String getMRN(Patient patient) {
        for (Identifier identifier: patient.getIdentifier()) {
            if (mrnSystem.equals(identifier.getSystem())) {
                return identifier.getValue();
            }
        }

        return null;
    }

    public String serialize(Resource resource) {
        return resource == null ? null : fhirClient.getFhirContext().newJsonParser().encodeResourceToString(resource);
    }
}