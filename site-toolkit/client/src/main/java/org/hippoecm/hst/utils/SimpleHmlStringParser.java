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
package org.hippoecm.hst.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleHmlStringParser {

    private final static Logger log = LoggerFactory.getLogger(SimpleHmlStringParser.class);
    public static final String[] EXTERNALS = {"http:", "https:", "webdav:", "ftp:", "mailto:", "#"};
    public static final String LINK_TAG = "<a";
    public static final String IMG_TAG = "<img";
    public static final String END_TAG = ">";
    public static final String HREF_ATTR_NAME = "href=\"";
    public static final String SRC_ATTR_NAME = "src=\"";
    public static final String ATTR_END = "\"";

    public static String parse(Node node, String html, HttpServletRequest request, HstResponse response) {
        // only create if really needed
        StringBuilder sb = null;
        
        // strip off html & body tag
        String innerHTML = SimpleHtmlExtractor.getInnerHtml(html, "body", false);
        if(innerHTML != null) {
            html = innerHTML;
        }
        html.trim();
        
        HstRequestContext reqContext = ((HstRequest)request).getRequestContext();
        
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
                        HstLink href = getLink(documentPath,node, reqContext, response);
                        if(href != null && href.getPath() != null) {
                            sb.append(href.toUrlForm((HstRequest)request, response, false));
                        } else {
                           log.warn("Skip href because url is null"); 
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
                        HstLink binaryLink = getLink(srcPath, node, reqContext, response);
                        if(binaryLink != null && binaryLink.getPath() != null) {
                             sb.append(binaryLink.toUrlForm((HstRequest)request, response, false));
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

    public static HstLink getLink(String path, Node node, HstRequestContext reqContext,
            HttpServletResponse response) {
        
        try {
            path = URLDecoder.decode(path, "utf-8");
        } catch (UnsupportedEncodingException e1) {
            log.warn("UnsupportedEncodingException for documentPath");
        }

        // translate the documentPath to a URL in combination with the Node and the mapping object
        if (path.startsWith("/")) {
            // absolute location, try to translate directly
            log.warn("Cannot rewrite absolute path '{}'. Expected a relative path. Return '{}'", path, path);
            return null;
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
                Node mirrorNode = ((HippoWorkspace)((HippoSession)node.getSession()).getWorkspace()).getHierarchyResolver().getNode(node, path);    
                if (mirrorNode != null) {
                    return reqContext.getHstLinkCreator().create(mirrorNode, reqContext.getResolvedSiteMapItem());
                } else {
                    log.warn("Cannot find node '{}' for internal link for document '{}'. Cannot create link", path, node.getPath());
                }
            } catch (InvalidItemStateException e) {
                log.warn("Unable to rewrite '{}' to proper url : '{}'. Return null", path, e.getMessage());
            } catch (RepositoryException e) {
                log.warn("Unable to rewrite '{}' to proper url : '{}'. Return null", path, e.getMessage());
            }
        }
        return null;
    }
    
    public static boolean isExternal(String path) {
        for (String prefix : EXTERNALS) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
    
}
