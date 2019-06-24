package edu.utah.kmm.emerse.epic;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public class EpicService {

    private String apiRoot;

    private RestTemplate restTemplate = new RestTemplate();

    public EpicService(IGenericClient client, Credentials credentials) {
        BasicAuthenticationInterceptor interceptor = new BasicAuthenticationInterceptor(credentials.getUsername(), credentials.getPassword());
        restTemplate.getInterceptors().add(interceptor);
        String fhirRoot = client.getServerBase();
        apiRoot = StringUtils.substringBeforeLast(fhirRoot, "/api/") + "/api/";
    }

    public <T> T get(String url, MultiValueMap<String, String> params, Class<T> returnType) {
        RequestEntity request = RequestEntity
                .get(createURI(url, params))
                .accept(MediaType.ALL)
                .build();

        return restTemplate.exchange(request, returnType).getBody();
    }

    public <T> T post(String url, Object body, boolean asJSON, Class<T> returnType) {
        body = body instanceof MultiValueMap ? ((MultiValueMap<?, ?>) body).toSingleValueMap() : body;

        RequestEntity request = RequestEntity
                .post(createURI(url, null))
                .contentType(asJSON ? MediaType.APPLICATION_JSON : MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .body(body);

        return restTemplate.exchange(request, returnType).getBody();
    }

    private URI createURI(String url, MultiValueMap<String, String> params) {
        String uri = UriComponentsBuilder.fromUriString(url.startsWith("http") ? url : apiRoot + url)
                .queryParams(params)
                .toUriString();

        return URI.create(uri);
    }
}
