package edu.utah.kmm.emerse.fhir;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.AttachmentDt;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.resource.Binary;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DocumentReference;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.IdentifierTypeCodesEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.utah.kmm.emerse.model.DocumentContent;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * FHIR-related services.
 */
public class FhirService {

    @Autowired
    private IGenericClient fhirClient;

    public List<DocumentReference> getDocuments(Patient patient) {
       List<DocumentReference> documents = new ArrayList<>();

        Bundle bundle = fhirClient.search()
                .forResource(DocumentReference.class)
                .where(DocumentReference.SUBJECT.hasId(patient.getId()))
                .returnBundle(Bundle.class)
                .execute();

        for (Bundle.Entry entry: bundle.getEntry()) {
            IResource resource = entry.getResource();

            if (resource instanceof DocumentReference) {
                documents.add((DocumentReference) resource);
            }
        }

        return documents;
    }

    public DocumentContent getDocumentContent(DocumentReference documentReference) {
        if (!documentReference.getContent().isEmpty()) {
            DocumentReference.Content content = documentReference.getContentFirstRep();
            AttachmentDt attachment = content.getAttachment();

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
        for (IdentifierDt identifier: patient.getIdentifier()) {
            if (identifier.getType().getValueAsEnum().contains(IdentifierTypeCodesEnum.MR)) {
                return identifier.getValue();
            }
        }

        return null;
    }
}
