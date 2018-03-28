/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.v09.content.rewriter;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson.LinkModel;
import org.hippoecm.hst.content.rewriter.impl.SimpleContentRewriter;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBefore;

public class HtmlContentRewriter extends SimpleContentRewriter {
    private final static Logger log =
            LoggerFactory.getLogger(HtmlContentRewriter.class);
    private HtmlCleaner htmlCleaner;


    public HtmlContentRewriter(final HtmlCleaner htmlCleaner) {
        this.htmlCleaner = htmlCleaner;
    }


    @Override
    public String rewrite(final String html, final Node node,
                          final HstRequestContext requestContext,
                          final Mount targetMount) {

        if (html == null) {
            return null;
        }

        try {
            final TagNode rootNode = htmlCleaner.clean(html);

            final TagNode[] anchorTags = rootNode.getElementsByName("a", true);
            for (TagNode anchorTag : anchorTags) {
                String documentPath = anchorTag.getAttributeByName("href");
                if (StringUtils.isBlank(documentPath)) {
                    continue;
                }
                if (isExternal(documentPath)) {
                    continue;
                } else {
                    String documentPathQueryString = substringAfter(documentPath, "?");
                    if (!isEmpty(documentPathQueryString)) {
                        documentPath = substringBefore(documentPath, "?");
                    }

                    final HstLink hstLink = getDocumentLink(documentPath, node, requestContext, targetMount);
                    if (hstLink == null || hstLink.isNotFound() || hstLink.getPath() == null) {
                        log.info("Could not create a link for '{}'", documentPath);
                        // TODO should we remove the <a> element completely and only keep the text
                        continue;
                    }
                    String rewrittenHref = hstLink.toUrlForm(requestContext, false);
                    if (!isEmpty(documentPathQueryString)) {
                        if (rewrittenHref.contains("?")) {
                            rewrittenHref += "&" + documentPathQueryString;
                        } else {
                            rewrittenHref += "?" + documentPathQueryString;
                        }
                    }
                    // override the href attr
                    setAttribute(anchorTag, "href", rewrittenHref);

                    final String linkType = LinkModel.getLinkType(requestContext, hstLink);
                    setAttribute(anchorTag, "data-type", linkType);
                }
            }

            final TagNode[] imageTags = rootNode.getElementsByName("img", true);

            for (TagNode imageTag : imageTags) {
                final String srcPath = imageTag.getAttributeByName("src");
                if (StringUtils.isBlank(srcPath)) {
                    continue;
                }
                if (isExternal(srcPath)) {
                    continue;
                }

                final HstLink binaryLink = getBinaryLink(srcPath, node, requestContext, targetMount);

                if (binaryLink == null || binaryLink.isNotFound() || binaryLink.getPath() == null) {
                    log.info("Could not create a src for '{}'", srcPath);
                    continue;
                }

                setAttribute(imageTag, "src", binaryLink.toUrlForm(requestContext, false));

            }

            // everything is rewritten. Now write the "body" element as result
            final TagNode[] targetNodes = rootNode.getElementsByName("body", true);
            if (targetNodes.length > 0) {
                TagNode bodyNode = targetNodes[0];
                return htmlCleaner.getInnerHtml(bodyNode);
            } else {
                log.warn("Cannot rewrite content for '{}' because there is no 'body' element" + node.getPath());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private void setAttribute(TagNode tagNode, String attrName, String attrValue) {
        if (tagNode.hasAttribute(attrName)) {
            tagNode.removeAttribute(attrName);
        }
        tagNode.addAttribute(attrName, attrValue);
    }

}