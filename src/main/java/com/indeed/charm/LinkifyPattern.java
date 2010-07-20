package com.indeed.charm;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 */
public class LinkifyPattern {

    private final String name;
    private final Pattern pattern;
    private final String replacement;


    LinkifyPattern(String name, String pattern, String replacement) {
        this.name = name;
        this.pattern = Pattern.compile(pattern);
        this.replacement = replacement;
    }

    public String apply(String s) {
        Matcher m = pattern.matcher(s);
        return m.replaceAll(replacement);
    }

    public String getName() {
        return name;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getReplacement() {
        return replacement;
    }
}
