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
package org.hippoecm.hst.tag;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.rewriter.ContentRewriter;
import org.hippoecm.hst.content.rewriter.ImageVariant;
import org.hippoecm.hst.content.rewriter.impl.SimpleContentRewriter;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstHtmlTag extends TagSupport {
    

    private final static Logger log = LoggerFactory.getLogger(HstHtmlTag.class);
    
    private static final long serialVersionUID = 1L;

    protected HippoHtml hippoHtml;

    protected String var;
    
    protected String scope;
        
    protected ContentRewriter<String> contentRewriter;

    /**
     * Whether links should be rewritten to fully qualified links (URLs) including scheme, host, port etc. 
     * Default false
     */
    protected boolean fullyQualifiedLinks;

    /**
     * Holds the {@link org.hippoecm.hst.content.rewriter.ImageVariant} when there is configured an {@link org.hippoecm.hst.content.rewriter.ImageVariant} and is <code>null</code>
     * when no image variant has been specified
     */
    protected ImageVariant imageVariant;
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException{
       
        if (var != null) {
            pageContext.removeAttribute(var, PageContext.PAGE_SCOPE);
        }
        
        return EVAL_BODY_INCLUDE;
    }
    
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() throws JspException{

        HttpServletRequest servletRequest = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) pageContext.getResponse();
        HstRequest hstRequest = HstRequestUtils.getHstRequest(servletRequest);
        HstResponse hstResponse = HstRequestUtils.getHstResponse(servletRequest, servletResponse);
        
        if(hstRequest == null || hstResponse == null) {
            log.error("Cannot continue HstHtmlTag because response/request not an instance of hst response/request");
            cleanup();
            return EVAL_PAGE;
        }
        
        String characterEncoding = hstResponse.getCharacterEncoding();
        
        if (characterEncoding == null) {
            characterEncoding = "UTF-8";
        }
        
        if(hippoHtml == null || hippoHtml.getContent() == null ) {
            log.warn("Node or content is null. Return");
            cleanup();
            return EVAL_PAGE;
        }
            
        String html = hippoHtml.getContent();
       
        if(hippoHtml.getNode() != null) {
            if (contentRewriter == null) {
                contentRewriter = new SimpleContentRewriter();
            }
            contentRewriter.setFullyQualifiedLinks(fullyQualifiedLinks);
            contentRewriter.setImageVariant(imageVariant);
            html = contentRewriter.rewrite(html, hippoHtml.getNode(), hstRequest.getRequestContext());
        } else {
            log.warn("Node should be a HippoNode and response a HstResponse");
        }
        
        if(html == null) {
            html = "";
        }
        if (var == null) {
            try {               
                JspWriter writer = pageContext.getOut();
                writer.print(html);
            } catch (IOException ioe) {
                cleanup();
                throw new JspException(
                    " Exception: cannot write to the output writer.");
            }
        } 
        else {
            int varScope = PageContext.PAGE_SCOPE;
            
            if (this.scope != null) {
                if ("request".equals(this.scope)) {
                    varScope = PageContext.REQUEST_SCOPE;
                } else if ("session".equals(this.scope)) {
                    varScope = PageContext.SESSION_SCOPE;
                } else if ("application".equals(this.scope)) {
                    varScope = PageContext.APPLICATION_SCOPE;
                }
            }
            
            pageContext.setAttribute(var, html, varScope);
        }

        cleanup();
        return EVAL_PAGE;
    }
    
    

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#release()
     */
    @Override
    public void release(){
        super.release();
        cleanup();

    }

    protected void cleanup() {
        var = null;
        scope = null;
        fullyQualifiedLinks = Boolean.FALSE;
        hippoHtml = null;
        contentRewriter = null;
        imageVariant = null;
    }


    /**
     * Returns the var property.
     * @return String
     */
    public String getVar() {
        return var;
    }
    
    public String getScope() {
        return scope;
    }
  
    public HippoHtml getHippohtml(){
        return this.hippoHtml;
    }
    
    public ContentRewriter<String> getContentRewriter() {
        return contentRewriter;
    }
    
    /**
     * @param fullyQualifiedLinks flag to define whether internal links are rewritten into fully qualified links (URLs)
     *                                 (including scheme and domain)
     */
    public void setFullyQualifiedLinks(boolean fullyQualifiedLinks) {
        this.fullyQualifiedLinks = fullyQualifiedLinks;
    }

    /**
     * Sets the var property.
     * @param var The var to set
     * @return void
     */
    public void setVar(String var) {
        this.var = var;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public void setHippohtml(HippoHtml hippoHtml) {
        this.hippoHtml = hippoHtml;
    }
    
    public void setContentRewriter(ContentRewriter<String> contentRewriter) {
        this.contentRewriter = contentRewriter;
    }

    public void setImageVariant(final ImageVariant imageVariant) {
        this.imageVariant = imageVariant;
    }


    /* -------------------------------------------------------------------*/
        
    /**
     * TagExtraInfo class for HstURLTag.
     */
    public static class TEI extends TagExtraInfo {
        
        public VariableInfo[] getVariableInfo(TagData tagData) {
            VariableInfo vi[] = null;
            String var = tagData.getAttributeString("var");
            if (var != null) {
                vi = new VariableInfo[1];
                vi[0] =
                    new VariableInfo(var, "java.lang.String", true,
                                 VariableInfo.AT_BEGIN);
            }
            return vi;
        }

    }
    

    
}
