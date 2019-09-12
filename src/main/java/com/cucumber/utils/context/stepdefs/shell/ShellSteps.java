package com.cucumber.utils.context.stepdefs.shell;

import com.cucumber.utils.clients.shell.ShellClient;
import com.cucumber.utils.context.utils.Cucumbers;
import com.cucumber.utils.context.utils.ScenarioLogger;
import com.google.inject.Inject;
import cucumber.runtime.java.guice.ScenarioScoped;
import io.cucumber.java.en.Then;

@ScenarioScoped
public class ShellSteps {

    @Inject
    private Cucumbers cucumbers;
    @Inject
    private ScenarioLogger logger;
    private ShellClient shellClient = new ShellClient();

    @Then("SHELL execute command \"{}\" and check response=\"{}\"")
    public void executeAndCompare(String cmd, String expected) {
        logger.log("Execute cmd '%s' and compare response with: %s", cmd, expected);
        String actual = shellClient.command("bash", "-c", cmd).trim();
        cucumbers.compare(expected, actual);
    }
}
