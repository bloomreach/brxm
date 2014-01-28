/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.components.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.core.component.HstRequest;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public final class SiteUtils {


    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;


    /**
     * Gets parameter in following order: namespaced parameter,  public request parameter, component parameter
     *
     * @param parameterName name of the parameter
     * @param request       instance of HstRequest
     * @param component     instance of BaseHstComponent
     * @return parameter value or null if nothing is set
     */
    @Nullable
    public static String getAnyParameter(final String parameterName, final HstRequest request, final BaseHstComponent component) {

        String value = request.getParameter(parameterName);
        if (!Strings.isNullOrEmpty(value)) {
            return value;
        }
        value = component.getPublicRequestParameter(request, parameterName);
        if (!Strings.isNullOrEmpty(value)) {
            return value;
        }
        return component.getComponentParameter(parameterName);
    }


    private static final String DEFAULT_APPEND_TEXT = " ...";

    /**
     * Abbreviate a text to _approximately_ some number of characters, trying to
     * find a nice word end and then appending some string (defaulting to three dots).
     */
    public static String abbreviateText(final String text, final int numberOfCharacters, final String appendText) {

        int nrChars = numberOfCharacters;

        if (nrChars > 0 && text != null && text.length() > nrChars) {

            // search a nice word end, _backwards_ from nrChars

            int spaceIndex = text.lastIndexOf(' ', nrChars);
            int dotIndex = text.lastIndexOf('.', nrChars);
            int commaIndex = text.lastIndexOf(',', nrChars);
            int questionIndex = text.lastIndexOf('?', nrChars);
            int exclamationIndex = text.lastIndexOf('!', nrChars);

            // set index to last space
            nrChars = spaceIndex;

            if (dotIndex > nrChars) {
                nrChars = dotIndex;
            }
            if (commaIndex > nrChars) {
                nrChars = commaIndex;
            }
            if (questionIndex > nrChars) {
                nrChars = questionIndex;
            }
            if (exclamationIndex > nrChars) {
                nrChars = exclamationIndex;
            }
            // final check for < 0
            if (nrChars < 0) {
                nrChars = 0;
            }

            if (appendText != null) {
                return text.substring(0, nrChars) + appendText;
            } else {
                return text.substring(0, nrChars) + DEFAULT_APPEND_TEXT;
            }
        }
        return text;
    }

    /**
     * Abbreviate a text to _approximately_ some number of characters, trying to
     * find a nice word end and then appending three dots.
     */
    public static String abbreviateText(final String text, final int numberOfCharacters) {
        return abbreviateText(text, numberOfCharacters, null);
    }

    /**
     * Concatenates a collection of strings by concatenating the strings and inserting a separator in between
     * each of them. Nulls are handled automatically and there is no separator at the end of sequence.
     *
     * @param strings   collection of strings (collection may contain null objects, those are ignored)
     * @param separator the separator
     * @return concatenated string
     */
    public static String concat(Iterable<String> strings, String separator) {
        StringBuilder builder = new StringBuilder();
        Joiner.on(separator).skipNulls().appendTo(builder, strings);
        return builder.toString();
    }

    /**
     * Concatenates an array of strings by concatenating the strings and inserting a separator in between
     * each of them. Nulls are handled automatically and there is no separator at the end of sequence.
     *
     * @param strings   the strings
     * @param separator the separator
     * @return concatenated string
     */
    public static String concat(String[] strings, String separator) {
        StringBuilder builder = new StringBuilder();
        Joiner.on(separator).skipNulls().appendTo(builder, strings);
        return builder.toString();
    }

    /**
     * Remove empty (length zero when trimmed) values from a list.
     */
    public static List<String> removeEmptyValues(final Collection<String> values) {

        if (values == null) {
            return null;
        }

        final List<String> result = new ArrayList<>(values.size());
        for (String value : values) {
            if (!(value.trim().length() == 0)) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * Remove empty (length zero when trimmed) values from an array.
     */
    public static String[] removeEmptyValues(final String[] values) {

        if (values == null) {
            return null;
        }

        final List<String> result = removeEmptyValues(Arrays.asList(values));
        return result.toArray(new String[result.size()]);
    }

    /**
     * Replaces <strong>{@code ${variableName}}</strong> variable in a template with the replacement value provided.
     *
     * @param variableName     variable name
     * @param replacementValue replacement value
     * @param template         string which contains variable e.g
     *                         <p /><strong>{@code My name is ${username} and my login is ${login}}</strong>
     * @return string (template with string replacements)
     */
    public static String replacePlaceHolders(final String variableName, final String replacementValue, final CharSequence template) {
        Pattern pattern = Pattern.compile("(\\$\\{" + variableName + "*\\})");
        Matcher matcher = pattern.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, replacementValue);
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }


    private SiteUtils() {
    }
}
