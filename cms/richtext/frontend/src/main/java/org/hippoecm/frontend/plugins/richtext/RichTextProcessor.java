/*
 *  Copyright 2008 Hippo.
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

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.protocol.http.WicketURLDecoder;
import org.hippoecm.frontend.plugins.xinha.services.links.ExternalXinhaLink;

public class RichTextProcessor {

    private static Pattern LINK_AND_IMAGES_PATTERN = Pattern.compile("<(a|img)(\\s+)(.*?)(src|href)=\"(.*?)\"(.*?)>",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    private static Pattern IMG_PATTERN = Pattern.compile("<img[^>]+>", Pattern.CASE_INSENSITIVE);
    private static Pattern SRC_PATTERN = Pattern.compile("src=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static Pattern FACET_SELECT_PATTERN = Pattern.compile("facetselect=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static Pattern FACET_SELECT_OR_SRC_PATTERN = Pattern.compile("(facetselect|type|src)=\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);

    private static Pattern LINK_PATTERN = Pattern.compile("<a[^>]+>", Pattern.CASE_INSENSITIVE);
    private static Pattern HREF_PATTERN = Pattern.compile("href=\"[^\"]+\"", Pattern.CASE_INSENSITIVE);
    private static Pattern RESOURCE_DEFINITION_PATTERN = Pattern.compile("/\\{_document\\}/([^/]+)$", Pattern.CASE_INSENSITIVE);

    private static ResourceReference BROKEN_IMAGE = new ResourceReference(RichTextProcessor.class, "broken-image-32.png");

    /**
     * Decorate the targets of relative image links in a text.  Text and decorator may
     * neither be null.
     */
    public static String prefixImageLinks(String text, IImageURLProvider decorator) {

        StringBuffer processed = new StringBuffer();
        Matcher imageMatcher = IMG_PATTERN.matcher(text);

        while (imageMatcher.find()) {
            String img = imageMatcher.group();
            StringBuffer newImg = new StringBuffer();
            Matcher sourceMatcher = SRC_PATTERN.matcher(img);

            if (sourceMatcher.find()) {
                String src = sourceMatcher.group();
                String link = sourceMatcher.group(1);

                if (!link.startsWith("http:") && !link.startsWith("https:")) {
                    String url;
                    try {
                        url = decorator.getURL(link);
                    } catch (RichTextException ex) {
                        url = RequestCycle.get().urlFor(BROKEN_IMAGE, null).toString();
                    }
                    sourceMatcher.appendReplacement(newImg, ("src=\"" + url + "\"").replace("\\", "\\\\").replace("$", "\\$"));
                    newImg.append(' ');
                    newImg.append("facetselect=\"");
                    newImg.append(link);
                    newImg.append("\" ");
                    Matcher resourceDefinitionMatcher = RESOURCE_DEFINITION_PATTERN.matcher(link);
                    if (resourceDefinitionMatcher.find()) {
                        String type = resourceDefinitionMatcher.group(1);
                        newImg.append("type=\"");
                        newImg.append(type);
                        newImg.append("\" ");
                    }
                } else {
                    sourceMatcher.appendReplacement(newImg, src.replace("\\", "\\\\").replace("$", "\\$"));
                }
            }

            sourceMatcher.appendTail(newImg);
            imageMatcher.appendReplacement(processed, newImg.toString().replace("\\", "\\\\").replace("$", "\\$"));
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
            Matcher s = FACET_SELECT_OR_SRC_PATTERN.matcher(img);

            String src = null;
            String facet = null;
            while (s.find()) {
                String srcOrFacet = s.group();
                if (srcOrFacet.startsWith("src")) {
                    src = srcOrFacet;
                } else if (srcOrFacet.startsWith("facetselect")) {
                    facet = srcOrFacet;
                }
                s.appendReplacement(newImg, "");
            }
            if (src != null && facet != null) {
                Matcher fs = FACET_SELECT_PATTERN.matcher(facet);
                if (fs.find()) {
                    fs.appendReplacement(newImg, ("src=\"" + fs.group(1) + "\"").replace("\\", "\\\\").replace("$",
                            "\\$"));
                }
                fs.appendTail(newImg);
            } else if (src != null) {
                newImg.append(src);
            } else if (facet != null) {
                newImg.append(facet);
            }
            s.appendTail(newImg);
            m.appendReplacement(processed, newImg.toString().replace("\\", "\\\\").replace("$", "\\$"));
        }

        m.appendTail(processed);
        return processed.toString();
    }

    /**
     * Return the internal links, i.e. the links to other documents / images / assets
     * in the text.  The text may not be null.
     */
    public static Set<String> getInternalLinks(String text) {
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
            linkName = WicketURLDecoder.PATH_INSTANCE.decode(linkName);
            links.add(linkName);
        }
        return links;
    }

    public static String decorateLinks(String text, ILinkDecorator decorator) {
        StringBuffer processed = new StringBuffer();
        Matcher m = LINK_PATTERN.matcher(text);
        while (m.find()) {
            String anchor = m.group();
            StringBuffer newAnchor = new StringBuffer();
            Matcher s = HREF_PATTERN.matcher(anchor);
            if (s.find()) {
                String href = s.group();
                // skip href=
                String link = href.substring(5);

                if (link.charAt(0) == '"') {
                    link = link.substring(1);
                }
                if (link.charAt(link.length() - 1) == '"') {
                    link = link.substring(0, link.length() - 1);
                }

                link = isExternalLink(link) ? decorator.externalLink(link) : decorator.internalLink(link);
                s.appendReplacement(newAnchor, link.replace("\\", "\\\\").replace("$", "\\$"));
            }
            s.appendTail(newAnchor);
            m.appendReplacement(processed, newAnchor.toString().replace("\\", "\\\\").replace("$", "\\$"));
        }
        m.appendTail(processed);

        return processed.toString().replaceAll("<a target=\"_blank\"", "<a ");
    }

    private static boolean isExternalLink(String link) {
        for (String protocol : ExternalXinhaLink.PROTOCOLS) {
            if (link.startsWith(protocol)) {
                return true;
            }
        }
        return false;
    }

}
