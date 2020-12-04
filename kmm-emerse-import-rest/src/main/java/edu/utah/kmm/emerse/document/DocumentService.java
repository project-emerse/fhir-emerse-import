package edu.utah.kmm.emerse.document;

import edu.utah.kmm.emerse.fhir.FhirService;
import edu.utah.kmm.emerse.fhir.IdentifierType;
import edu.utah.kmm.emerse.patient.PatientService;
import edu.utah.kmm.emerse.util.MiscUtil;
import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.dstu3.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for document-related operations.
 */
public class DocumentService {

    @Autowired
    private FhirService fhirService;

    @Autowired
    private PatientService patientService;

    @Value("${fhir.document.classes:clinical-notes}")
    private String documentClasses;

    /**
     * Returns a document given its FHIR id.
     *
     * @param docid The document's FHIR id.
     * @return The requested document.
     */
    public DocumentReference getDocumentById(String docid) {
        return fhirService.readResource(DocumentReference.class, docid);
    }

    /**
     * Returns all documents for the specified patient.
     *
     * @param id The patient identifier.
     * @param type The identifier type.
     * @return A list of documents for the specified patient (never null).
     */
    public List<DocumentReference> getDocumentsForPatient(
            String id,
            IdentifierType type) {
        MiscUtil.validateIdentiferType(type, IdentifierType.MRN, IdentifierType.PATID);

        if (type == IdentifierType.MRN) {
            id = patientService.getPatientByMrn(id).getId();
        }

        Bundle bundle = fhirService.getGenericClient().search()
                .forResource(DocumentReference.class)
                .where(DocumentReference.PATIENT.hasId(id))
                .where(DocumentReference.CLASS.exactly().code(documentClasses.replace(" ", "")))
                .returnBundle(Bundle.class)
                .execute();

        return bundle.getEntry().stream()
                .filter(entry -> entry.getResource() instanceof DocumentReference)
                .map(entry -> (DocumentReference) entry.getResource())
                .collect(Collectors.toList());
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

    /**
     * Fetches the content associated with a document.
     *
     * @param document The document.
     * @return A DTO containing the document content (null if no content).
     */
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
