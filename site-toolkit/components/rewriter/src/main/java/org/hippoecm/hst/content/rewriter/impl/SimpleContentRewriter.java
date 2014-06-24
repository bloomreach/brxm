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
package org.hippoecm.hst.content.rewriter.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.rewriter.ImageVariant;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.utils.SimpleHtmlExtractor;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SimpleContentRewriter
 * 
 * @version $Id: SimpleContentRewriter.java 24267 2010-10-11 09:09:56Z aschrijvers $
 */
public class SimpleContentRewriter extends AbstractContentRewriter<String> {
    
    private final static Logger log = LoggerFactory.getLogger(SimpleContentRewriter.class);

    /**
     * External URL resources which are not generated from the repository resources.
     */
    protected static final String[] EXTERNALS = { "http:", "https:", "webdav:", "ftp:", "mailto:", "#", "callto:", "data:" };

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
    public String rewrite(String html, HstRequestContext requestContext) {
        return getInnerHtml(html);
    }

    @Override
    public String rewrite(String html, Node node, HstRequestContext requestContext) {
        return rewrite(html, node, requestContext, (Mount)null);
    }
    
    @Override
    public String rewrite(String html, Node node, HstRequestContext requestContext, String targetSiteAlias) {
        Mount targetMount = requestContext.getMount(targetSiteAlias);
        return rewrite(html, node, requestContext, targetMount);
    }
    
    @Override
    public String rewrite(final String html, Node node, HstRequestContext requestContext, Mount targetMount) {

        // strip off html & body tag
        String rewrittenHtml = getInnerHtml(html);
        if (StringUtils.isEmpty(rewrittenHtml)) {
            return rewrittenHtml;
        }

        // only create if really needed
        StringBuilder sb = null;
        int globalOffset = 0;
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
                    String documentPath = rewrittenHtml.substring(hrefIndexStart, hrefIndexEnd);

                    offset = endTag;
                    sb.append(rewrittenHtml.substring(globalOffset, hrefIndexStart));
                    
                    if(isExternal(documentPath)) {
                        sb.append(documentPath);
                    } else {
                        String queryString = StringUtils.substringAfter(documentPath, "?");
                        boolean hasQueryString = !StringUtils.isEmpty(queryString); 
                        if (hasQueryString) {
                            documentPath = StringUtils.substringBefore(documentPath, "?");
                        }
                        
                        HstLink href = getDocumentLink(documentPath,node, requestContext, targetMount);
                        if (href != null && href.getPath() != null) {
                            sb.append(href.toUrlForm(requestContext, isFullyQualifiedLinks()));
                        } else {
                            log.debug("could not resolve internal document link for '{}'. Return page not found link", documentPath);
                            HstLink notFoundLink = requestContext.getHstLinkCreator().createPageNotFoundLink(requestContext.getResolvedMount().getMount());
                            sb.append(notFoundLink.toUrlForm(requestContext, isFullyQualifiedLinks()));
                        }
                        
                        if (hasQueryString) {
                            sb.append('?').append(queryString);
                        }
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
                   
                    if(isExternal(srcPath)) {
                        sb.append(srcPath);
                    } else {
                        HstLink binaryLink = getBinaryLink(srcPath, node, requestContext, targetMount);
                        if (binaryLink != null && binaryLink.getPath() != null) {
                            sb.append(binaryLink.toUrlForm(requestContext, isFullyQualifiedLinks()));
                        } else {
                            log.debug("could not resolve internal binary link for '{}'. Return page not found link", srcPath);
                            HstLink notFoundLink = requestContext.getHstLinkCreator().createPageNotFoundLink(requestContext.getResolvedMount().getMount());
                            sb.append(notFoundLink.toUrlForm(requestContext, isFullyQualifiedLinks()));
                        }
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

    private String getInnerHtml(final String html) {
        if (html == null) {
            return null;
        }
        String innerHTML = SimpleHtmlExtractor.getInnerHtml(html, "body", false);
        if (innerHTML == null) {
            if (HTML_TAG_PATTERN.matcher(html).find() || BODY_TAG_PATTERN.matcher(html).find()) {
                return null;
            }
            return html;
        } else {
            return innerHTML;
        }
    }

    protected HstLink getDocumentLink(String path, Node node, HstRequestContext requestContext, Mount targetMount) {
        return getLink(path, node, requestContext, targetMount);
    }
    
    
    protected HstLink getBinaryLink(String path, Node node, HstRequestContext requestContext, Mount targetMount) {
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
    
    protected HstLink getLink(String path, Node node, HstRequestContext reqContext, Mount targetMount) {
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            log.warn("UnsupportedEncodingException for documentPath");
        }

        // translate the documentPath to a URL in combination with the Node and the mapping object
        if (path.startsWith("/")) {
            // this is an absolute path, which is not an internal content link. We just try to create a link for it directly
            if (targetMount == null) {
                return reqContext.getHstLinkCreator().create(path, reqContext.getResolvedMount().getMount());
            } else {
                return reqContext.getHstLinkCreator().create(path, targetMount);
            }
        } else {
            // relative node, most likely a mirror node:
            String nodePath = null;
            final String relPath = path;
            try {
                nodePath = node.getPath();
                if (rewritingBinaryLink) {

                    if (!isValidBinariesPath(nodePath, relPath)) {
                        return null;
                    }
                    String[] binaryPathSegments = relPath.split("/");
                    Node mirrorNode = node.getNode(binaryPathSegments[0]);
                    if (!mirrorNode.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                        log.info("For '{}' a node of type hippo:mirror of hippo:facetselect is expected but was of type '{}'. Cannot " +
                                "create a link for that node type.", mirrorNode.getPath(), mirrorNode.getPrimaryNodeType().getName());
                        return null;
                    }
                    String uuid = mirrorNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                    Node referencedNode = mirrorNode.getSession().getNodeByIdentifier(uuid);
                    if (!referencedNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                        log.info("Unable to rewrite path '{}' for node '{}' to proper binary url : Expected link to a " +
                                "node of type hippo:handle but was of type '{}'.", new String[]{relPath, nodePath, referencedNode.getPrimaryNodeType().getName()});
                        return null;
                    }
                    Node binaryDocument = referencedNode.getNode(referencedNode.getName());
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
                            log.info("Unable to rewrite path '{}' for node '{}' to proper binary url for imageVariant '{}'.", new String[]{relPath, nodePath, imageVariant.getName()});
                            return null;
                        }
                        return createLink(binary, reqContext, targetMount);
                    } else {
                        Node binary = binaryDocument.getNode(binaryPathSegments[2]);
                        return createLink(binary, reqContext, targetMount);
                    }

                } else {
                    Node mirrorNode = node.getNode(relPath);
                    if (mirrorNode.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                        String uuid = mirrorNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                        Node referencedNode = mirrorNode.getSession().getNodeByIdentifier(uuid);
                        if (referencedNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                            if (!referencedNode.hasNode(referencedNode.getName())) {
                                log.info("Unable to rewrite path '{}' for node '{}' to proper url because no (readable) document" +
                                        " node below linked handle node: '{}'.", new String[]{relPath, nodePath});
                                return null;
                            }
                            referencedNode = referencedNode.getNode(referencedNode.getName());
                        }
                        return createLink(referencedNode, reqContext, targetMount);
                    } else {
                        log.info("For '{}' a node of type hippo:mirror of hippo:facetselect is expected but was of type '{}'. Cannot " +
                                "create a link for that node type.", mirrorNode.getPath(), mirrorNode.getPrimaryNodeType().getName());
                    }
                }
            } catch (ItemNotFoundException e) {
                log.info("Unable to rewrite path '{}' for node '{}' to proper url : '{}'.", new String[]{relPath, nodePath, e.getMessage()});
            } catch (PathNotFoundException e) {
                log.info("Unable to rewrite path '{}' for node '{}' to proper url : '{}'.", new String[]{relPath, nodePath, e.getMessage()});
            } catch (RepositoryException e) {
                log.warn("Unable to rewrite path '{}' for node '{}' to proper url : '{}'.", new String[]{relPath, nodePath, e.getMessage()});
            }
        }
        return null;
    }

    private HstLink createLink(final Node node, final HstRequestContext reqContext, final Mount targetMount) throws RepositoryException {
        if (isCanonicalLinks()) {
            if (targetMount != null) {
                log.info("TargetMount is defined to create a link for, but target mount is ignored in case a canonical link is " +
                        "requested. Ignoring target mount '{}' but instead return canonical link for nodepath '{}'.",
                        targetMount.toString(), node.getPath());
            }
            return reqContext.getHstLinkCreator().createCanonical(node, reqContext);
        }
        if (targetMount == null) {
            return reqContext.getHstLinkCreator().create(node, reqContext);
        } else {
            return reqContext.getHstLinkCreator().create(node, targetMount);
        }
    }

    private boolean isValidBinariesPath(final String nodePath, final String relPath) {
        final String[] binaryPathSegments = relPath.split("/");
        if (binaryPathSegments.length == 3 && binaryPathSegments[1].equals("{_document}")) {
          return true;
        }
        log.info("Unable to rewrite relPath '{}' for node '{}' to proper url : '{}'. For binary links we expect" +
                " a relative relPath in the form /a/{_document}/myproject:thumbnail.", new String[]{relPath, nodePath});
        return false;
    }

    protected boolean isExternal(String path) {
        for (String prefix : EXTERNALS) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

}
