package com.cucumber.utils.engineering.placeholders;


import com.cucumber.utils.engineering.utils.RegexUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScenarioPropertiesGenerator {

    public static final String SYMBOL_START = "~\\[";
    public static final String SYMBOL_END = "\\]";
    private static final String SYMBOL_REGEX =
            SYMBOL_START + "(.*?)" + SYMBOL_END;

    private static final Pattern SYMBOL_PATTERN = Pattern.compile(SYMBOL_REGEX,
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

    private String target;
    private String source;
    private Map<String, String> properties = new LinkedHashMap<>();

    public ScenarioPropertiesGenerator(String target, String source) {
        this.target = target;
        this.source = source;
        generateProperties();
    }

    public boolean targetIsStandaloneProperty() {
        return target.startsWith(SYMBOL_START.replaceFirst("\\\\", ""))
                && target.endsWith(SYMBOL_END.replaceFirst("\\\\", ""));
    }

    public String getParsedTarget() {
        return getParsedTarget(false);
    }

    public String getParsedTarget(boolean usePatternQuotes) {
        String str = target;
        for (Map.Entry<String, String> e : properties.entrySet()) {
            str = str.replaceAll(SYMBOL_START + e.getKey() + SYMBOL_END,
                    Matcher.quoteReplacement(usePatternQuotes ? Pattern.quote(e.getValue()) : e.getValue()));
        }
        return str;
    }

    public Map<String, String> getProperties() {
        return this.properties;
    }

    private void generateProperties() {
        List<String> propertyNames = getPropertyKeys();
        if (propertyNames.isEmpty()) {
            return;
        }
        boolean isRegex = RegexUtils.isRegex(target);
        String str = !isRegex ? "\\Q" + target + "\\E" : target;
        for (String name : propertyNames) {
            str = str.replaceAll(SYMBOL_START + name + SYMBOL_END,
                    !isRegex ? "\\\\E(.*)\\\\Q" : "(.*)");
        }
        Pattern pattern =
                Pattern.compile(str, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                properties.put(propertyNames.get(i - 1), matcher.group(i));
            }
        }
    }

    private List<String> getPropertyKeys() {
        List<String> names = new ArrayList<>();
        Matcher matcher = SYMBOL_PATTERN.matcher(target);
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                names.add(matcher.group(i));
            }
        }
        return names;
    }
}
