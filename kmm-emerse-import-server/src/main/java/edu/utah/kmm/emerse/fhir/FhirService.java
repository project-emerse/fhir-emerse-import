package edu.utah.kmm.emerse.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import edu.utah.kmm.emerse.auth.AuthenticatorRegistry;
import edu.utah.kmm.emerse.auth.IAuthenticator;
import edu.utah.kmm.emerse.document.ContentDTO;
import edu.utah.kmm.emerse.patient.IPatientLookup;
import edu.utah.kmm.emerse.patient.PatientLookupRegistry;
import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * FHIR-related services.
 */
public class FhirService {

    @Value("${fhir.mrn.system}")
    private String mrnSystem;

    @Value("${fhir.server.root}")
    private String fhirRoot;

    @Value("${fhir.server.authtype}")
    private String authenticationType;

    @Value("${fhir.server.patientlookup:DEFAULT}")
    private String patientLookupType;

    @Value("${fhir.server.headers:}")
    private String extraHeaders;

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private Credentials fhirServiceCredentials;

    @Autowired
    private AuthenticatorRegistry authenticatorRegistry;

    @Autowired
    private PatientLookupRegistry patientLookupRegistry;

    private IGenericClient genericClient;

    private IAuthenticator authenticator;

    private IPatientLookup patientLookup;

    private CapabilityStatement capabilityStatement;

    public FhirService() {
    }

    private void init() {
        initGenericClient();
        authenticator = authenticatorRegistry.get(authenticationType);
        Assert.notNull(authenticator, "Unrecognized authentication scheme: " + authenticationType);
        initialize(authenticator);
        patientLookup = patientLookupRegistry.get(patientLookupType);
        Assert.notNull(patientLookup, "Unknown patient lookup plugin: " + patientLookupType);
        initialize(patientLookup);
    }

    private void initialize(IInitializable target) {
        target.initialize(this);
    }

    private void initGenericClient() {
        genericClient = fhirContext.newRestfulGenericClient(fhirRoot);
        genericClient.setEncoding(EncodingEnum.JSON);

        if (!extraHeaders.isEmpty()) {
            AdditionalRequestHeadersInterceptor interceptor = new AdditionalRequestHeadersInterceptor();

            for(String header: extraHeaders.split("\\n")) {
                String[] pcs = header.split("\\:", 2);
                interceptor.addHeaderValue(pcs[0].trim(), pcs.length == 2 ? pcs[1].trim() : "");
            }

            genericClient.registerInterceptor(interceptor);
        }

        capabilityStatement = genericClient.capabilities().ofType(CapabilityStatement.class).execute();
    }

    public IGenericClient getGenericClient() {
        return genericClient;
    }

    public Credentials getCredentials() {
        return fhirServiceCredentials;
    }

    public CapabilityStatement getCapabilityStatement() {
        return capabilityStatement;
    }

    public Patient getPatient(String id, IdentifierType type) {
        Assert.isTrue(type == IdentifierType.MRN || type == IdentifierType.PATID, "Invalid identifier type.");
        return type == IdentifierType.MRN ? getPatientByMrn(id) : getPatientById(id);
    }

    public Patient getPatientByMrn(String mrn) {
        return patientLookup.lookupByMrn(mrn);
    }

    public String extractMRN(Patient patient) {
        for (Identifier identifier: patient.getIdentifier()) {
            if (mrnSystem.equals(identifier.getSystem())) {
                return identifier.getValue();
            }
        }

        return null;
    }

    public <T extends IBaseResource> T readResource(Class<T> type, String fhirId) {
        try {
            return genericClient.read()
                    .resource(type)
                    .withId(fhirId)
                    .execute();
        } catch (Exception e) {
            return null;
        }
    }

    public Patient getPatientById(String patid) {
        return readResource(Patient.class, patid);
    }

    public DocumentReference getDocumentById(String docid) {
        return readResource(DocumentReference.class, docid);
    }

    public List<DocumentReference> getDocumentsForPatient(String patid) {
        List<DocumentReference> documents = new ArrayList<>();
        Bundle bundle = genericClient.search()
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
     * Return the FHIR id of the subject of a document.
     *
     * @param document The document.
     * @return The FHIR id of the subject, or null if not found.
     */
    public String getPatientId(DocumentReference document) {
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
     * Return the MRN of the subject of a document.
     *
     * @param document The document.
     * @return The MRN of the subject, or null if not found.
     */
    public String getPatientMrn(DocumentReference document) {
        Resource res = document.getSubjectTarget();

        if (res instanceof Patient) {
            return extractMRN((Patient) res);
        }

        String patid = getPatientId(document);

        if (patid != null) {
            Patient patient = getPatientById(patid);
            document.setSubjectTarget(patient);
            return extractMRN(patient);
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
                Binary data = genericClient.read().resource(Binary.class).withUrl(attachment.getUrl()).execute();
                return new ContentDTO(data.getContentAsBase64(), data.getContentType());
            }
        }

        return null;
    }

    public String serialize(IBaseResource resource) {
        return resource == null ? null : genericClient.getFhirContext().newJsonParser().encodeResourceToString(resource);
    }

    public <T extends IBaseResource> T deserialize(String data, Class<T> resourceType) {
        return data == null ? null : (T) genericClient.getFhirContext().newJsonParser().parseResource(data);
    }

}
