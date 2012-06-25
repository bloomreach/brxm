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
package org.hippoecm.hst.content.rewriter.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.rewriter.ImageVariant;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.utils.SimpleHtmlExtractor;
import org.hippoecm.repository.api.HippoWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SimpleContentRewriter
 * 
 * @version $Id: SimpleContentRewriter.java 24267 2010-10-11 09:09:56Z aschrijvers $
 */
public class SimpleContentRewriter extends AbstractContentRewriter<String> {
    
    private final static Logger log = LoggerFactory.getLogger(SimpleContentRewriter.class);
    
    protected static final String[] EXTERNALS = {"http:", "https:", "webdav:", "ftp:", "mailto:", "#","callto:"};
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
    public String rewrite(String html, Node node, HstRequestContext requestContext) {
        return rewrite(html, node, requestContext, (Mount)null);
    }
    
    @Override
    public String rewrite(String html, Node node, HstRequestContext requestContext, String targetSiteAlias) {
        Mount targetMount = requestContext.getMount(targetSiteAlias);
        return rewrite(html, node, requestContext, targetMount);
    }
    
    @Override
    public String rewrite(String html, Node node, HstRequestContext requestContext, Mount targetMount) {
        // only create if really needed
        StringBuilder sb = null;
        
        // strip off html & body tag
        String innerHTML = SimpleHtmlExtractor.getInnerHtml(html, "body", false);
        
        if (innerHTML == null) {
            if (html == null || HTML_TAG_PATTERN.matcher(html).find() || BODY_TAG_PATTERN.matcher(html).find()) {
                return null;
            }
        } else {
            html = innerHTML;
        }
        
        if ("".equals(html)) {
            return "";
        }
        
        int globalOffset = 0;
        while (html.indexOf(LINK_TAG, globalOffset) > -1) {
            int offset = html.indexOf(LINK_TAG, globalOffset);

            int hrefIndexStart = html.indexOf(HREF_ATTR_NAME, offset);
            if (hrefIndexStart == -1) {
                break;
            }

            if (sb == null) {
                sb = new StringBuilder(html.length());
            }

            hrefIndexStart += HREF_ATTR_NAME.length();
            offset = hrefIndexStart;
            int endTag = html.indexOf(END_TAG, offset);
            boolean appended = false;
            if (hrefIndexStart < endTag) {
                int hrefIndexEnd = html.indexOf(ATTR_END, hrefIndexStart);
                if (hrefIndexEnd > hrefIndexStart) {
                    String documentPath = html.substring(hrefIndexStart, hrefIndexEnd);

                    offset = endTag;
                    sb.append(html.substring(globalOffset, hrefIndexStart));
                    
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
                           log.warn("Skip href because url is null");
                        }
                        
                        if (hasQueryString) {
                            sb.append('?').append(queryString);
                        }
                    }
                    
                    sb.append(html.substring(hrefIndexEnd, endTag));
                    appended = true;
                }
            }
            if (!appended && offset > globalOffset) {
                sb.append(html.substring(globalOffset, offset));
            }
            globalOffset = offset;
        }

        if (sb != null) {
            sb.append(html.substring(globalOffset, html.length()));
            html = String.valueOf(sb);
            sb = null;
        }

        globalOffset = 0;
        while (html.indexOf(IMG_TAG, globalOffset) > -1) {
            int offset = html.indexOf(IMG_TAG, globalOffset);

            int srcIndexStart = html.indexOf(SRC_ATTR_NAME, offset);

            if (srcIndexStart == -1) {
                break;
            }

            if (sb == null) {
                sb = new StringBuilder(html.length());
            }
            srcIndexStart += SRC_ATTR_NAME.length();
            offset = srcIndexStart;
            int endTag = html.indexOf(END_TAG, offset);
            boolean appended = false;
            if (srcIndexStart < endTag) {
                int srcIndexEnd = html.indexOf(ATTR_END, srcIndexStart);
                if (srcIndexEnd > srcIndexStart) {
                    String srcPath = html.substring(srcIndexStart, srcIndexEnd);
                    
                    offset = endTag;
                    sb.append(html.substring(globalOffset, srcIndexStart));
                   
                    if(isExternal(srcPath)) {
                        sb.append(srcPath);
                    } else {
                        HstLink binaryLink = getBinaryLink(srcPath, node, requestContext, targetMount);
                        if (binaryLink != null && binaryLink.getPath() != null) {
                            sb.append(binaryLink.toUrlForm(requestContext, isFullyQualifiedLinks()));
                        } else {
                            log.warn("Could not translate image src. Skip src");
                        }
                    }
                    
                    sb.append(html.substring(srcIndexEnd, endTag));
                    appended = true;
                }
            }
            if (!appended && offset > globalOffset) {
                sb.append(html.substring(globalOffset, offset));
            }
            globalOffset = offset;
        }

        if (sb == null) {
            return html;
        } else {
            sb.append(html.substring(globalOffset, html.length()));
            return sb.toString();
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
            try {
                /*
                 * Important: use here the HippoWorkspace hierarchy resolver and not a direct getNode(path) because the hierarchy resolver
                 * contains logic about how to fetch relative paths for for example a picked image in an RTE field. 
                 * 
                 * For example, the relative path of a picked image in RTE field is something like: facetNode/[some-encoding-wildcard]/thumbnail
                 * 
                 * The hierarchy resolver knows how to solve: [some-encoding-wildcard]
                 * 
                 */
                String variantPath = null;
                boolean fallback = false;
                if (rewritingBinaryLink && getImageVariant() != null) {
                    String[] segments = path.split("/");
                    if (segments.length == 3) {
                        ImageVariant imageVariant = getImageVariant();
                        fallback = imageVariant.isFallback();
                        if (imageVariant.getReplaces().isEmpty()) {
                            // replace segments[2] regardless the variant
                            variantPath = segments[0] + "/" + segments[1] + "/" + imageVariant.getName();
                        } else {
                            // only replace segments[2] if it is included in imageVariant.getReplaces()
                            if (imageVariant.getReplaces().contains(segments[2])) {
                                variantPath = segments[0] + "/" + segments[1] + "/" + imageVariant.getName();
                            }
                        }
                    } else {
                        log.debug("Only know how to get a different variant for links that have 3 segments in its path. Skip variant for path '{}'", path);
                    }
                }
                
                Node mirrorNode = null;
                String triedPath = path;
                if (variantPath != null) {
                    mirrorNode = ((HippoWorkspace) node.getSession().getWorkspace()).getHierarchyResolver().getNode(node, variantPath);
                    triedPath = variantPath;
                    if (mirrorNode == null) {
                        if (fallback) {
                            log.debug("Could not find the image variant '{}', try the original path '{}'", variantPath, path);
                            triedPath = path;
                            mirrorNode = ((HippoWorkspace) node.getSession().getWorkspace()).getHierarchyResolver().getNode(node, path);
                        } else {
                            // warning about this will be logged later because mirrorNode == null
                            log.debug("Could not find the image variant '{}' and fallback is false", variantPath);
                        }
                    } 
                } else {
                    mirrorNode = ((HippoWorkspace) node.getSession().getWorkspace()).getHierarchyResolver().getNode(node, path);
                }
                if (mirrorNode != null) {
                    if (targetMount == null) {
                        return reqContext.getHstLinkCreator().create(mirrorNode, reqContext);
                    } else {
                        return reqContext.getHstLinkCreator().create(mirrorNode, targetMount);
                    }
                } else {
                    log.warn("Cannot find node '{}' for internal link for document '{}'. Cannot create link", triedPath, node.getPath());
                }
            } catch (InvalidItemStateException e) {
                log.warn("Unable to rewrite '{}' to proper url : '{}'. Return null", path, e.getMessage());
            } catch (RepositoryException e) {
                log.warn("Unable to rewrite '{}' to proper url : '{}'. Return null", path, e.getMessage());
            }
        }
        return null;
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
