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

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

public class RichTextProcessor {

    private static ResourceReference BROKEN_IMAGE = new PackageResourceReference(RichTextProcessor.class, "broken-image-32.png");

    /**
     * @param tag the HTML tag to match
     * @return a pattern for matching the given HTML start tag.
     */
    private static Pattern startTagPattern(final String tag) {
        return Pattern.compile("<" + tag + "[^>]+>", Pattern.CASE_INSENSITIVE);
    }

    /**
     * @param tag the HTML tag to match
     * @return a pattern for matching tags with at least a 'data-uuid' attribute, e.g. <a href="link.html" data-uuid="1234">
     *         for 'a' tags, or <img src="image.png" data-uuid="5678"/> for 'img' tags.
     */
    private static Pattern startTagWithUuidPattern(final String tag) {
        return Pattern.compile("<" + tag + "[^>]+data-uuid=\"[^\"]+\"[^>]*>", Pattern.CASE_INSENSITIVE);
    }

    /**
     * @param tag the HTML attribute to match
     * @return a pattern for matching the given HTML attribute. The value of the attribute (between quotes) is the first
     * matching group.
     */
    private static Pattern attributePattern(final String attribute) {
        return Pattern.compile(attribute + "=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    }

    private static Pattern LINK_PATTERN = startTagPattern("a");
    private static Pattern IMG_PATTERN = startTagPattern("img");
    private static Pattern LINK_OR_IMG_PATTERN = Pattern.compile("<(a|img)[^>]+>", Pattern.CASE_INSENSITIVE);

    private static Pattern INTERNAL_LINK_PATTERN = startTagWithUuidPattern("a");
    private static Pattern INTERNAL_IMG_PATTERN = startTagWithUuidPattern("img");

    private static Pattern HREF_PATTERN = attributePattern("href");
    private static Pattern SRC_PATTERN = attributePattern("src");
    private static Pattern UUID_PATTERN = attributePattern("data-uuid");

    private static Pattern FACET_SELECT_PATTERN = Pattern.compile("data-facetselect=\"([^\"]+)\"\\s*", Pattern.CASE_INSENSITIVE);
    private static Pattern FACET_SELECT_OR_TYPE_OR_SRC_PATTERN = Pattern.compile("(data-facetselect|data-type|src)=\"([^\"]+)\"\\s*", Pattern.CASE_INSENSITIVE);

    private static Pattern EXTERNAL_LINK_HREF_PATTERN = Pattern.compile("^(mailto:|[a-z]+://.+?)", Pattern.CASE_INSENSITIVE);

    private static Pattern RESOURCE_DEFINITION_PATTERN = Pattern.compile("/\\{_document\\}/([^/]+)$", Pattern.CASE_INSENSITIVE);

    /**
     * Decorate the targets of relative image links in a text. The decorator may not be null.
     */
    public static String prefixInternalImageLinks(String text, IImageURLProvider decorator) {
        if (text == null) {
            return null;
        }

        StringBuffer processed = new StringBuffer();
        Matcher imageMatcher = IMG_PATTERN.matcher(text);

        while (imageMatcher.find()) {
            String img = imageMatcher.group();
            StringBuffer newImg = new StringBuffer();
            Matcher sourceMatcher = SRC_PATTERN.matcher(img);

            if (sourceMatcher.find()) {
                String src = sourceMatcher.group();
                String link = sourceMatcher.group(1);

                if (!isExternalLink(link)) {
                    String url;
                    try {
                        url = decorator.getURL(link);
                    } catch (RichTextException ex) {
                        url = RequestCycle.get().urlFor(BROKEN_IMAGE, null).toString();
                    }
                    appendEscapedReplacement(sourceMatcher, newImg, "src=\"" + url + "\"");
                    newImg.append(' ');
                    newImg.append("data-facetselect=\"");
                    newImg.append(link);
                    newImg.append("\"");
                    Matcher resourceDefinitionMatcher = RESOURCE_DEFINITION_PATTERN.matcher(link);
                    if (resourceDefinitionMatcher.find()) {
                        String type = resourceDefinitionMatcher.group(1);
                        newImg.append(" data-type=\"");
                        newImg.append(type);
                        newImg.append("\"");
                    }
                } else {
                    appendEscapedReplacement(sourceMatcher, newImg, src);
                }
            }

            sourceMatcher.appendTail(newImg);
            appendEscapedReplacement(imageMatcher, processed, trimTag(newImg.toString()));
        }

        imageMatcher.appendTail(processed);

        return processed.toString();
    }

    public static String restoreFacets(String text) {
        if (text == null) {
            return null;
        }

        StringBuffer processed = new StringBuffer();
        Matcher m = IMG_PATTERN.matcher(text);

        while (m.find()) {
            String img = m.group();
            StringBuffer newImg = new StringBuffer();
            Matcher s = FACET_SELECT_OR_TYPE_OR_SRC_PATTERN.matcher(img);

            String src = null;
            String facet = null;
            while (s.find()) {
                String facetOrTypeOrSrc = s.group();
                if (facetOrTypeOrSrc.startsWith("src")) {
                    src = facetOrTypeOrSrc;
                } else if (facetOrTypeOrSrc.startsWith("data-facetselect")) {
                    facet = facetOrTypeOrSrc;
                }
                s.appendReplacement(newImg, "");
            }
            if (src != null && facet != null) {
                Matcher fs = FACET_SELECT_PATTERN.matcher(facet);
                if (fs.find()) {
                    appendEscapedReplacement(fs, newImg, "src=\"" + fs.group(1) + "\" ");
                }
                fs.appendTail(newImg);
            } else if (src != null) {
                newImg.append(src);
            } else if (facet != null) {
                newImg.append(facet);
            }
            s.appendTail(newImg);
            appendEscapedReplacement(m, processed, trimTag(newImg.toString()));
        }

        m.appendTail(processed);
        return processed.toString();
    }

    /**
     * Return the UUIDs of internal links, i.e. the UUIDs of the linked documents / images / assets
     * in the text.  The text may not be null.
     */
    public static Set<String> getInternalLinkUuids(String text) {
        Set<String> uuids = new TreeSet<String>();
        if (text != null) {
            final Matcher m = LINK_OR_IMG_PATTERN.matcher(text);
            while (m.find()) {
                final String tag = m.group();
                final Matcher uuidMatcher = UUID_PATTERN.matcher(tag);
                if (uuidMatcher.find()) {
                    final Matcher hrefMatcher = HREF_PATTERN.matcher(tag);
                    boolean isExternalLink = false;
                    if (hrefMatcher.find()) {
                        final String href = hrefMatcher.group(1);
                        isExternalLink = isExternalLink(href);
                    }
                    if (!isExternalLink) {
                        final String uuid = uuidMatcher.group(1);
                        uuids.add(uuid);
                    }
                }
            }
        }
        return uuids;
    }

    public static String decorateLinkHrefs(final String text, final ILinkDecorator decorator) {
        if (text == null) {
            return null;
        }
        return decorateLinkAttributes(text, decorator, HREF_PATTERN);
    }

    private static String decorateLinkAttributes(final String text, final ILinkDecorator decorator, final Pattern attributePattern) {
        return decorateTagAttributes(text, decorator, LINK_PATTERN, attributePattern);
    }

    public static String decorateInternalLinkHrefs(final String text, final ILinkDecorator decorator) {
        if (text == null) {
            return null;
        }
        return decorateInternalLinkAttributes(text, decorator, HREF_PATTERN);
    }

    public static String decorateInternalLinkUuids(final String text, final ILinkDecorator decorator) {
        return decorateInternalLinkAttributes(text, decorator, UUID_PATTERN);
    }

    private static String decorateInternalLinkAttributes(final String text, final ILinkDecorator decorator, final Pattern attributePattern) {
        return decorateTagAttributes(text, decorator, INTERNAL_LINK_PATTERN, attributePattern);
    }

    public static String decorateImgSrcs(final String text, final ILinkDecorator decorator) {
        return decorateTagAttributes(text, decorator, IMG_PATTERN, SRC_PATTERN);
    }

    public static String decorateInternalImgUuids(final String text, final ILinkDecorator decorator) {
        return decorateTagAttributes(text, decorator, INTERNAL_IMG_PATTERN, UUID_PATTERN);
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

    private static boolean isExternalLink(String href) {
        return EXTERNAL_LINK_HREF_PATTERN.matcher(href).find();
    }

}
