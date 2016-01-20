/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.restapi.content.html;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.rewriter.impl.SimpleContentRewriter;
import org.hippoecm.hst.restapi.content.ResourceContext;
import org.hippoecm.hst.restapi.content.linking.LinkConversionException;
import org.hippoecm.hst.restapi.content.linking.RestApiLinkCreator;
import org.hippoecm.hst.restapi.content.linking.Link;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_CONTENT;
import static org.hippoecm.repository.HippoStdNodeType.NT_HTML;

public class RestApiHtmlParser {

    private final static Logger log =
            LoggerFactory.getLogger(SimpleContentRewriter.class);
    public static final String DATA_HIPPO_LINK_ATTR = "data-hippo-link";

    private HtmlCleaner htmlCleaner;
    private RestApiLinkCreator restApiLinkCreator;

    public void setHtmlCleaner(final HtmlCleaner htmlCleaner) {
        this.htmlCleaner = htmlCleaner;
    }

    public void setRestApiLinkCreator(final RestApiLinkCreator restApiLinkCreator) {
        this.restApiLinkCreator = restApiLinkCreator;
    }

    /**
     * @param context
     * @param htmlNode
     * @return
     */
    public ParsedContent parseContent(final ResourceContext context, final Node htmlNode) throws RepositoryException, IllegalArgumentException {
        if (!htmlNode.isNodeType(NT_HTML)) {
            log.warn("Only nodes of type '{}' can be parsed for their content but nodetype for '{}' " +
                    "is '{}'.", NT_HTML, htmlNode.getPath(), htmlNode.getPrimaryNodeType().getName());
            throw new IllegalArgumentException(String.format("Only nodes of type '%s' can be parsed for their content", NT_HTML));
        }
        final ContentParser parser = new ContentParser(context, htmlNode, htmlCleaner, restApiLinkCreator);
        return parser.parse();
    }

    /**
     * InternalContentRestApiHtmlRewriter is *not* thread-safe because it also collects 'Link' objects next to rewriting
     * the html
     */
     static class ContentParser extends SimpleContentRewriter {

        private final ResourceContext context;
        private final Node htmlNode;
        private final HtmlCleaner htmlCleaner;
        private final RestApiLinkCreator restApiLinkCreator;
        private Map<String, Link> linkMap = new LinkedHashMap<>();

        public ContentParser(final ResourceContext context, final Node htmlNode, final HtmlCleaner htmlCleaner, final RestApiLinkCreator restApiLinkCreator) {
            this.context = context;
            this.htmlNode = htmlNode;
            this.htmlCleaner = htmlCleaner;
            this.restApiLinkCreator = restApiLinkCreator;
        }

        public ParsedContent parse() throws RepositoryException {
            final String html = htmlNode.getProperty(HIPPOSTD_CONTENT).getString();
            final HstRequestContext requestContext = context.getRequestContext();
            final Mount targetMount = context.getRequestContext().getResolvedMount().getMount();

            if (html == null) {
                return null;
            }

            TagNode rootNode = htmlCleaner.clean(html);
            TagNode[] links = rootNode.getElementsByName("a", true);

            for (TagNode link : links) {
                String documentPath = link.getAttributeByName("href");
                if (isEmpty(documentPath) || isExternal(documentPath)) {
                    continue;
                } else {
                    final String queryString = substringAfter(documentPath, "?");
                    if (!isEmpty(queryString)) {
                        log.debug("Remove query string '{}' for '{}' for content node '{}'", queryString, documentPath, htmlNode.getPrimaryItem());
                        documentPath = StringUtils.substringBefore(documentPath, "?");
                    }
                    HstLink hstLink = getDocumentLink(documentPath, htmlNode, requestContext, targetMount);
                    if (hstLink == null || hstLink.isNotFound() || hstLink.getPath() == null) {
                        // remove the <a> element and just keep the text
                        log.info("non existing link for '{}' in content '{}', either due to removed document or not allowed to read document. " +
                                " only insert the text, and remove the link", documentPath, htmlNode.getPath());
                        removeLinkElement(link, true);
                        continue;
                    } else {
                        try {
                            if (hstLink.isContainerResource()) {
                                // documentPath is link to a binary
                                linkMap.put(documentPath, new Link(hstLink.toUrlForm(requestContext, true)));
                            } else {
                                // convert document HstLink to a content api link
                                linkMap.put(documentPath, restApiLinkCreator.convert(context, hstLink));
                            }
                            link.removeAttribute("href");
                            link.addAttribute(DATA_HIPPO_LINK_ATTR, documentPath);
                        } catch (LinkConversionException e) {
                            log.warn("Could not convert HstLink content api : {}", e.toString());
                            removeLinkElement(link, true);
                            continue;
                        }
                    }
                }
            }

            final Map<String, String> shortPathToSrcPathMap = new HashMap<>();
            TagNode[] images = rootNode.getElementsByName("img", true);
            for (TagNode image : images) {
                String srcPath = image.getAttributeByName("src");
                if (isEmpty(srcPath) || isExternal(srcPath)) {
                    continue;
                } else {
                    HstLink binaryLink = getBinaryLink(srcPath, htmlNode, requestContext, targetMount);
                    if (binaryLink != null) {
                        if (binaryLink.isNotFound() || binaryLink.getPath() == null) {
                            removeImageElement(image);
                        } else {
                            image.removeAttribute("src");

                            final String shortPath = shortenPath(srcPath, shortPathToSrcPathMap);

                            image.addAttribute(DATA_HIPPO_LINK_ATTR, shortPath);
                            linkMap.put(shortPath, new Link(binaryLink.toUrlForm(requestContext, true)));
                        }
                    } else {
                        removeImageElement(image);
                    }
                }
            }

            TagNode[] targetNodes = rootNode.getElementsByName("body", true);
            if (targetNodes.length > 0) {
                TagNode bodyNode = targetNodes[0];
                return new ParsedContent(htmlCleaner.getInnerHtml(bodyNode), linkMap);
            } else {
                log.warn("Cannot parseContent content for '{}' because there is no 'body' element" + htmlNode.getPath());
                return null;
            }
        }


        private void removeLinkElement(final TagNode link, final boolean keepText) {
            if (keepText) {
                final ContentNode contentNode = new ContentNode(link.getText().toString());
                link.getParent().insertChildBefore(link, contentNode);
            }
            link.getParent().removeChild(link);
        }

        private void removeImageElement(final TagNode image) {
            image.getParent().removeChild(image);
        }

        /**
         * This method translates a {@code srcPath} like "snail-193611_640.jpg/{_document}/hippogallery:thumbnail" to
         * "snail-193611_640.jpg". To avoid that if within the same content another variant of the same image is referenced,
         * eg "snail-193611_640.jpg/{_document}/hippogallery:original", this method keeps track of the map of short src to
         * original srcPath. As a result, "snail-193611_640.jpg/{_document}/hippogallery:original" will map to "snail-193611_640.jpg/1"
         * in case snail-193611_640.jpg" already maps to "snail-193611_640.jpg/{_document}/hippogallery:thumbnail"
         * @param srcPath typically a image/asset path in the html content. Image paths frequently contain a '/', something
         *                like snail-193611_640.jpg/{_document}/hippogallery:thumbnail
         * @param shortPathToSrcPathMap the map that keeps track of which short paths are already in use for all original srcPath's
         * @return the shortPath representation. In case srcPath does not contain a '/', the returned value is equal to srcPath
         */
         static String shortenPath(final String srcPath, final Map<String, String> shortPathToSrcPathMap) {
            if (srcPath.indexOf("/") == -1) {
                shortPathToSrcPathMap.put(srcPath, srcPath);
                return srcPath;
            }

            final String shortBasePath = StringUtils.substringBefore(srcPath, "/");
            String shortPath = shortBasePath;
            int i = 1;
            while (shortPathToSrcPathMap.containsKey(shortPath) && !shortPathToSrcPathMap.get(shortPath).equals(srcPath)) {
                shortPath = shortBasePath + "/" + i++;
            }
            shortPathToSrcPathMap.put(shortPath, srcPath);
            return shortPath;
        }
    }

}
