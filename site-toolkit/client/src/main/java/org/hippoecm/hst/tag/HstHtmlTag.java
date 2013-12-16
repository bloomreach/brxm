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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.VariableInfo;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.rewriter.ContentRewriter;
import org.hippoecm.hst.content.rewriter.ImageVariant;
import org.hippoecm.hst.content.rewriter.impl.SimpleContentRewriter;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstHtmlTag extends TagSupport {

    private final static Logger log = LoggerFactory.getLogger(HstHtmlTag.class);
    
    private static final long serialVersionUID = 1L;

    protected HippoHtml hippoHtml;

    protected String var;
    
    protected String scope;
        
    protected ContentRewriter<String> contentRewriter;

    protected String formattedText;

    protected String text;

    protected enum RewriteMode { HIPPOHTML, FORMATTEDTEXT, UNKNOWN, TEXT }

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

        final HstRequestContext requestContext = RequestContextProvider.get();

        if(requestContext == null) {
            log.error("Cannot continue HstHtmlTag because no HstRequestContext available");
            cleanup();
            return EVAL_PAGE;
        }

        String characterEncoding = pageContext.getResponse().getCharacterEncoding();
        if (characterEncoding == null) {
            characterEncoding = "UTF-8";
        }

        RewriteMode mode = determineMode();
        log.debug("Determined rewrite mode: " + mode);

        String html;
        if(mode.equals(RewriteMode.HIPPOHTML) || mode.equals(RewriteMode.FORMATTEDTEXT)) {
            if (contentRewriter == null) {
                contentRewriter = new SimpleContentRewriter();
            }
        }
        switch (mode) {
            case HIPPOHTML:
                contentRewriter.setFullyQualifiedLinks(fullyQualifiedLinks);
                contentRewriter.setImageVariant(imageVariant);
                html = contentRewriter.rewrite(hippoHtml.getContent(), hippoHtml.getNode(), requestContext);
                break;
            case FORMATTEDTEXT:
                html = contentRewriter.rewrite(formattedText, requestContext);
                break;
            case TEXT:
                html = lineEndingsToHTML(text);
                break;
            default:
                log.info("No input available to rewrite.");
                cleanup();
                return EVAL_PAGE;
        }

        if(html == null) {
            html = StringUtils.EMPTY;
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

    private RewriteMode determineMode() {
        if((hippoHtml != null && hippoHtml.getContent() != null)) {
            return RewriteMode.HIPPOHTML;
        }
        if(formattedText != null) {
            return RewriteMode.FORMATTEDTEXT;
        }
        if(text != null) {
            return RewriteMode.TEXT;
        } else {
            return RewriteMode.UNKNOWN;
        }
    }

    private String lineEndingsToHTML(final String html) {
        String escaped = StringEscapeUtils.escapeHtml(html);

        if (StringUtils.isBlank(escaped)) {
            return escaped;
        } else {
            escaped = StringUtils.replace(escaped, "\n\n", "</p><p>");
            escaped = StringUtils.replace(escaped, "<p></p>", "<br/><br/>");
            escaped = StringUtils.replace(escaped, "\n", "<br/>");

            StringBuilder builder = new StringBuilder("<p>");
            builder.append(escaped);
            builder.append("</p>");

            return builder.toString();
        }
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
        formattedText = null;
        text = null;
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

    public void setFormattedText(final String formattedText) {
        this.formattedText = formattedText;
    }

    public void setText(final String text) {
        this.text = text;
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
