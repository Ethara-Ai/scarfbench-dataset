package com.dmytrobilokha.treen.notes.service;

import java.util.regex.Pattern;

public enum LinkType {

    //Order matters, it determines priority of the detection method
    GEO("(geo:)?\\h*(\\+|-)?\\d+\\.\\d+\\h*,\\h*(\\+|-)?\\d+\\.\\d+") {
        @Override
        public String format(String link) {
            var linkText = link.strip();
            var schemedLink = linkText.startsWith("geo:") ? linkText : "geo:" + linkText;
            return schemedLink.replaceAll("\\h", "");
        }
    },

    WEB(LinkPatternConstants.WEB_URL) {
        @Override
        public String format(String link) {
            var linkText = link.strip();
            if (LinkPatternConstants.STARTS_WITH_PROTOCOL.matcher(linkText).matches()) {
                return linkText;
            }
            return "https://" + linkText;
        }
    },

    UNKNOWN(".*") {
        @Override
        public String format(String link) {
            return link;
        }
    },
    ;

    private final Pattern pattern;

    LinkType(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    LinkType(Pattern pattern) {
        this.pattern = pattern;
    }

    public static LinkType detect(String link) {
        var linkText = link.strip();
        for (var type : LinkType.values()) {
            if (type.pattern.matcher(linkText).matches()) {
                return type;
            }
        }
        return UNKNOWN;
    }

    public abstract String format(String link);

}
