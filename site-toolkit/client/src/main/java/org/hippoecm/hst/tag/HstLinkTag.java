/*
 *  Copyright 2010-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.IdentifiableContentBean;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.hippoecm.hst.util.QueryStringBuilder;
import org.hippoecm.hst.utils.TagUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.INDEX;
import static org.hippoecm.hst.utils.TagUtils.getQueryString;
import static org.hippoecm.hst.utils.TagUtils.writeOrSetVar;

/**
 * Abstract supporting class for Hst Link tags
 */

public class HstLinkTag extends ParamContainerTag {

    private final static Logger log = LoggerFactory.getLogger(HstLinkTag.class);
    
    private static final long serialVersionUID = 1L;
    public static final String UNALLOWED_ATTR_COMBINATION_MSG = "Incorrect usage of hst:link tag. Not allowed to specifcy two of the attributes 'link', 'hippobean', 'path', nodeId or 'siteMapItemRefId' at same time.";

    protected HstLink link;
    
    protected IdentifiableContentBean identifiableContentBean;

    protected String nodeId;

    protected String path;

    protected String siteMapItemRefId;
    
    protected String subPath;
    
    protected String var;
    
    protected String scope;

    protected boolean fullyQualified;

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
    
    /**
     * The type of the mount the link is meant for
     */
    protected String mountType;
    
    protected boolean skipTag; 

    /**
     * if defined, first a link for this preferSiteMapItem is tried to be created. 
     */
    protected HstSiteMapItem preferSiteMapItem;
    
    /**
     * whether to fallback to normal linkrewriting when the preferSiteMapItem was not able to linkrewrite the item. Default true
     */
    protected boolean fallback = true;
        
    /**
     * Whether either the <code>link</code>, <code>path</code>, <code>identifiableContentBean</code> or <code>siteMapItemRefId</code> did
     * have its setter called. Also, only one of the setters is allowed. If none called, we return a hst link for the current
     * URL
     */
    protected boolean linkForAttributeSet = false;

    protected Boolean escapeXml = true;

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException{
    
        if (var != null) {
            TagUtils.removeVar(var, pageContext, scope);
        }
        
        return EVAL_BODY_INCLUDE;
    }
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() throws JspException{
        try {
            if(skipTag) {
                return EVAL_PAGE;
            }

            HttpServletRequest servletRequest = (HttpServletRequest) pageContext.getRequest();
            HstRequestContext reqContext = HstRequestUtils.getHstRequestContext(servletRequest);

            HippoBean hippoBean = null;
            if (identifiableContentBean != null) {
                if (identifiableContentBean instanceof HippoBean) {
                    hippoBean = (HippoBean) identifiableContentBean;
                } else {
                    // TOOD enable custom linkrewriters
                    writeOrSetVar(identifiableContentBean.getIdentifier(), var, pageContext, scope);
                    return EVAL_PAGE;
                }
            }

            if (hippoBean == null && nodeId != null) {
                try {
                    hippoBean = (HippoBean) reqContext.getObjectBeanManager().getObjectByUuid(nodeId);
                } catch (ObjectBeanManagerException e) {
                    log.warn("Cannot find content bean by id: '{}'.", nodeId);
                    return EVAL_PAGE;
                }
            }

            if(linkForAttributeSet) {
                if(link == null && path == null && hippoBean == null && siteMapItemRefId == null) {
                    String dispatcher = (String)servletRequest.getAttribute("javax.servlet.include.servlet_path");
                    if(dispatcher == null) {
                        log.info("Cannot get a link because no link , path, node or sitemapItemRefId is set for a hst:link");
                    } else {
                        log.info("Cannot get a link because no link , path, node or sitemapItemRefId is set for a hst:link in template {}'", dispatcher);
                    }
                    return EVAL_PAGE;
                }
            } else {
                // hst link for current URL requested
                if(reqContext != null && reqContext.getResolvedSiteMapItem() != null) {
                    final ResolvedSiteMapItem r = reqContext.getResolvedSiteMapItem();

                    if (INDEX.equals(r.getHstSiteMapItem().getValue())) {
                        // _index_ sitemap item : The _index_ is never visible in the URL if there is a parent sitemap
                        // item
                        path = StringUtils.substringBeforeLast(r.getPathInfo(), "/" + INDEX);
                    } else {
                        path = r.getPathInfo();
                    }
                }
            }


           if(reqContext == null) {
               if(path != null) {
                   log.debug("Although there is not HstRequestContext, a link for path='{}' is created similar to how the c:url tag would do it", path);
                   String pathInfo = TagUtils.createPathInfoWithoutRequestContext(path, parametersMap, removedParametersList, servletRequest);
                   writeOrSetVar(pathInfo, var, pageContext, scope);
                   return EVAL_PAGE;
               }

               log.info("There is no HstRequestContext on the request. Cannot create an HstLink outside the hst request processing. Return");
               return EVAL_PAGE;
           }

           boolean mountAliasOrTypeSet = (mountAlias != null || mountType != null);

           if( (preferSiteMapItem != null || navigationStateful) && mountAliasOrTypeSet) {
               log.error("It is not allowed in a hst:link tag to configure a mount in combination with 'navigationStateful == true' or a hst:sitemapitem in the hst:link.");
               throw new JspException("It is not allowed in a hst:link tag to configure a mount in combination with 'navigationStateful == true' or a hst:sitemapitem in the hst:link.");
           }

           Mount mount = null;
           if(mountAliasOrTypeSet) {
               if (mountAlias != null && mountType != null) {
                   mount = reqContext.getMount(mountAlias, mountType);
               } else if (mountAlias != null && mountType == null) {
                   mount = reqContext.getMount(mountAlias);
               } else if (mountAlias == null && mountType != null) {
                   mount = reqContext.getMount(reqContext.getResolvedMount().getMount().getAlias(), mountType);
               }
               if(mount == null) {
                   log.info("Cannot resolve mount with alias '{}' (type '{}') for current request. Cannot create a link for '{}'. Return page not found Link for current Mount",
                               new String[] { mountAlias, mountType, path });
                   Mount requestedMount = reqContext.getResolvedMount().getMount();
                   link = reqContext.getHstLinkCreator().create(HstSiteMapUtils.getPath(requestedMount, requestedMount.getPageNotFound()), requestedMount);
               }
           } else {
               mount = reqContext.getResolvedMount().getMount();
           }

           if(link == null && hippoBean != null) {
                if(hippoBean.getNode() == null) {
                    log.info("Cannot get a link for a detached node");
                    return EVAL_PAGE;
                }
                if(mountAliasOrTypeSet) {
                    link = reqContext.getHstLinkCreator().create(hippoBean.getNode(), mount);
                }
                else if(canonical) {
                    link = reqContext.getHstLinkCreator().createCanonical(hippoBean.getNode(), reqContext, preferSiteMapItem);
                } else {
                    link = reqContext.getHstLinkCreator().create(hippoBean.getNode(), reqContext, preferSiteMapItem, fallback, navigationStateful);
                }
            }

            if (link == null && path != null) {
                String pathInfo;
                if (path.contains("?")) {
                    mergeParameters(StringUtils.substringAfter(path, "?"), parametersMap);
                    pathInfo  = StringUtils.substringBefore(path, "?");
                } else {
                    pathInfo = path;
                }

                link = reqContext.getHstLinkCreator().create(pathInfo, mount);
            }

            if(link == null && this.siteMapItemRefId != null) {
                link = reqContext.getHstLinkCreator().createByRefId(siteMapItemRefId, mount);
            }

            if(link == null) {
                log.info("Unable to rewrite link. Return EVAL_PAGE");
                return EVAL_PAGE;
            }

            if(subPath != null) {
                link.setSubPath(subPath);
            }

            String urlString = link.toUrlForm(reqContext , fullyQualified);

            try {
                if (navigationStateful) {
                    // append again the current queryString as we are context relative
                    Map<String, String[]> currentRequestParameterMap = reqContext.getBaseURL().getParameterMap();
                    Map<String, String[]> parameterMapForLink = combineParametersMap(parametersMap,
                            currentRequestParameterMap);
                    if (parameterMapForLink != null && !parameterMapForLink.isEmpty()) {
                        QueryStringBuilder queryStringBuilder = new QueryStringBuilder(reqContext.getBaseURL().getURIEncoding());
                        for (Entry<String, String[]> entry : parameterMapForLink.entrySet()) {
                            if(removedParametersList.contains(entry.getKey())) {
                                // set to null by hst:param tag, thus skip
                                continue;
                            }
                            String name = entry.getKey();
                            if (entry.getValue() != null) {
                                for (String value : entry.getValue()) {
                                    if (value != null) {
                                        queryStringBuilder.append(name, value);
                                    }
                                }
                            }
                        }
                        urlString += queryStringBuilder.toString();
                    }
                } else if (!parametersMap.isEmpty()) {
                    String queryString = getQueryString(reqContext.getBaseURL().getURIEncoding(), parametersMap, removedParametersList);
                    if (StringUtils.isNotBlank(queryString)) {
                        if (urlString.contains("?")) {
                            // remove the "?" from the query string
                            urlString += "&" + queryString.substring(1);
                        } else {
                            urlString += queryString;
                        }
                    }
                }
            } catch (UnsupportedEncodingException e) {
                throw new JspException("UnsupportedEncodingException on the base url", e);
            }

            if (escapeXml) {
                urlString = HstRequestUtils.escapeXml(urlString);
            }

            writeOrSetVar(urlString, var, pageContext, scope);

            return EVAL_PAGE;
        } finally {
            cleanup();
        }
    }

    private void mergeParameters(final String queryString, final Map<String, List<String>> parameters) {
        String[] paramPairs = queryString.split("&");
        for (String paramPair : paramPairs) {
            String[] paramNameAndValue = paramPair.split("=");
            if (paramNameAndValue.length == 0) {
                // skip
            } else if (paramNameAndValue.length == 1) {
                // don't set value null or empty list as that is interpreted as removal and then a developer cannot
                // use path=/foo/bar?1.01.01 any more.
                parameters.put(paramNameAndValue[0],  Lists.newArrayList(""));
            } else {
                parameters.put(paramNameAndValue[0], Lists.newArrayList(paramNameAndValue[1]));
            }
            int positionInRemoveList = removedParametersList.indexOf(paramNameAndValue[0]);
            if (positionInRemoveList > -1){
                removedParametersList.remove(positionInRemoveList);
            }
        }
    }


    @Override
    protected void cleanup() {
        super.cleanup();
        var = null;
        identifiableContentBean = null;
        nodeId = null;
        scope = null;
        path = null;
        siteMapItemRefId = null;
        subPath = null;
        link = null;
        fullyQualified = false;
        skipTag = false;
        preferSiteMapItem = null;
        fallback = true;
        canonical = false;
        navigationStateful = false;
        mountAlias = null;
        mountType = null;
        linkForAttributeSet = false;
        escapeXml = true;
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
    
    public IdentifiableContentBean getHippobean(){
        return this.identifiableContentBean;
    }
    
    public String getNodeId(){
        return this.nodeId;
    }
    
    public String getPath(){
        return path;
    }
    
    public String getSubPath(){
        return this.subPath;
    }

    /**
     * Returns escapeXml property.
     * @return Boolean
     */
    public Boolean getEscapeXml() {
        return escapeXml;
    }

    public void setLink(HstLink hstLink) {
        if(linkForAttributeSet) {
           log.warn(UNALLOWED_ATTR_COMBINATION_MSG + " Ignore the attr link '{}'", link);
           return;    
        } 
        linkForAttributeSet = true;
        this.link = hstLink;
    }

    public void setFullyQualified(boolean fullyQualified) {
        this.fullyQualified = fullyQualified;
    }
    
    public boolean isFullyQualified() {
        return this.fullyQualified;
    }
    
    public void setPath(String path) {
        if(linkForAttributeSet) {
            log.warn(UNALLOWED_ATTR_COMBINATION_MSG + " Ignore the attr path '{}'", path);
            return;    
         } 
         linkForAttributeSet = true;
        this.path = path;
    }

    public void setSiteMapItemRefId(String siteMapItemRefId) {
        if(linkForAttributeSet) {
            log.warn(UNALLOWED_ATTR_COMBINATION_MSG + " Ignore the attr siteMapItemRefId '{}'", siteMapItemRefId);
            return;    
         } 
         linkForAttributeSet = true;
        this.siteMapItemRefId = siteMapItemRefId;
    }
    
    public void setSubPath(String subPath) {
        this.subPath = subPath;
    }
    
    public void setCanonical(boolean canonical) {
        this.canonical = canonical;
    }
    
    public void setNavigationStateful(boolean navigationStateful) {
        this.navigationStateful = navigationStateful;
    }
    
    public void setHippobean(IdentifiableContentBean identifiableContentBean) {
        if(linkForAttributeSet) {
            log.warn(UNALLOWED_ATTR_COMBINATION_MSG + " Ignore the attr identifiableContentBean '{}'", identifiableContentBean.getIdentifier());
            return;    
         } 
         linkForAttributeSet = true;
        this.identifiableContentBean = identifiableContentBean;
    }

    public void setNodeId(String nodeId) {
        if(linkForAttributeSet) {
            log.warn(UNALLOWED_ATTR_COMBINATION_MSG + " Ignore the attr nodeId '{}'", nodeId);
            return;
        }
        linkForAttributeSet = true;
        this.nodeId = nodeId;
    }

    public void setMount(String mount) {
        this.mountAlias = mount;
    }
    
    public String getMount(){
        return mountAlias;
    }
    
    public String getMountType() {
        return mountType;
    }

    public void setMountType(String mountType) {
        this.mountType = mountType;
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

    /**
     * Sets the escapeXml property.
     * @param escapeXml
     * @return void
     */
    public void setEscapeXml(Boolean escapeXml) {
        this.escapeXml = escapeXml;
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
    
    private Map<String, String[]> combineParametersMap(final Map<String, List<String>> parametersMap,
                                                       final Map<String, String[]> currentRequestParameterMap) {
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
