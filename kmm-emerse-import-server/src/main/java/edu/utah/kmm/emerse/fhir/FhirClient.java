package edu.utah.kmm.emerse.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import edu.utah.kmm.emerse.model.DocumentContent;
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
public class FhirClient {

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

    public FhirClient() {
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

    private void initialize(IInitializable initializable) {
        initializable.initialize(genericClient, fhirServiceCredentials);
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
    }

    public Patient getPatientByMrn(String mrn) {
        return patientLookup.lookupByIdentifier(mrnSystem, mrn);
    }

    public Patient getPatientById(String patientId) {
        return genericClient.read()
                .resource(Patient.class)
                .withId(patientId)
                .execute();
    }

    public Bundle getDocumentBundle(String patientId) {
        return genericClient.search()
                .forResource(DocumentReference.class)
                .where(DocumentReference.PATIENT.hasId(patientId))
                .where(DocumentReference.CLASS.exactly().code("clinical-notes"))
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
            DocumentReference.DocumentReferenceContentComponent content = documentReference.getContentFirstRep();
            Attachment attachment = content.getAttachment();

            if (!attachment.getDataElement().isEmpty()) {
                return new DocumentContent(attachment.getData(), attachment.getContentType());
            }

            if (!attachment.getUrlElement().isEmpty()) {
                Binary data = genericClient.read().resource(Binary.class).withUrl(attachment.getUrl()).execute();
                return new DocumentContent(data.getContentAsBase64(), data.getContentType());
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
        return resource == null ? null : genericClient.getFhirContext().newJsonParser().encodeResourceToString(resource);
    }

    public <T extends IBaseResource> T deserialize(String data, Class<T> resourceType) {
        return data == null ? null : (T) genericClient.getFhirContext().newJsonParser().parseResource(data);
    }

}
