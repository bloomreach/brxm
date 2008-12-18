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
package org.hippoecm.hst.core.template.node.content;

import javax.jcr.Node;

import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Object that searches for HTML links in a content string and replaces
 * the link with valid URL links.
 */
public class SimpleContentRewriterImpl implements ContentRewriter {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /*
     * log all rewriting to the SourceRewriter interface
     */
    private Logger log = LoggerFactory.getLogger(ContentRewriter.class);
    
    private final String LINK_TAG = "<a";
    private final String IMG_TAG = "<img";
    private final String END_TAG = ">";
    private final String HREF_ATTR_NAME = "href=\"";
    private final String SRC_ATTR_NAME = "src=\"";
    private final String ATTR_END = "\"";
    
    private PathTranslator pathTranslator;
    private HstRequestContext hstRequestContext;
    
    /*
     * constructor with default URLPathTranslatorImpl();
     */
    public SimpleContentRewriterImpl(HstRequestContext hstRequestContext) {
        this.pathTranslator = new PathTranslatorImpl(new PathToHrefTranslatorImpl(hstRequestContext), new PathToSrcTranslatorImpl(hstRequestContext));
        this.hstRequestContext = hstRequestContext;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.core.template.node.content.SourceRewriter#replace(javax.jcr.Session, java.lang.String)
     */
    public String replace(final Node node,String content) {
        long start = System.currentTimeMillis();
        // only create if really needed
        StringBuffer sb = null;
        
        int globalOffset = 0;
        while (content.indexOf(LINK_TAG, globalOffset) > -1) {
            int offset = content.indexOf(LINK_TAG, globalOffset);
         
            int hrefIndexStart = content.indexOf(HREF_ATTR_NAME, offset);
            if(hrefIndexStart == -1) {
                break;
            }
            
            if (sb == null) {
                sb = new StringBuffer(content.length());
            }
            
            hrefIndexStart += HREF_ATTR_NAME.length(); 
            offset = hrefIndexStart;
            int endTag = content.indexOf(END_TAG, offset);
            boolean appended = false;
            if(hrefIndexStart < endTag) {
                int hrefIndexEnd = content.indexOf(ATTR_END, hrefIndexStart);
                if(hrefIndexEnd > hrefIndexStart) {
                    String documentPath = content.substring(hrefIndexStart, hrefIndexEnd);
                    log.debug("trying to translate document path : " + documentPath );
                    String url = pathTranslator.documentPathToHref(node, documentPath);
                    log.debug("translated '" + documentPath + "' --> '" + url +"'");
                    offset = endTag; 
                    sb.append(content.substring(globalOffset, hrefIndexStart));
                    sb.append(url);
                    sb.append(content.substring(hrefIndexEnd, endTag));
                    appended = true;
                }
            }
            if(!appended && offset > globalOffset) {
               sb.append(content.substring(globalOffset, offset)); 
            }
            globalOffset = offset;
        }
        
        if (sb != null) {
            sb.append(content.substring(globalOffset, content.length()));
            content = String.valueOf(sb);
            sb = null;
        }
        
        globalOffset = 0;
        while (content.indexOf(IMG_TAG, globalOffset) > -1) {
            int offset = content.indexOf(IMG_TAG, globalOffset);
            
            int srcIndexStart = content.indexOf(SRC_ATTR_NAME, offset);
           
            if(srcIndexStart == -1) {
                break;
            }
            
            if (sb == null) {
                sb = new StringBuffer(content.length());
            }
            srcIndexStart += SRC_ATTR_NAME.length(); 
            offset = srcIndexStart;
            int endTag = content.indexOf(END_TAG, offset);
            boolean appended = false;
            if(srcIndexStart < endTag) {
                int srcIndexEnd = content.indexOf(ATTR_END, srcIndexStart);
                if(srcIndexEnd > srcIndexStart) {
                    String documentPath = content.substring(srcIndexStart, srcIndexEnd);
                    log.debug("translating document path : " + documentPath );
                    String src = pathTranslator.documentPathToSrc(node, documentPath);
                    log.debug("translated '" + documentPath + "' --> '" + src +"'");
                    offset = endTag;
                    sb.append(content.substring(globalOffset, srcIndexStart));
                    sb.append(src);
                    sb.append(content.substring(srcIndexEnd, endTag));
                    appended = true;
                }
            }
            if(!appended && offset > globalOffset) {
               sb.append(content.substring(globalOffset, offset)); 
            }
            globalOffset = offset;
        }
        
        log.debug("Parsing content and linkrewriting took " + (System.currentTimeMillis() - start) + " ms.");
        
        if (sb == null) {
            return content;
        } else {
            sb.append(content.substring(globalOffset, content.length()));
            return sb.toString();
        }
    }

}
