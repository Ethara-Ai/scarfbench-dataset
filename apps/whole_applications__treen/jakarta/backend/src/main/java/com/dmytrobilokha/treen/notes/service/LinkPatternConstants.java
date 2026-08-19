/*
This file has been taken from the Android code at
https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/util/Patterns.java
It has been modified, all the non-relevant patterns have been removed
 */

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dmytrobilokha.treen.notes.service;

import java.util.regex.Pattern;

@SuppressWarnings({"checkstyle:AvoidEscapedUnicodeCharacters", "checkstyle:DeclarationOrder"})
final class LinkPatternConstants {

    static final Pattern PROCESSED_GEO_LINK = Pattern.compile("geo:(\\+|-)?\\d+\\.\\d+,(\\+|-)?\\d+\\.\\d+");
    static final int GEO_PREFIX_LENGTH = "geo:".length();
    private static final String PROTOCOL = "(?i:http|https|rtsp|ftp)://";

    static final Pattern STARTS_WITH_PROTOCOL = Pattern.compile(PROTOCOL + ".*");

    private static final String USER_INFO = "(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
            + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
            + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@";

    private static final String UCS_CHAR = "["
            + "\u00A0-\uD7FF"
            + "\uF900-\uFDCF"
            + "\uFDF0-\uFFEF"
            + "\uD800\uDC00-\uD83F\uDFFD"
            + "\uD840\uDC00-\uD87F\uDFFD"
            + "\uD880\uDC00-\uD8BF\uDFFD"
            + "\uD8C0\uDC00-\uD8FF\uDFFD"
            + "\uD900\uDC00-\uD93F\uDFFD"
            + "\uD940\uDC00-\uD97F\uDFFD"
            + "\uD980\uDC00-\uD9BF\uDFFD"
            + "\uD9C0\uDC00-\uD9FF\uDFFD"
            + "\uDA00\uDC00-\uDA3F\uDFFD"
            + "\uDA40\uDC00-\uDA7F\uDFFD"
            + "\uDA80\uDC00-\uDABF\uDFFD"
            + "\uDAC0\uDC00-\uDAFF\uDFFD"
            + "\uDB00\uDC00-\uDB3F\uDFFD"
            + "\uDB44\uDC00-\uDB7F\uDFFD"
            + "&&[^\u00A0[\u2000-\u200A]\u2028\u2029\u202F\u3000]]";

    private static final String LABEL_CHAR = "a-zA-Z0-9" + UCS_CHAR;

    private static final String PORT_NUMBER = "\\:\\d{1,5}";

    private static final String PATH_AND_QUERY = "[/\\?](?:(?:[" + LABEL_CHAR
            + ";/\\?:@&=#~"  // plus optional query params
            + "\\-\\.\\+!\\*'\\(\\),_\\$])|(?:%[a-fA-F0-9]{2}))*";

    private static final String WORD_BOUNDARY = "(?:\\b|$|^)";

    private static final String IRI_LABEL =
            "[" + LABEL_CHAR + "](?:[" + LABEL_CHAR + "_\\-]{0,61}[" + LABEL_CHAR + "]){0,1}";

    private static final String PUNYCODE_TLD = "xn\\-\\-[\\w\\-]{0,58}\\w";

    private static final String TLD_CHAR = "a-zA-Z" + UCS_CHAR;

    private static final String TLD = "(" + PUNYCODE_TLD + "|" + "[" + TLD_CHAR + "]{2,63}" + ")";

    private static final String HOST_NAME = "(" + IRI_LABEL + "\\.)+" + TLD;

    private static final String IP_ADDRESS_STRING =
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))";

    private static final String DOMAIN_NAME_STR = "(" + HOST_NAME + "|" + IP_ADDRESS_STRING + ")";

    static final Pattern WEB_URL = Pattern.compile("("
            + "("
            + "(?:" + PROTOCOL + "(?:" + USER_INFO + ")?" + ")?"
            + "(?:" + DOMAIN_NAME_STR + ")"
            + "(?:" + PORT_NUMBER + ")?"
            + ")"
            + "(" + PATH_AND_QUERY + ")?"
            + WORD_BOUNDARY
            + ")");

    private LinkPatternConstants() {
        //Constants class
    }

}
