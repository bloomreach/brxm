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
package org.hippoecm.hst.tag;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.core.linking.HstLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract supporting class for Hst Link tags
 */

public class HstLinkTag extends TagSupport {
    

    private final static Logger log = LoggerFactory.getLogger(HstLinkTag.class);
    
    private static final long serialVersionUID = 1L;

    protected HstLink link;
    
    protected String path;
    
    protected String var;
    
    protected String context;
    
    protected String scope;

    protected Boolean escapeXml = true;
        
    protected Map<String, List<String>> parametersMap = new HashMap<String, List<String>>();
    
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
        
        if(this.link != null && this.path != null) { 
           log.warn("Both HstLink & a path are set in single hst:link. Using the HstLink and disregard the path");
        }
        
        if(this.link == null && this.path == null) {
            log.warn("Cannot get a link because no value or path is set");
            return EVAL_PAGE;
        }
        
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();
        String characterEncoding = response.getCharacterEncoding();
        
        if (characterEncoding == null) {
            characterEncoding = "UTF-8";
        }
        
        StringBuilder url = new StringBuilder();

        if (this.context != null) {
            url.append(this.context);
        } else {
            url.append(request.getContextPath());
            url.append(request.getServletPath());
        }
        
        if(this.link != null) {
            this.path = link.getPath();
        }
        
        if (!this.path.startsWith("/")) {
            url.append("/");
        }
        url.append(this.path);
        
        String urlString = response.encodeURL(url.toString());

    
        if (var == null) {
            try {               
                JspWriter writer = pageContext.getOut();
                writer.print(urlString);
            } catch (IOException ioe) {
                throw new JspException(
                    "Portlet/ResourceURL-Tag Exception: cannot write to the output writer.");
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
            
            pageContext.setAttribute(var, urlString, varScope);
        }
        
        /*cleanup*/
        parametersMap.clear();
        var = null;
        scope = null;
        context = null;
        path = null;
        link = null;
        
        return EVAL_PAGE;
    }
    
    

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#release()
     */
    @Override
    public void release(){
        super.release();        
    }
    
    
    /**
     * Returns the var property.
     * @return String
     */
    public String getVar() {
        return var;
    }
    
    public String getContext() {
        return context;
    }
    
    public String getScope() {
        return scope;
    }
    
    public HstLink getValue() {
        return link;
    }
    
    
    public void setValue(HstLink hstLink) {
        this.link = hstLink;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * Sets the var property.
     * @param var The var to set
     * @return void
     */
    public void setVar(String var) {
        this.var = var;
    }
    
    public void setContext(String context) {
        this.context = context;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
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
