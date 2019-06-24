package edu.utah.kmm.emerse.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.utah.kmm.emerse.model.DocumentContent;
import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * FHIR-related services.
 */
public class FhirClient {

    @Value("${fhir.mrn.system}")
    private String mrnSystem;

    private final IGenericClient fhirClient;

    private final IAuthenticator authenticator;

    private final IPatientLookup patientLookup;

    public FhirClient(IGenericClient fhirClient, IAuthenticator authenticator, IPatientLookup patientLookup) {
        this.fhirClient = fhirClient;
        this.authenticator = authenticator;
        this.patientLookup = patientLookup;
        // patientLookup.initialize(fhirClient, fhirServiceCredentials);
    }
    
    public Patient getPatientByMrn(String mrn) {
        return patientLookup.lookupByIdentifier(mrnSystem, mrn);
    }

    public Patient getPatientById(String patientId) {
        authenticator.authenticate(patientId);

        return fhirClient.read()
                .resource(Patient.class)
                .withId(patientId)
                .execute();
    }

    public Bundle getDocumentBundle(String patientId) {
        authenticator.authenticate(patientId);

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

    private String getPatientId(DocumentReference documentReference) {
        Resource res = documentReference.getSubjectTarget();

        if (res instanceof Patient) {
            return res.getId();
        }

        if (documentReference.hasSubject()) {
            Reference ref = documentReference.getSubject();
            String id = ref.getId();
            return id == null ? StringUtils.substringAfterLast(ref.getReference(), "/") : id;
        }

        return null;
    }

    public DocumentContent getDocumentContent(DocumentReference documentReference) {
        if (!documentReference.getContent().isEmpty()) {
            documentReference.getSubjectTarget().getId();
            DocumentReference.DocumentReferenceContentComponent content = documentReference.getContentFirstRep();
            Attachment attachment = content.getAttachment();

            if (!attachment.getDataElement().isEmpty()) {
                return new DocumentContent(attachment.getData(), attachment.getContentType());
            }

            if (!attachment.getUrlElement().isEmpty()) {
                authenticator.authenticate(getPatientId(documentReference));
                Binary data = fhirClient.read().resource(Binary.class).withUrl(attachment.getUrl()).execute();
                return new DocumentContent(data.getContent(), data.getContentType());
            }
        }

        return null;
    }

    public String extractMRN(Patient patient) {
        for (Identifier identifier: patient.getIdentifier()) {
            if (mrnSystem.equals(identifier.getSystem())) {
                return identifier.getValue();
            }
        }

        return null;
    }

    public String serialize(IBaseResource resource) {
        return resource == null ? null : fhirClient.getFhirContext().newJsonParser().encodeResourceToString(resource);
    }

    public <T extends IBaseResource> T deserialize(String data, Class<T> resourceType) {
        return data == null ? null : (T) fhirClient.getFhirContext().newJsonParser().parseResource(data);
    }

}
