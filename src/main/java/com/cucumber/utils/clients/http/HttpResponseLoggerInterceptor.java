package com.cucumber.utils.clients.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class HttpResponseLoggerInterceptor implements HttpResponseInterceptor {

    private Logger log = LogManager.getLogger();

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        log.debug("--- HTTP RESPONSE ---");
        log.debug("Response STATUS: {}", response.getStatusLine());
        log.debug("Response HEADERS: {}", Arrays.asList(response.getAllHeaders()));
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
                return "Cannot consume HTTP response: " + e.getMessage();
            } finally {
                try {
                    if (response != null && content != null) {
                        response.setEntity(new StringEntity(content));
                    }
                } catch (UnsupportedEncodingException e) {
                    log.error(e);
                }
            }
        });
        log.debug("---------------------");
    }
}