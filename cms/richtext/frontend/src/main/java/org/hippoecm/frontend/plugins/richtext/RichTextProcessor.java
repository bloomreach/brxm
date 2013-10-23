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

import java.nio.charset.Charset;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.encoding.UrlDecoder;

public class RichTextProcessor {

    private static Pattern LINK_AND_IMAGES_PATTERN = Pattern.compile("<(a|img)(\\s+)(.*?)(uuid)=\"(.*?)\"(.*?)>",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    private static Pattern IMG_PATTERN = Pattern.compile("<img[^>]+>", Pattern.CASE_INSENSITIVE);
    private static Pattern SRC_PATTERN = Pattern.compile("src=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static Pattern FACET_SELECT_PATTERN = Pattern.compile("facetselect=\"([^\"]+)\"\\s*", Pattern.CASE_INSENSITIVE);
    private static Pattern FACET_SELECT_OR_TYPE_OR_SRC_PATTERN = Pattern.compile("(facetselect|type|src)=\"([^\"]+)\"\\s*",
            Pattern.CASE_INSENSITIVE);

    private static Pattern LINK_PATTERN = Pattern.compile("<a[^>]+>", Pattern.CASE_INSENSITIVE);
    private static Pattern EXTERNAL_LINK_PATTERN = Pattern.compile("^[a-z]+:/", Pattern.CASE_INSENSITIVE);
    private static Pattern HREF_PATTERN = Pattern.compile("href=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static Pattern UUID_PATTERN = Pattern.compile("uuid=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static Pattern RESOURCE_DEFINITION_PATTERN = Pattern.compile("/\\{_document\\}/([^/]+)$", Pattern.CASE_INSENSITIVE);

    private static ResourceReference BROKEN_IMAGE = new PackageResourceReference(RichTextProcessor.class, "broken-image-32.png");

    /**
     * Decorate the targets of relative image links in a text.  Text and decorator may
     * neither be null.
     */
    public static String prefixInternalImageLinks(String text, IImageURLProvider decorator) {

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
                    newImg.append("facetselect=\"");
                    newImg.append(link);
                    newImg.append("\"");
                    Matcher resourceDefinitionMatcher = RESOURCE_DEFINITION_PATTERN.matcher(link);
                    if (resourceDefinitionMatcher.find()) {
                        String type = resourceDefinitionMatcher.group(1);
                        newImg.append(" type=\"");
                        newImg.append(type);
                        newImg.append("\"");
                    }
                } else {
                    appendEscapedReplacement(sourceMatcher, newImg, src);
                }
            }

            sourceMatcher.appendTail(newImg);
            appendEscapedReplacement(imageMatcher, processed, removeSpacesBeforeEndTag(newImg.toString()));
        }

        imageMatcher.appendTail(processed);

        return processed.toString();
    }

    public static String restoreFacets(String text) {
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
                } else if (facetOrTypeOrSrc.startsWith("facetselect")) {
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
            appendEscapedReplacement(m, processed, removeSpacesBeforeEndTag(newImg.toString()));
        }

        m.appendTail(processed);
        return processed.toString();
    }

    /**
     * Return the UUIDs of internal links, i.e. the UUIDs of the linked documents / images / assets
     * in the text.  The text may not be null.
     */
    public static Set<String> getInternalLinkUuids(String text) {
        Set<String> links = new TreeSet<String>();
        Matcher m = LINK_AND_IMAGES_PATTERN.matcher(text);
        while (m.find()) {
            String link = m.group(5);
            if (isExternalLink(link)) {
                continue;
            }
            String linkName;
            if (link.indexOf('/') > 0) {
                linkName = link.substring(0, link.indexOf('/'));
            } else {
                linkName = link;
            }
            final Charset charset = getCharset();
            linkName = UrlDecoder.PATH_INSTANCE.decode(linkName, charset);
            links.add(linkName);
        }
        return links;
    }

    private static Charset getCharset() {
        final RequestCycle requestCycle = RequestCycle.get();
        if (requestCycle != null) {
            return requestCycle.getRequest().getCharset();
        }
        return Charset.forName("UTF-8");
    }

    public static String decorateLinkHrefs(final String text, final ILinkDecorator decorator) {
        String decorated = decorateLinkAttributes(text, decorator, HREF_PATTERN);
        return decorated.replaceAll("<a target=\"_blank\"", "<a ");
    }

    public static String decorateLinkUuids(final String text, final ILinkDecorator decorator) {
        return decorateLinkAttributes(text, decorator, UUID_PATTERN);
    }

    private static String decorateLinkAttributes(final String text, final ILinkDecorator decorator, final Pattern attributePattern) {
        return decorateTagAttributes(text, decorator, LINK_PATTERN, attributePattern);
    }

    public static String decorateImgSrcs(final String text, final ILinkDecorator decorator) {
        return decorateTagAttributes(text, decorator, IMG_PATTERN, SRC_PATTERN);
    }

    public static String decorateImgUuids(final String text, final ILinkDecorator decorator) {
        return decorateTagAttributes(text, decorator, IMG_PATTERN, UUID_PATTERN);
    }

    private static String decorateTagAttributes(final String text, final ILinkDecorator decorator, final Pattern tagPattern, final Pattern attributePattern) {
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
            appendEscapedReplacement(m, processed, removeSpacesBeforeEndTag(newTag.toString()));
        }
        m.appendTail(processed);

        return processed.toString();
    }

    private static void appendEscapedReplacement(Matcher m, StringBuffer sb, String replacement) {
        final String escaped = replacement.replace("\\", "\\\\").replace("$", "\\$");
        m.appendReplacement(sb, escaped);
    }

    private static String removeSpacesBeforeEndTag(final String text) {
        return text.replaceFirst("\\s*/>$", "/>");
    }

    private static boolean isExternalLink(String href) {
        return EXTERNAL_LINK_PATTERN.matcher(href).find();
    }

}
