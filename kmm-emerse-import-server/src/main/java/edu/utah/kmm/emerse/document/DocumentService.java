package edu.utah.kmm.emerse.document;

import edu.utah.kmm.emerse.database.DatabaseService;
import edu.utah.kmm.emerse.fhir.FhirService;
import edu.utah.kmm.emerse.patient.PatientService;
import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.dstu3.model.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class DocumentService {

    @Autowired
    private FhirService fhirService;

    @Autowired
    private PatientService patientService;

    public DocumentReference getDocumentById(String docid) {
        return fhirService.readResource(DocumentReference.class, docid);
    }

    public List<DocumentReference> getDocumentsForPatient(String patid) {
        List<DocumentReference> documents = new ArrayList<>();
        Bundle bundle = fhirService.getGenericClient().search()
                .forResource(DocumentReference.class)
                .where(DocumentReference.PATIENT.hasId(patid))
                .where(DocumentReference.CLASS.exactly().code("clinical-notes"))
                .returnBundle(Bundle.class)
                .execute();

        for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
            Resource resource = entry.getResource();

            if (resource instanceof DocumentReference) {
                documents.add((DocumentReference) resource);
            }
        }

        return documents;
    }

    /**
     * Return the MRN of the subject of a document.
     *
     * @param document The document.
     * @return The MRN of the subject, or null if not found.
     */
    public String extractMRN(DocumentReference document) {
        Resource res = document.getSubjectTarget();

        if (res instanceof Patient) {
            return patientService.extractMRN((Patient) res);
        }

        String patid = extractPatientId(document);

        if (patid != null) {
            Patient patient = patientService.getPatientById(patid);
            document.setSubjectTarget(patient);
            return patientService.extractMRN(patient);
        }

        return null;
    }

    /**
     * Return the FHIR id of the subject of a document.
     *
     * @param document The document.
     * @return The FHIR id of the subject, or null if not found.
     */
    public String extractPatientId(DocumentReference document) {
        Resource res = document.getSubjectTarget();

        if (res instanceof Patient) {
            return res.getId();
        }

        if (document.hasSubject()) {
            Reference ref = document.getSubject();
            String id = ref.getId();
            return id == null ? StringUtils.substringAfterLast(ref.getReference(), "/") : id;
        }

        return null;
    }

    public ContentDTO getDocumentContent(DocumentReference document) {
        if (!document.getContent().isEmpty()) {
            DocumentReference.DocumentReferenceContentComponent content = document.getContentFirstRep();
            Attachment attachment = content.getAttachment();

            if (!attachment.getDataElement().isEmpty()) {
                return new ContentDTO(attachment.getData(), attachment.getContentType());
            }

            if (!attachment.getUrlElement().isEmpty()) {
                Binary data = fhirService.getGenericClient().read().resource(Binary.class).withUrl(attachment.getUrl()).execute();
                return new ContentDTO(data.getContentAsBase64(), data.getContentType());
            }
        }

        return null;
    }


}
