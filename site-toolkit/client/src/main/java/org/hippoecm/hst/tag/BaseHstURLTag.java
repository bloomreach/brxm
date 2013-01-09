/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract supporting class for Hst URL tags (action, redner and resource)
 */

public abstract class BaseHstURLTag extends ParamContainerTag {
    
    private static final Logger log = LoggerFactory.getLogger(BaseHstURLTag.class);
    
    private static final long serialVersionUID = 1L;

    protected String var = null;

    protected Boolean escapeXml = true;

    protected String resourceId;

    protected boolean fullyQualified;

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
        try {
            HstURL url = getUrl();

            if (url == null) {
                throw new IllegalStateException("internal error: url not set");
            }

            url.setResourceID(getResourceId());

            setUrlParameters(url);

            HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();

            //  properly encoding urls to allow non-cookie enabled sessions - ref.) PLUTO-252
            String urlString = response.encodeURL(url.toString());

            if (escapeXml) {
                urlString = doEscapeXml(urlString);
            }
            HstRequestContext requestContext =  HstRequestUtils.getHstRequestContext((HttpServletRequest) pageContext.getRequest());
            if (makeURLFullyQualified(requestContext)) {
                Mount mount = requestContext.getResolvedMount().getMount();
                String scheme = mount.getScheme();
                // When 0, the Mount is port agnostic. Then take port from current container url
                int port = (mount.getPort() == 0 ? requestContext.getBaseURL().getPortNumber() : mount.getPort());

                if (!mount.isPortInUrl() || ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
                    urlString = scheme + "://" + mount.getVirtualHost().getHostName() + urlString;
                } else {
                    urlString = scheme + "://" + mount.getVirtualHost().getHostName() + ":" + port + urlString;
                }
            }

            if (var == null) {
                try {
                    JspWriter writer = pageContext.getOut();
                    writer.print(urlString);
                } catch (IOException ioe) {
                    cleanup();
                    throw new JspException("HstURL-Tag Exception: cannot write to the output writer.");
                }
            }
            else {
                pageContext.setAttribute(var, urlString, PageContext.PAGE_SCOPE);
            }

            return EVAL_PAGE;
        } finally {
            cleanup();
        }
    }

    private boolean makeURLFullyQualified(final HstRequestContext requestContext) {
        if (requestContext == null) {
            if (fullyQualified) {
              log.warn("Cannot make url fully qualified when requestContext is null");
            }
            return false;
        }
        return fullyQualified || requestContext.isFullyQualifiedURLs();
    }

    @Override
    protected void cleanup() {
        resourceId = null;
        escapeXml = true;
        var = null;
        fullyQualified = false;
        super.cleanup();
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
    
    /**
     * Returns escapeXml property.
     * @return Boolean
     */
    public Boolean getEscapeXml() {
        return escapeXml;
    }
    
    /**
     * Returns resource ID property
     * @return
     */
    public String getResourceId() {
        return this.resourceId;
    }
    
    /**
     * Returns true if the generated URL should be a fully qualified URL, 
     * starting with 'http://' or 'https://', etc.
     * @return
     */
    public boolean isFullyQualified() {
        return this.fullyQualified;
    }
    
    /**
     * Sets the var property.
     * @param var The var to set
     * @return void
     */
    public void setVar(String var) {
        this.var = var;
    }
    
    /**
     * Sets the escapeXml property.
     * @param escapeXml
     * @return void
     */
    public void setEscapeXml(Boolean escapeXml) {
        this.escapeXml = escapeXml;
    }
    
    /**
     * Sets the resource ID property.
     * @param resourceId
     */
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
    
    /**
     * Sets the flag to generate URL as a fully qualified URL, 
     * starting with 'http://' or 'https://', etc.
     */
    public void setFullyQualified(boolean fullyQualified) {
        this.fullyQualified = fullyQualified;
    }
    
    /**
     * Copies the parameters from map to the BaseURL.
     * @param url BaseURL
     * @return void
     */
    protected void setUrlParameters(HstURL url) {
        for(String key : parametersMap.keySet()) {
            
            List<String> valueList = parametersMap.get(key);
        
            String[] valueArray = valueList.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
            
            url.setParameter(key, valueArray);
        }
        
        for (String key : removedParametersList) {
            url.setParameter(key, (String) null);
        }
    }
    
    
    /**
     * @return the url
     */
    protected abstract HstURL getUrl();


    /**
     * @param url the url to set
     */
    protected abstract void setUrl(HstURL url);
    
    
    /**
     * Replaces in String str the characters &,>,<,",' 
     * with their corresponding character entity codes.
     * @param str - the String where to replace 
     * @return String 
     */
    protected String doEscapeXml(String str) {
        if(!isEmpty(str)){
            str = str.replaceAll("&", "&amp;");
            str = str.replaceAll("<", "&lt;");
            str = str.replaceAll(">", "&gt;");
            str = str.replaceAll("\"", "&#034;");
            str = str.replaceAll("'", "&#039;");
        }
        return str;
    }
       
    
    /**
     * Checks if the string is empty. 
     * @param str String
     * @return boolean
     */
    private boolean isEmpty(String str) {
        return ((str == null) || (str.length() == 0));
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
