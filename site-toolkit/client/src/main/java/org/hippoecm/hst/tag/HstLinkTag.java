/*
 *  Copyright 2010 Hippo.
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.utils.PageContextPropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract supporting class for Hst Link tags
 */

public class HstLinkTag extends ParamContainerTag {
    

    private final static Logger log = LoggerFactory.getLogger(HstLinkTag.class);
    
    private static final long serialVersionUID = 1L;

    protected HstLink link;
    
    protected HippoBean hippoBean;

    protected String path;

    protected String siteMapItemRefId;
    
    protected String subPath;
    
    protected String var;
    
    protected String scope;

    protected boolean external;

    /**
     * boolean indicating whether the link that will be created is the canonical link. The canonical link is always the same, regardless the current context, in other 
     * words, regardless the current URL. 
     */
    protected boolean canonical;
    
    /**
     * boolean indicating whether the link that will be created is wrt the current URL and virtual location of the jcr Node backing the HippoBean or wrt the canonical location.
     * Note that this is different then the variable <code>canonical</code> : <code>canonical</code> true or false refers to whether the link should be
     * created wrt the current url or not. 
     */
    protected boolean navigationStateful;
    
    /**
     * The alias of the mount the link is meant for
     */
    protected String mountAlias;
    
    protected boolean skipTag; 

    protected Boolean escapeXml = true;
    
    /**
     * if defined, first a link for this preferSiteMapItem is tried to be created. 
     */
    protected HstSiteMapItem preferSiteMapItem;
    
    /**
     * whether to fallback to normal linkrewriting when the preferSiteMapItem was not able to linkrewrite the item. Default true
     */
    protected boolean fallback = true;
        
    
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
        if(skipTag) {
            return EVAL_PAGE;
        }
        if(this.link == null && this.path == null && this.hippoBean == null && siteMapItemRefId == null) {
            log.warn("Cannot get a link because no link , path, node or sitemapItemRefId is set");
            return EVAL_PAGE;
        }
        
       HttpServletRequest servletRequest = (HttpServletRequest) pageContext.getRequest();
       HstRequestContext reqContext = getHstRequestContext(servletRequest);
       
       if(reqContext == null) {
           log.warn("There is no HstRequestContext on the request. Cannot create an HstLink outside the hst request processing. Return");
           return EVAL_PAGE;
       } 
       
       if( (preferSiteMapItem != null || navigationStateful) && mountAlias != null) {
           log.error("It is not allowed in a hst:link tag to configure a mount in combination with 'navigationStateful == true' or a hst:sitemapitem in the hst:link.");
           throw new JspException("It is not allowed in a hst:link tag to configure a mount in combination with 'navigationStateful == true' or a hst:sitemapitem in the hst:link.");
       }
       
       SiteMount mount = null;
       if(mountAlias != null) {
          mount = reqContext.getMount(mountAlias);
          if(mount == null) {
              throw new JspException("Cannot resolve mount with alias '"+mountAlias+"' for current request. Cannot create a link for '"+path+"'");
          }
       } else {
           mount = reqContext.getResolvedSiteMount().getSiteMount();
       }
       
       if(this.hippoBean != null) {
            if(hippoBean.getNode() == null) {
                log.warn("Cannot get a link for a detached node");
                return EVAL_PAGE;
            }
            if(mountAlias != null) {
                this.link = reqContext.getHstLinkCreator().create(hippoBean.getNode(), mount);
            }
            else if(canonical) {
                this.link = reqContext.getHstLinkCreator().createCanonical(hippoBean.getNode(), reqContext, preferSiteMapItem);
            } else {
                this.link = reqContext.getHstLinkCreator().create(hippoBean.getNode(), reqContext, preferSiteMapItem, fallback, navigationStateful);
            }
        }
        
        if(this.link == null && this.path != null) {
            VirtualHost virtualHost = reqContext.getVirtualHost();
            boolean containerResource = (virtualHost != null && virtualHost.getVirtualHosts().isExcluded(this.path));
            this.link = reqContext.getHstLinkCreator().create(this.path, mount, containerResource);
        }

        if(this.link == null && this.siteMapItemRefId != null) {
            this.link = reqContext.getHstLinkCreator().createByRefId(siteMapItemRefId, reqContext.getResolvedSiteMount().getSiteMount());
        }

        if(this.link == null) {
            log.warn("Unable to rewrite link. Return EVAL_PAGE");
            return EVAL_PAGE;
        }
        
        if(subPath != null) {
            link.setSubPath(subPath);
        }
        
        String urlString = this.link.toUrlForm(reqContext , external);
        
        try {
            if (navigationStateful) {
                // append again the current queryString as we are context relative
                Map<String, String[]> currentRequestParameterMap = reqContext.getBaseURL().getParameterMap();
                Map<String, String[]> parameterMapForLink = combineParametersMap(parametersMap,
                        currentRequestParameterMap);
                if (parameterMapForLink != null && !parameterMapForLink.isEmpty()) {
                    StringBuilder queryString = new StringBuilder();
                    boolean firstParamDone = false;
                    for (Entry<String, String[]> entry : parameterMapForLink.entrySet()) {
                        if(removedParametersList.contains(entry.getKey())) {
                            // set to null by hst:param tag, thus skip
                            continue;
                        }
                        String name = entry.getKey();
                        if (entry.getValue() != null) {
                            for (String value : entry.getValue()) {
                                if(value != null) {
                                    queryString.append(firstParamDone ? "&" : "?").append(name).append("=").append(URLEncoder.encode(value, reqContext.getBaseURL().getCharacterEncoding()));
                                    firstParamDone = true;
                                }
                            }
                        }

                    }
                    urlString += queryString.toString();
                }
            } else if (!parametersMap.isEmpty()) {
                boolean firstParamDone = false;
                StringBuilder queryString = new StringBuilder();
                for (Entry<String, List<String>> entry : parametersMap.entrySet()) {
                    if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                        String name = entry.getKey();
                        if(removedParametersList.contains(name)) {
                            // set to null by hst:param tag, thus skip
                            continue;
                        }
                        if (entry.getValue() != null) {
                            for (String value : entry.getValue()) {
                                if(value != null) {
                                    queryString.append(firstParamDone ? "&" : "?").append(name).append("=").append(URLEncoder.encode(value, reqContext.getBaseURL().getCharacterEncoding()));
                                    firstParamDone = true;
                                }
                            }
                        }
                    }
                }
                urlString += queryString.toString();
            }
        } catch (UnsupportedEncodingException e) {
            throw new JspException("UnsupportedEncodingException on the base url", e);
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
        removedParametersList.clear();
        var = null;
        hippoBean = null;
        scope = null;
        path = null;
        siteMapItemRefId = null;
        subPath = null;
        link = null;
        external = false;
        skipTag = false;
        preferSiteMapItem = null;
        fallback = true;
        canonical = false;
        navigationStateful = false;
        mountAlias = null;
        
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
    
    public String getSubPath(){
        return this.subPath;
    }
    
    public boolean isExternal(){
        return this.external;
    }
    
    public void setLink(HstLink hstLink) {
        this.link = hstLink;
    }
    
    public void setLinkByBeanPath(String beanPath) {
        this.link = (HstLink) PageContextPropertyUtils.getProperty(pageContext, beanPath);
    }
    
    public void setExternal(boolean external) {
        this.external = external;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setSiteMapItemRefId(String siteMapItemRefId) {
        this.siteMapItemRefId = siteMapItemRefId;
    }
    
    public void setSubPath(String subPath) {
        this.subPath = subPath;
    }
    
    public void setCanonical(boolean canonical) {
        this.canonical = canonical;
    }
    
    /**
     * @deprecated use {@link #setNavigationStateful(boolean)} instead
     */
    @Deprecated
    public void setContextRelative(boolean navigationStateful) {
        this.navigationStateful = navigationStateful;
    }
    
    public void setNavigationStateful(boolean navigationStateful) {
        this.navigationStateful = navigationStateful;
    }
    
    public void setHippobean(HippoBean hippoBean) {
        this.hippoBean = hippoBean;
    }
    
    public void setMount(String mount) {
        this.mountAlias = mount;
    }
    
    public String getMount(){
        return mountAlias;
    }
    
    public void setHippobeanByBeanPath(String beanPath) {
        this.hippoBean = (HippoBean) PageContextPropertyUtils.getProperty(pageContext, beanPath);
        if(this.hippoBean == null) {
            log.debug("No bean for '{}'. The tag will be skipped.", beanPath);
            skipTag = true;
        }
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


    public void setPreferredSiteMapItem(HstSiteMapItem preferSiteMapItem) {
        this.preferSiteMapItem = preferSiteMapItem;
    }


    public void setFallback(boolean fallback) {
       this.fallback = fallback;
    }
    

    protected HstRequestContext getHstRequestContext(HttpServletRequest servletRequest) {
        return (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
    }
    private Map<String, String[]> combineParametersMap(Map<String, List<String>> parametersMap,
            Map<String, String[]> currentRequestParameterMap) {
        if((parametersMap == null || parametersMap.isEmpty()) && (currentRequestParameterMap == null || currentRequestParameterMap.isEmpty())) {
           // no params at all
            return null;
        }
        LinkedHashMap<String, String[]> combinedParametersMap = new LinkedHashMap<String, String[]>();
        List<String> alreadyAddedParameters = new ArrayList<String>();
        // to maintain correct order, first inject the parameters from the current request if there are request parameters
        if((currentRequestParameterMap != null && !currentRequestParameterMap.isEmpty())) {
            for(Entry<String, String[]> entry : currentRequestParameterMap.entrySet()) {
                if(parametersMap != null && parametersMap.containsKey(entry.getKey())) {
                    // replace an existing query param for the current url.
                    combinedParametersMap.put(entry.getKey(), parametersMap.get(entry.getKey()).toArray(new String[parametersMap.get(entry.getKey()).size()]));
                    alreadyAddedParameters.add(entry.getKey());
                } else {
                    combinedParametersMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if(parametersMap != null && !parametersMap.isEmpty()) {
            for(Entry<String, List<String>> entry : parametersMap.entrySet()) {
                if(alreadyAddedParameters.contains(entry.getKey())) {
                    // already added: skip
                    continue;
                }
                combinedParametersMap.put(entry.getKey(), entry.getValue().toArray(new String[entry.getValue().size()]));
            }
        }
        
        return combinedParametersMap;
    }
}
