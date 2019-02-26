package com.cucumber.utils.basicstepdefs.http;

import com.cucumber.utils.clients.http.HttpClient;
import com.cucumber.utils.clients.http.Method;
import com.cucumber.utils.context.compare.Cucumbers;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.java.guice.ScenarioScoped;
import io.cucumber.datatable.DataTable;
import org.apache.http.HttpResponse;

import java.util.List;
import java.util.Map;

@ScenarioScoped
public class HttpClientSteps {
    private HttpClient.Builder builder = new HttpClient.Builder();
    private HttpResponse response;

    @Given("HTTP REST service at address \"{cstring}\"")
    public void setAddress(String address) {
        builder.address(address);
    }

    @And("HTTP path \"{cstring}\"")
    public void setPath(String path) {
        builder.path(path);
    }

    @And("^HTTP headers$")
    public void setHeaders(DataTable table) {
        List<Map<String, String>> list = table.asMaps();
        if (!list.isEmpty()) {
            for (Map.Entry<String, String> e : list.get(list.size() - 1).entrySet()) {
                builder.addHeader(e.getKey(), e.getValue());
            }
        }
    }

    @And("^HTTP query params$")
    public void setQueryParams(DataTable table) {
        List<Map<String, String>> list = table.asMaps();
        if (!list.isEmpty()) {
            for (Map.Entry<String, String> e : list.get(list.size() - 1).entrySet()) {
                builder.addQueryParam(e.getKey(), e.getValue());
            }
        }
    }

    @And("HTTP method {Method}")
    public void setMethod(Method method) {
        builder.method(method);
    }

    @And("HTTP entity \"{cstring}\"")
    public void setEntity(String entity) {
        builder.entity(entity);
    }

    @And("HTTP proxy host \"{cstring}\" port \"{cstring}\" and scheme \"{cstring}\"")
    public void useProxy(String host, int port, String scheme) {
        builder.useProxy(host, port, scheme);
    }

    @And("HTTP timeout \"{cstring}\"")
    public void setTimeout(int timeout) {
        builder.timeout(timeout);
    }

    @When("^HTTP execute$")
    public void execute() {
        this.response = builder.build().execute();
    }

    @Then("^HTTP compare response body with$")
    public void compareResponseBodyWith(String expected) {
        Cucumbers.compare(expected, response);
    }

    @And("HTTP compare response status code with \"{cstring}\"")
    public void compareResponseStatusCodeWith(int expected) {
    }
}