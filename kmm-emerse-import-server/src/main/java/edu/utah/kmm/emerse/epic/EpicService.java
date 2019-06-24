package edu.utah.kmm.emerse.epic;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public class EpicService {

    private final String apiRoot;

    private final RestTemplate restTemplate;

    public EpicService(IGenericClient client, Credentials credentials) {
        restTemplate = buildRestTemplate();
        BasicAuthenticationInterceptor interceptor = new BasicAuthenticationInterceptor(credentials.getUsername(), credentials.getPassword());
        restTemplate.getInterceptors().add(interceptor);
        String fhirRoot = client.getServerBase();
        apiRoot = StringUtils.substringBeforeLast(fhirRoot, "/api/") + "/api/";
    }

    private RestTemplate buildRestTemplate() {
        CloseableHttpClient httpClient = HttpClientBuilder
                .create()
                .setRedirectStrategy(new RedirectStrategy() {

                    @Override
                    public boolean isRedirected(HttpRequest request, HttpResponse response,
                                                HttpContext context) {
                        return false;
                    }

                    @Override
                    public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
                        return null;
                    }
                })
                .build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }

    public <T> ResponseEntity<T> getResponse(String url, MultiValueMap<String, String> params, Class<T> returnType) {
        RequestEntity request = addHeaders(RequestEntity
                .get(createURI(url, params)))
                .build();

        return restTemplate.exchange(request, returnType);
    }

    public <T> T get(String url, MultiValueMap<String, String> params, Class<T> returnType) {
        return getResponse(url, params, returnType).getBody();
    }

    public <T> ResponseEntity<T> postResponse(String url, Object body, boolean asJSON, Class<T> returnType) {
        body = asJSON && body instanceof MultiValueMap ? ((MultiValueMap<?, ?>) body).toSingleValueMap() : body;

        RequestEntity request = addHeaders(RequestEntity
                .post(createURI(url, null)))
                .contentType(asJSON ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_FORM_URLENCODED)
                .body(body);

        return restTemplate.exchange(request, returnType);
    }

    private <T extends RequestEntity.HeadersBuilder<T>> T addHeaders(RequestEntity.HeadersBuilder<T> builder) {
        return builder.accept(MediaType.APPLICATION_JSON)
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .header("Accept-Encoding")
                .acceptCharset();
    }

    public <T> T post(String url, Object body, boolean asJSON, Class<T> returnType) {
        return postResponse(url, body, asJSON, returnType).getBody();
    }

    private URI createURI(String url, MultiValueMap<String, String> params) {
        String uri = UriComponentsBuilder.fromUriString(url.startsWith("http") ? url : apiRoot + url)
                .queryParams(params)
                .toUriString();

        return URI.create(uri);
    }
}
