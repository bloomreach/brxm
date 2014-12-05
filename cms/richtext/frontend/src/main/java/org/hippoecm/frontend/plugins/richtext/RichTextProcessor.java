/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.richtext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

public class RichTextProcessor {

    public static ResourceReference BROKEN_IMAGE = new PackageResourceReference(RichTextProcessor.class, "broken-image-32.png");

    public static String INTERNAL_LINK_DEFAULT_HREF = "http://";

    private static Pattern EXTERNAL_LINK_HREF_PATTERN = Pattern.compile("^(#|/|[a-z][a-z0-9+-.]*:)", Pattern.CASE_INSENSITIVE);

    /**
     * @param tag the HTML tag to match
     * @return a pattern for matching the given HTML start tag.
     */
    private static Pattern startTagPattern(final String tag) {
        return Pattern.compile("<" + tag + "[^>]+>", Pattern.CASE_INSENSITIVE);
    }

    /**
     * @param attribute the HTML attribute to match
     * @return a pattern for matching the given HTML attribute. The value of the attribute (between quotes) is the first
     * matching group.
     */
    private static Pattern attributePattern(final String attribute) {
        return Pattern.compile(attribute + "=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    }

    private static Pattern LINK_PATTERN = startTagPattern("a");

    private static Pattern HREF_PATTERN = attributePattern("href");

    public static String decorateLinkHrefs(final String text, final ILinkDecorator decorator) {
        if (text == null) {
            return null;
        }
        return decorateLinkAttributes(text, decorator, HREF_PATTERN);
    }

    private static String decorateLinkAttributes(final String text, final ILinkDecorator decorator, final Pattern attributePattern) {
        return decorateTagAttributes(text, decorator, LINK_PATTERN, attributePattern);
    }

    private static String decorateTagAttributes(final String text, final ILinkDecorator decorator, final Pattern tagPattern, final Pattern attributePattern) {
        if (text == null) {
            return null;
        }

        final StringBuffer processed = new StringBuffer();
        final Matcher m = tagPattern.matcher(text);
        while (m.find()) {
            final String tag = m.group();
            final StringBuffer newTag = new StringBuffer();
            final Matcher s = attributePattern.matcher(tag);
            if (s.find()) {
                final String attributeValue = s.group(1);
                final String decorated = isExternalLink(attributeValue) ? decorator.externalLink(attributeValue) : decorator.internalLink(attributeValue);
                appendEscapedReplacement(s, newTag, decorated);
            }
            s.appendTail(newTag);
            appendEscapedReplacement(m, processed, trimTag(newTag.toString()));
        }
        m.appendTail(processed);

        return processed.toString();
    }

    private static void appendEscapedReplacement(Matcher m, StringBuffer sb, String replacement) {
        final String escaped = replacement.replace("\\", "\\\\").replace("$", "\\$");
        m.appendReplacement(sb, escaped);
    }

    private static String trimTag(final String text) {
        return text.replaceAll("(<[a-z]+\\s)\\s*", "$1").replaceAll("\\s*>", ">").replaceFirst("\\s*/>$", "/>");
    }

    /**
     * @return true if the href value is an external link. The default internal link href value should not be handled
     * as external link.
     */
    public static boolean isExternalLink(String href) {
        return !INTERNAL_LINK_DEFAULT_HREF.equals(href) && EXTERNAL_LINK_HREF_PATTERN.matcher(href).find();
    }

}
