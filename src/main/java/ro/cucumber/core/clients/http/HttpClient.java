package ro.cucumber.core.clients.http;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpClient {
    private Logger log = LogManager.getLogger();
    private Integer timeout;
    private HttpHost proxyHost;
    private String address;
    private URIBuilder uriBuilder;
    private Map<String, String> headers;
    private String entity;
    private Method method;

    private CloseableHttpClient client;
    private HttpRequestBase request;

    private HttpClient(Builder builder) {
        this.proxyHost = builder.proxyHost;
        this.timeout = builder.timeout;
        this.address = builder.address;
        this.uriBuilder = builder.uriBuilder;
        this.headers = builder.headers;
        this.entity = builder.entity;
        this.method = builder.method;

        validateMethod();
        validateAddress();

        this.client = getClient();
        this.request = getRequest();
    }

    public HttpResponse execute() {
        try {
            logRequest();
            HttpResponse response = client.execute(request);
            logResponse(response);
            return response;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CloseableHttpClient getClient() {
        RequestConfig.Builder configBuilder = RequestConfig.custom();
        if (timeout != null) {
            configBuilder.setConnectTimeout(timeout).setConnectionRequestTimeout(timeout)
                    .setSocketTimeout(timeout);
        }
        if (proxyHost != null) {
            configBuilder.setProxy(proxyHost);
        }
        HostnameVerifier allowAllHosts = new NoopHostnameVerifier();
        return HttpClients.custom()
                .setSSLSocketFactory(new SSLConnectionSocketFactory(getSslContext(), allowAllHosts))
                .setDefaultRequestConfig(configBuilder.build()).build();
    }

    private SSLContext getSslContext() {
        SSLContext ctx;
        try {
            ctx = SSLContext.getInstance("TLS");
            ctx.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()},
                    new SecureRandom());
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return ctx;
    }

    private HttpRequestBase getRequest() {

        HttpRequestBase request;
        String url;

        try {
            url = address + "/" + uriBuilder.build().toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e.getMessage());
        }
        switch (method) {
            case GET:
                request = new HttpGet(url);
                break;
            case POST:
                HttpPost post = new HttpPost(url);
                try {
                    post.setEntity(new StringEntity(entity != null ? entity : ""));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                request = post;
                break;
            case PUT:
                HttpPut put = new HttpPut(url);
                try {
                    put.setEntity(new StringEntity(entity != null ? entity : ""));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                request = put;
                break;
            case DELETE:
                request = new HttpDelete(url);
                break;
            case OPTIONS:
                request = new HttpOptions(url);
                break;
            case TRACE:
                request = new HttpTrace(url);
                break;
            case HEAD:
                request = new HttpHead(url);
                break;
            default:
                throw new IllegalStateException("Invalid HTTP method");
        }
        setHeaders(request);
        return request;
    }

    private void setHeaders(HttpRequestBase request) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request.setHeader(entry.getKey(), entry.getValue());
        }
    }

    private void validateMethod() {
        if (method == null) {
            throw new IllegalStateException("HTTP Method missing");
        }
    }

    private void validateAddress() {
        if (address == null) {
            throw new IllegalStateException("HTTP Address missing");
        }
    }

    private void logRequest() {
        log.debug("---- HTTP REQUEST ----");
        try {
            log.debug("{}: {}{}", method, address, uriBuilder.build().toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        log.debug("Request HEADERS: {}", headers);
        if (proxyHost != null) {
            log.debug("PROXY host: {}", proxyHost);
        }
        if (entity != null) {
            log.debug("Request BODY:{}{}", System.lineSeparator(), entity);
        }
    }

    private void logResponse(HttpResponse response) {
        log.debug("---- HTTP RESPONSE ----");
        log.debug("Response STATUS: {}", response.getStatusLine());
        log.debug("Response HEADERS: {}", response.getAllHeaders());
        log.debug("Response BODY:{}{}", () -> System.lineSeparator(), () -> {
            HttpEntity entity = response.getEntity();
            if (response == null) {
                return null;
            }
            String content = null;
            try {
                content = EntityUtils.toString(entity);
                return content;
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    response.setEntity(new StringEntity(content));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static class Builder {
        private Integer timeout;
        private HttpHost proxyHost;
        private String address;
        private URIBuilder uriBuilder = new URIBuilder();
        private Map<String, String> headers = new HashMap<>();
        private String entity;
        private Method method;

        public Builder useProxy(String proxyHost, int proxyPort, String proxyScheme) {
            this.proxyHost = new HttpHost(proxyHost, proxyPort, proxyScheme);
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder address(String address) {
            this.address = address.replaceFirst("/*$", "");
            return this;
        }

        public Builder path(String path) {
            this.uriBuilder.setPath(path.replaceFirst("^/*", ""));
            return this;
        }

        public Builder addHeader(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder setHeader(String name, String value) {
            this.headers.clear();
            this.headers.put(name, value);
            return this;
        }

        public Builder setHeaders(Map<String, String> headers) {
            this.headers.clear();
            this.headers.putAll(headers);
            return this;
        }

        public Builder addQueryParam(String name, String value) {
            this.uriBuilder.addParameter(name, value);
            return this;
        }

        public Builder setQueryParam(String name, String value) {
            this.uriBuilder.setParameter(name, value);
            return this;
        }

        public Builder setQueryParams(Map<String, String> queryParams) {
            List<NameValuePair> paramsList = new ArrayList();
            queryParams.forEach((k, v) -> paramsList.add(new BasicNameValuePair(k, v)));
            this.uriBuilder.setParameters(paramsList);
            return this;
        }

        public Builder entity(String entity) {
            this.entity = entity;
            return this;
        }

        public Builder method(Method method) {
            this.method = method;
            return this;
        }

        public HttpClient build() {
            return new HttpClient(this);
        }
    }

    @Override
    public String toString() {
        return "HttpClient{" + "timeout=" + timeout + ", proxyHost=" + proxyHost + ", address='"
                + address + '\'' + ", uriBuilder=" + uriBuilder + ", headers=" + headers
                + ", entity='" + entity + '\'' + ", method=" + method + '}';
    }
}


class DefaultTrustManager implements X509TrustManager {

    @Override
    public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] arg0, String arg1) {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
