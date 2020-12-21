/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.rewriter.impl;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.rewriter.ImageVariant;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.utils.SimpleHtmlExtractor;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

/**
 * SimpleContentRewriter
 *
 * Rewrites rich text stored in HippoHtml nodes. Links to repository content are rewritten to URLs.
 *
 * The href attributes of anchor tags can contain a reference to a child node of the HippoHtml node. This child node
 * will contain a link to a repository resource. The same applies to the src attributes of image tags.
 * 
 */
public class SimpleContentRewriter extends AbstractContentRewriter<String> {

    private static final Logger log = LoggerFactory.getLogger(SimpleContentRewriter.class);

    /**
     * External URL resources which are not generated from the repository resources.
     */
    protected static final String[] EXTERNALS = { 
        "http:", 
        "https:", 
        "webdav:", 
        "ftp:", 
        "mailto:", 
        "#", 
        "callto:", 
        "data:", 
        "tel:", 
        "sms:",
        "/",
        "$",
        "sip:" };

    protected static final String LINK_TAG = "<a";
    protected static final String IMG_TAG = "<img";
    protected static final String END_TAG = ">";
    protected static final String HREF_ATTR_NAME = "href=\"";
    protected static final String SRC_ATTR_NAME = "src=\"";
    protected static final String ATTR_END = "\"";
    protected static final Pattern HTML_TAG_PATTERN = Pattern.compile("<html[\\s\\/>]", Pattern.CASE_INSENSITIVE);
    protected static final Pattern BODY_TAG_PATTERN = Pattern.compile("<body[\\s\\/>]", Pattern.CASE_INSENSITIVE);

    private boolean rewritingBinaryLink = false;
    
    public SimpleContentRewriter() {

    }

    @Override
    public String rewrite(final String html, final HstRequestContext requestContext) {
        return getInnerHtml(html);
    }

    @Override
    public String rewrite(final String html, final Node hippoHtmlNode, final HstRequestContext requestContext) {
        return rewrite(html, hippoHtmlNode, requestContext, (Mount)null);
    }
    
    @Override
    public String rewrite(final String html, final Node hippoHtmlNode, final HstRequestContext requestContext,
                          final String targetSiteAlias) {
        final Mount targetMount = requestContext.getMount(targetSiteAlias);
        return rewrite(html, hippoHtmlNode, requestContext, targetMount);
    }

    /**
     * Rewrite link references in rich text to valid URLs. A link reference must match the name of a child node of the
     * HippoHtml node that contains a link to a JCR node.
     * @param html rich text with possible link references
     * @param hippoHtmlNode the node that has child nodes that contain references to JCR nodes
     * @param requestContext the context for the current request
     * @param targetMount mount that the links preferably point to. Implementation can choose to support cross mount links
     *                    as well or not.
     * @return rewritten html with URLs
     */
    @Override
    public String rewrite(final String html, final Node hippoHtmlNode, final HstRequestContext requestContext,
                          final Mount targetMount) {

        // strip off html & body tag
        String rewrittenHtml = getInnerHtml(html);
        if (StringUtils.isEmpty(rewrittenHtml)) {
            return rewrittenHtml;
        }

        // only create if really needed
        StringBuilder sb = null;
        int globalOffset = 0;
        String documentLinkHref;

        while (rewrittenHtml.indexOf(LINK_TAG, globalOffset) > -1) {
            int offset = rewrittenHtml.indexOf(LINK_TAG, globalOffset);

            int hrefIndexStart = rewrittenHtml.indexOf(HREF_ATTR_NAME, offset);
            if (hrefIndexStart == -1) {
                break;
            }

            if (sb == null) {
                sb = new StringBuilder(rewrittenHtml.length());
            }

            hrefIndexStart += HREF_ATTR_NAME.length();
            offset = hrefIndexStart;
            int endTag = rewrittenHtml.indexOf(END_TAG, offset);
            boolean appended = false;
            if (hrefIndexStart < endTag) {
                int hrefIndexEnd = rewrittenHtml.indexOf(ATTR_END, hrefIndexStart);
                if (hrefIndexEnd > hrefIndexStart) {
                    final String documentPath = rewrittenHtml.substring(hrefIndexStart, hrefIndexEnd);

                    offset = endTag;
                    sb.append(rewrittenHtml.substring(globalOffset, hrefIndexStart));

                    documentLinkHref = rewriteDocumentLink(documentPath, hippoHtmlNode, requestContext, targetMount);
                    if (documentLinkHref != null) {
                        sb.append(documentLinkHref);
                    }

                    sb.append(rewrittenHtml.substring(hrefIndexEnd, endTag));
                    appended = true;
                }
            }
            if (!appended && offset > globalOffset) {
                sb.append(rewrittenHtml.substring(globalOffset, offset));
            }
            globalOffset = offset;
        }

        if (sb != null) {
            sb.append(rewrittenHtml.substring(globalOffset, rewrittenHtml.length()));
            rewrittenHtml = String.valueOf(sb);
            sb = null;
        }

        globalOffset = 0;
        String binaryLinkSrc;

        while (rewrittenHtml.indexOf(IMG_TAG, globalOffset) > -1) {
            int offset = rewrittenHtml.indexOf(IMG_TAG, globalOffset);

            int srcIndexStart = rewrittenHtml.indexOf(SRC_ATTR_NAME, offset);

            if (srcIndexStart == -1) {
                break;
            }

            if (sb == null) {
                sb = new StringBuilder(rewrittenHtml.length());
            }
            srcIndexStart += SRC_ATTR_NAME.length();
            offset = srcIndexStart;
            int endTag = rewrittenHtml.indexOf(END_TAG, offset);
            boolean appended = false;
            if (srcIndexStart < endTag) {
                int srcIndexEnd = rewrittenHtml.indexOf(ATTR_END, srcIndexStart);
                if (srcIndexEnd > srcIndexStart) {
                    String srcPath = rewrittenHtml.substring(srcIndexStart, srcIndexEnd);
                    
                    offset = endTag;
                    sb.append(rewrittenHtml.substring(globalOffset, srcIndexStart));

                    binaryLinkSrc = rewriteBinaryLink(srcPath, hippoHtmlNode, requestContext, targetMount);
                    if (binaryLinkSrc != null) {
                        sb.append(binaryLinkSrc);
                    }

                    sb.append(rewrittenHtml.substring(srcIndexEnd, endTag));
                    appended = true;
                }
            }
            if (!appended && offset > globalOffset) {
                sb.append(rewrittenHtml.substring(globalOffset, offset));
            }
            globalOffset = offset;
        }

        if (sb == null) {
            return rewrittenHtml;
        } else {
            sb.append(rewrittenHtml.substring(globalOffset, rewrittenHtml.length()));
            return sb.toString();
        }
    }

    private static String getInnerHtml(final String html) {
        if (html == null) {
            return null;
        }
        final String innerHTML = SimpleHtmlExtractor.getInnerHtml(html, "body", false);
        if (innerHTML == null) {
            if (HTML_TAG_PATTERN.matcher(html).find() || BODY_TAG_PATTERN.matcher(html).find()) {
                return null;
            }
            return html;
        } else {
            return innerHTML;
        }
    }

    /**
     * Rewrites document link in <code>href</code> attribute of anchor tag.
     * @param documentLinkReference reference to a document link child node of the hippoHtmlNode
     * @param hippoHtmlNode the node that contains rich text and child nodes with jcr links
     * @param requestContext the context for the current request
     * @param targetMount mount that the link must point to
     * @return document link reference rewritten to a URL or page not found link
     */
    protected String rewriteDocumentLink(final String documentLinkReference, final Node hippoHtmlNode,
                                         final HstRequestContext requestContext, final Mount targetMount) {
        if (StringUtils.isEmpty(documentLinkReference)) {
            return documentLinkReference;
        }

        if (isExternal(documentLinkReference)) {
            return documentLinkReference;
        }

        final String[] hrefParts = StringUtils.split(documentLinkReference, '?');

        final HstLink documentLink = getDocumentLink(StringUtils.substringBefore(hrefParts[0], "#"), hippoHtmlNode, requestContext, targetMount);
        String rewrittenLinkHref;

        if (documentLink != null && documentLink.getPath() != null) {
            rewrittenLinkHref = documentLink.toUrlForm(requestContext, isFullyQualifiedLinks());
        } else {
            log.debug("could not resolve internal document link for '{}'. Return page not found link", documentLinkReference);
            HstLink notFoundLink = requestContext.getHstLinkCreator().createPageNotFoundLink(requestContext.getResolvedMount().getMount());
            rewrittenLinkHref = notFoundLink.toUrlForm(requestContext, isFullyQualifiedLinks());
        }

        if (hrefParts.length > 1) {
            return rewrittenLinkHref + '?' + hrefParts[1];
        } else {
            if (hrefParts[0].contains("#")) {
                return rewrittenLinkHref + "#" + StringUtils.substringAfter(hrefParts[0], "#");
            } else {
                return rewrittenLinkHref;
            }
        }

    }

    /**
     * Rewrites binary link in <code>src</code> attribute of <code>img</code> tag.
     * @param binaryLinkReference reference to a document link child node of the hippoHtmlNode
     * @param hippoHtmlNode the node that contains rich text and child nodes with jcr links
     * @param requestContext the context for the current request
     * @param targetMount mount that the link must point to
     * @return binary link reference rewritten to a URL or page not found link
     */
    protected String rewriteBinaryLink(final String binaryLinkReference, final Node hippoHtmlNode,
                                       final HstRequestContext requestContext, final Mount targetMount) {
        if (StringUtils.isEmpty(binaryLinkReference)) {
            return binaryLinkReference;
        }

        if (isExternal(binaryLinkReference)) {
            return binaryLinkReference;
        }

        final HstLink binaryLink = getBinaryLink(binaryLinkReference, hippoHtmlNode, requestContext, targetMount);

        if (binaryLink != null && binaryLink.getPath() != null) {
            return binaryLink.toUrlForm(requestContext, isFullyQualifiedLinks());
        } else {
            log.debug("could not resolve internal binary link for '{}'. Return page not found link", binaryLinkReference);
            HstLink notFoundLink = requestContext.getHstLinkCreator().createPageNotFoundLink(requestContext.getResolvedMount().getMount());
            return notFoundLink.toUrlForm(requestContext, isFullyQualifiedLinks());
        }
    }

    protected HstLink getDocumentLink(final String path, final Node hippoHtmlNode, final HstRequestContext requestContext,
                                      final Mount targetMount) {
        return getLink(path, hippoHtmlNode, requestContext, targetMount);
    }

    protected HstLink getBinaryLink(final String path, final Node node, final HstRequestContext requestContext,
                                    final  Mount targetMount) {
        // Instead of adding an extr boolean argument to the getLink(...) method to indicate whether a binaryLink
        // is rewritten or not, we use a 'rewritingBinaryLink' flag to indicate this. This is for historical 
        // backwards compatible reasons, as developers might have overridden 
        // protected HstLink getLink(String path, Node node, HstRequestContext reqContext, Mount targetMount) {
        // already, and by calling a new method with extra boolean we might break their implementation. Hence
        // we use the rewritingBinaryLink flag (which might seem strange if you do not know the historical reasons)
        rewritingBinaryLink = true;
        HstLink link =  getLink(path, node, requestContext, targetMount);
        rewritingBinaryLink = false;
        return link;
    }
    
    protected HstLink getLink(final String path, final Node hippoHtmlNode, final HstRequestContext requestContext,
                              final Mount targetMount) {
        final String linkPath = decodePath(path);
        // translate the documentPath to a URL in combination with the Node and the mapping object
        if (linkPath.startsWith("/")) {
            // this is an absolute path, which is not an internal content link. We just try to create a link for it directly
            if (targetMount == null) {
                return requestContext.getHstLinkCreator().create(linkPath, requestContext.getResolvedMount().getMount());
            } else {
                return requestContext.getHstLinkCreator().create(linkPath, targetMount);
            }
        } else {
            // relative node, most likely a mirror node:
            String nodePath = null;
            try {
                nodePath = hippoHtmlNode.getPath();
                if (rewritingBinaryLink) {

                    if (!isValidBinariesPath(nodePath, linkPath)) {
                        return null;
                    }
                    final String[] binaryPathSegments = linkPath.split("/");
                    final Node mirrorNode = hippoHtmlNode.getNode(binaryPathSegments[0]);
                    if (!mirrorNode.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                        log.info("For '{}' a node of type hippo:mirror of hippo:facetselect is expected but was of type '{}'. Cannot " +
                                "create a link for that node type.", mirrorNode.getPath(), mirrorNode.getPrimaryNodeType().getName());
                        return null;
                    }
                    final String uuid = mirrorNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                    Node referencedNode = mirrorNode.getSession().getNodeByIdentifier(uuid);
                    if (!referencedNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                        log.info("Unable to rewrite path '{}' for node '{}' to proper binary url : Expected link to a " +
                                "node of type hippo:handle but was of type '{}'.",
                                new String[]{linkPath, nodePath, referencedNode.getPrimaryNodeType().getName()});
                        return null;
                    }
                    final Node binaryDocument = referencedNode.getNode(referencedNode.getName());
                    if (getImageVariant() != null) {
                        ImageVariant imageVariant = getImageVariant();

                        Node binary = null;
                        if (imageVariant.getReplaces().isEmpty()) {
                            // replace binaryPathVariantSegments[2] regardless the variant
                            if (binaryDocument.hasNode(imageVariant.getName())) {
                                binary = binaryDocument.getNode(imageVariant.getName());
                            } else if (imageVariant.isFallback()) {
                                binary = binaryDocument.getNode(binaryPathSegments[2]);
                            }
                        } else {
                            // only replace binaryPathVariantSegments[2] if it is included in imageVariant.getReplaces()
                            if (imageVariant.getReplaces().contains(binaryPathSegments[2])) {
                                if (binaryDocument.hasNode(imageVariant.getName())) {
                                    binary = binaryDocument.getNode(imageVariant.getName());
                                } else if (imageVariant.isFallback()) {
                                    binary = binaryDocument.getNode(binaryPathSegments[2]);
                                }
                            } else {
                                binary = binaryDocument.getNode(binaryPathSegments[2]);
                            }
                        }
                        if (binary == null) {
                            log.info("Unable to rewrite path '{}' for node '{}' to proper binary url for imageVariant '{}'.", new String[]{linkPath, nodePath, imageVariant.getName()});
                            return null;
                        }
                        return createInternalLink(binary, requestContext, targetMount);
                    } else {
                        final Node binary = binaryDocument.getNode(binaryPathSegments[2]);
                        return createInternalLink(binary, requestContext, targetMount);
                    }

                } else {
                    final Node mirrorNode = hippoHtmlNode.getNode(linkPath);
                    if (mirrorNode.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                        String uuid = mirrorNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                        Node referencedNode = mirrorNode.getSession().getNodeByIdentifier(uuid);
                        if (referencedNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                            if (!referencedNode.hasNode(referencedNode.getName())) {
                                log.info("Unable to rewrite path '{}' for node '{}' to proper url because no (readable) document" +
                                        " node below linked handle node: '{}'.", new String[]{linkPath, nodePath});
                                return null;
                            }
                            referencedNode = referencedNode.getNode(referencedNode.getName());
                        }
                        return createInternalLink(referencedNode, requestContext, targetMount);
                    } else {
                        log.info("For '{}' a node of type hippo:mirror of hippo:facetselect is expected but was of type '{}'. Cannot " +
                                "create a link for that node type.", mirrorNode.getPath(), mirrorNode.getPrimaryNodeType().getName());
                    }
                }
            } catch (ItemNotFoundException | PathNotFoundException e) {
                log.info("Unable to rewrite path '{}' for node '{}' to proper url : '{}'.", new String[]{linkPath, nodePath, e.getMessage()});
            } catch (RepositoryException e) {
                if (e.getCause() instanceof IllegalArgumentException) {
                    // invalid docbase, do not log content issues on warn level
                    log.info("Unable to rewrite path '{}' for node '{}' to proper url : '{}'.", new String[]{linkPath, nodePath, e.getMessage()});
                } else {
                    log.warn("Unable to rewrite path '{}' for node '{}' to proper url : '{}'.", new String[]{linkPath, nodePath, e.getMessage()});
                }
            }
        }
        return null;
    }

    protected String decodePath(final String path) {
        try {
            return URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.warn("UnsupportedEncodingException for documentPath");
            return path;
        }
    }

    /**
     * Create an HstLink to a referenced node in rich text.
     * @param referencedNode the node to create a link to
     * @param requestContext the context for the current request
     * @param targetMount mount that the link by preference points to. may be null. If not null, a link for a different
     *                    {@link Mount} might be returned
     * @return link to the referenced node and optional target mount. target mount is ignored in case links must be canonical.
     * @throws RepositoryException if no path can be retrieved from the referencedNode
     */
    protected HstLink createInternalLink(final Node referencedNode, final HstRequestContext requestContext, final Mount targetMount)
            throws RepositoryException {
        if (isCanonicalLinks()) {
            if (targetMount != null) {
                log.info("TargetMount is defined to create a link for, but target mount is ignored in case a canonical link is " +
                                "requested. Ignoring target mount '{}' but instead return canonical link for nodepath '{}'.",
                        targetMount, referencedNode.getPath());
            }
            return requestContext.getHstLinkCreator().createCanonical(referencedNode, requestContext);
        }
        if (targetMount == null) {
            return requestContext.getHstLinkCreator().create(referencedNode, requestContext);
        } else {
            return requestContext.getHstLinkCreator().create(referencedNode, targetMount, true);
        }
    }

    private static boolean isValidBinariesPath(final String nodePath, final String relPath) {
        final String[] binaryPathSegments = relPath.split("/");
        if (binaryPathSegments.length == 3 && "{_document}".equals(binaryPathSegments[1])) {
          return true;
        }
        log.info("Unable to rewrite relPath '{}' for node '{}' to proper url : '{}'. For binary links we expect" +
                " a relative relPath in the form /a/{_document}/myproject:thumbnail.", new String[]{relPath, nodePath});
        return false;
    }

    /**
     * Check to see if a tag does not reference a dynamic resource
     * @param tagReference the tag reference
     * @return true if the tag does not reference a dynamic resource
     */
    protected boolean isExternal(final String tagReference) {
        for (final String prefix : EXTERNALS) {
            if (tagReference.startsWith(prefix)) {
                return true;
            }
        }
        if (tagReference.contains("://")) {
            return true;
        }
        return false;
    }

}
