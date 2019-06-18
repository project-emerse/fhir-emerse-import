package edu.utah.kmm.emerse.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.utah.kmm.emerse.model.DocumentContent;
import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.codec.binary.Base64;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FHIR-related services.
 */
public class FhirService {

    private static final String OAUTH_EXTENSION = "http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris";

    private static final String SCOPES = "patient/*.read";

    private final IGenericClient fhirClient;

    private final String mrnSystem;

    private URI authorizeEndpoint;

    private URI tokenEndpoint;

    private Credentials authorizationCredentials;

    private TokenResponse tokenResponse;

    public FhirService(
            FhirContext fhirContext,
            String fhirRoot,
            Credentials authorizationCredentials,
            String mrnSystem) throws Exception {
        this.fhirClient = fhirContext.newRestfulGenericClient(fhirRoot);
        this.fhirClient.setEncoding(EncodingEnum.JSON);
        this.authorizationCredentials = authorizationCredentials;
        this.mrnSystem = mrnSystem;
        getOAuthEndpoints();
        authenticate();
    }

    private void authenticate() {
        if (tokenResponse == null || tokenResponse.isExpired()) {
            tokenResponse = null;
            RestTemplate restTemplate = new RestTemplate();
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.set("grant_type", "client_credentials");
            params.set("scope", SCOPES);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic "
                    + Base64.encodeBase64String((authorizationCredentials.getUsername() + ":" + authorizationCredentials.getPassword()).getBytes()));
            RequestEntity<?> request = new RequestEntity<>(params, headers, HttpMethod.POST, tokenEndpoint, null);
            ResponseEntity<TokenResponse> response = restTemplate.exchange(request, TokenResponse.class);
            tokenResponse = response.getBody();
        }
    }

    private void getOAuthEndpoints() throws URISyntaxException {
        CapabilityStatement cp = fhirClient.capabilities().ofType(CapabilityStatement.class).execute();

        for (Extension ext: cp.getRest().get(0).getSecurity().getExtension()) {
            if (OAUTH_EXTENSION.equals(ext.getUrl())) {
                for (Extension ext2: ext.getExtension()) {
                    String url = ext2.getUrl();

                    if ("authorize".equals(url)) {
                        authorizeEndpoint = new URI(((UriType)ext2.getValue()).getValue());
                    } else if ("token".equals(url)) {
                        tokenEndpoint = new URI(((UriType)ext2.getValue()).getValue());
                    }
                }

                break;
            }
        }

        if (authorizeEndpoint == null) {
            throw new RuntimeException("Could not determine authorization endpoint");
        }
    }

    public String getMrnSystem() {
        return mrnSystem;
    }

    public Patient getPatientByMrn(String mrn) {
        authenticate();

        Bundle bundle = fhirClient.search()
            .forResource(Patient.class)
            .where(Patient.IDENTIFIER.exactly().systemAndCode(mrnSystem, mrn))
            .returnBundle(Bundle.class)
            .execute();

        return (Patient) bundle.getEntryFirstRep().getResource();
    }

    public Patient getPatientById(String id) {
        authenticate();

        return fhirClient.read()
                .resource(Patient.class)
                .withId(id)
                .execute();
    }

    public Bundle getDocumentBundle(String patientId) {
        authenticate();

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
                authenticate();
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

    public String serialize(IBaseResource resource) {
        return resource == null ? null : fhirClient.getFhirContext().newJsonParser().encodeResourceToString(resource);
    }

    public <T extends IBaseResource> T deserialize(String data, Class<T> resourceType) {
        return data == null ? null : (T) fhirClient.getFhirContext().newJsonParser().parseResource(data);
    }

}
