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

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.hosting.VirtualHost;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.MatchedMapping;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract supporting class for Hst Link tags
 */

public class HstLinkTag extends TagSupport {
    

    private final static Logger log = LoggerFactory.getLogger(HstLinkTag.class);
    
    private static final long serialVersionUID = 1L;

    protected HstLink link;
    
    protected HippoBean hippoBean;
    
    protected String path;
    
    protected String var;
    
    protected String scope;
    
    protected boolean external;

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
        
        if(this.link == null && this.path == null && this.hippoBean == null) {
            log.warn("Cannot get a link because no link , path or node is set");
            return EVAL_PAGE;
        }
        
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();

        // if hst request/esponse is retrieved, then this servlet has been dispatched by hst component.
        HstRequest hstRequest = (HstRequest) request.getAttribute(ContainerConstants.HST_REQUEST);
        HstResponse hstResponse = (HstResponse) request.getAttribute(ContainerConstants.HST_RESPONSE);
        
        String characterEncoding = response.getCharacterEncoding();
        
        if (characterEncoding == null) {
            characterEncoding = "UTF-8";
        }
        
        StringBuilder url = new StringBuilder();

        if(this.hippoBean != null) {
            if(hippoBean.getNode() == null) {
                log.warn("Cannot get a link for a detached node");
                return EVAL_PAGE;
            }
            if(hstRequest == null){
                log.warn("Cannot only get links for HstRequest");
                return EVAL_PAGE;
            }
            HstRequestContext reqContext = hstRequest.getRequestContext();
            this.link = reqContext.getHstLinkCreator().create(hippoBean.getNode(), reqContext.getResolvedSiteMapItem());
        }
        
        String[] pathElements = null;

        if (this.link != null) {
            pathElements = link.getPathElements();
        }
        
        boolean containerResource = false;
        
        if (this.path != null) {
            containerResource = true;
            path = PathUtils.normalizePath(path);
            pathElements = path.split("/");
        }
        
        if(pathElements == null) {
            log.warn("Unable to rewrite link. Return EVAL_PAGE");
            return EVAL_PAGE;
        }
        
        for(String elem : pathElements) {
            String enc = response.encodeURL(elem);
            url.append("/").append(enc);
        }
        
        String urlString = null;
        
        if (containerResource) {
            HstURL hstUrl = hstResponse.createResourceURL(ContainerConstants.CONTAINER_REFERENCE_NAMESPACE);
            hstUrl.setResourceID(url.toString());
            urlString = hstUrl.toString();
        } else {
            urlString = hstResponse.createNavigationalURL(url.toString()).toString();
            String customParams =  getCustomParameters(request, response);
            if(customParams != null) {
                urlString += customParams;
            }
        }
        
        if(this.isExternal()) {
            MatchedMapping mapping;
            if(( mapping = hstRequest.getRequestContext().getMatchedMapping()) != null) {
                StringBuilder builder = new StringBuilder();
                VirtualHost vhost = mapping.getMapping().getVirtualHost();
                if(vhost.getProtocol() == null) {
                    builder.append("http");
                } else {
                    builder.append(vhost.getProtocol());
                }
                builder.append("://").append(request.getServerName());
                if(vhost.isPortVisible()) {
                    builder.append(":").append(vhost.getPortNumber());
                }
                urlString = builder.toString() + urlString;
            } else {
                log.warn("Cannot create external link because there is no virtual host to use");
            }
        }
        
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
        path = null;
        link = null;
        external = false;
        
        return EVAL_PAGE;
    }
    
    
    /**
     * If you want in your application to append some queryString to urls, you can implement this method. Make sure that
     * if there are parameters, you should return a String starting with a <code>?</code>
     * 
     * @param request
     * @param response
     * @return a queryString to add to the link
     */
    protected String getCustomParameters(HttpServletRequest request, HttpServletResponse response) {
        return null;
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
    
    public String getScope() {
        return scope;
    }
    
    public HstLink getLink() {
        return link;
    }
    
    public HippoBean getHippobean(){
        return this.hippoBean;
    }
    
    public String getPath(){
        return this.path;
    }
    
    public boolean isExternal(){
        return this.external;
    }
    
    public void setLink(HstLink hstLink) {
        this.link = hstLink;
    }

    public void setExternal(boolean external) {
        this.external = external;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public void setHippobean(HippoBean hippoBean) {
        this.hippoBean = hippoBean;
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
