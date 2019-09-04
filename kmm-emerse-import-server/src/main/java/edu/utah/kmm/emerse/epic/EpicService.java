package edu.utah.kmm.emerse.epic;

import edu.utah.kmm.emerse.security.Credentials;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.client_id}")
    private String clientId;

    @Value("${epic.api.root}/")
    private String apiRoot;

    @Autowired
    private Credentials fhirServiceCredentials;

    private RestTemplate restTemplateBasic;

    private RestTemplate restTemplateNone;

    public void init() {
        restTemplateNone = buildRestTemplate();
        restTemplateBasic = buildRestTemplate();
        BasicAuthenticationInterceptor interceptor = new BasicAuthenticationInterceptor(fhirServiceCredentials.getUsername(), fhirServiceCredentials.getPassword());
        restTemplateBasic.getInterceptors().add(interceptor);
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

    public <T> ResponseEntity<T> getResponse(String url, MultiValueMap<String, String> params, Class<T> returnType, boolean authenticate) {
        RequestEntity request = addHeaders(RequestEntity
                .get(createURI(url, params)))
                .build();

        return getRestTemplate(authenticate).exchange(request, returnType);
    }

    public <T> T get(String url, MultiValueMap<String, String> params, Class<T> returnType, boolean authenticate) {
        return getResponse(url, params, returnType, authenticate).getBody();
    }

    public <T> ResponseEntity<T> postResponse(String url, Object body, boolean asJSON, Class<T> returnType, boolean authenticate) {
        body = body instanceof MultiValueMap ? ((MultiValueMap<?, ?>) body).toSingleValueMap() : body;

        RequestEntity request = addHeaders(RequestEntity
                .post(createURI(url, null)))
                .contentType(asJSON ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_FORM_URLENCODED)
                .body(body);

        return getRestTemplate(authenticate).exchange(request, returnType);
    }

    public <T> T post(String url, Object body, boolean asJSON, Class<T> returnType, boolean authenticate) {
        return postResponse(url, body, asJSON, returnType, authenticate).getBody();
    }

    private RestTemplate getRestTemplate(boolean authenticate) {
        return authenticate ? restTemplateBasic : restTemplateNone;
    }

    private <T extends RequestEntity.HeadersBuilder<T>> T addHeaders(RequestEntity.HeadersBuilder<T> builder) {
        return builder.accept(MediaType.APPLICATION_JSON)
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .header("Epic-Client-ID", clientId);
    }

    private URI createURI(String url, MultiValueMap<String, String> params) {
        String uri = UriComponentsBuilder.fromUriString(url.startsWith("http") ? url : apiRoot + url)
                .queryParams(params)
                .toUriString();

        return URI.create(uri);
    }
}
