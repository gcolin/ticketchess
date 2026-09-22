package com.github.gcolin.membership;

import java.util.LinkedHashMap;

public record MembershipSummarySection(String sectionKey, LinkedHashMap<String, MembershipSummaryLine> lines) {

    public static final String LICENSES_KEY = "LICENSES";
}
