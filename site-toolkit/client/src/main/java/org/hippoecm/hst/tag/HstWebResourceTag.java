/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import com.google.common.collect.ImmutableList;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.PathUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.webresources.Content;
import org.onehippo.cms7.services.webresources.WebResource;
import org.onehippo.cms7.services.webresources.WebResourceException;
import org.onehippo.cms7.services.webresources.WebResourcesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.utils.TagUtils.createPathInfoWithoutRequestContext;
import static org.hippoecm.hst.utils.TagUtils.getQueryString;
import static org.hippoecm.hst.utils.TagUtils.writeOrSetVar;

/**
 * Abstract supporting class for Hst Link tags
 */

public class HstWebResourceTag extends ParamContainerTag {

    private final static Logger log = LoggerFactory.getLogger(HstWebResourceTag.class);

    private final static String WEB_RESOURCES_SITEMAP_ITEM_ID = "WEB-RESOURCES-ID";
    private final static String WEB_RESOURCE_NAMED_PIPELINE_NAME = "WebResourcePipeline";

    private static final long serialVersionUID = 1L;

    protected String path;

    protected String var;

    protected String scope;

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {

        if (var != null) {
            pageContext.removeAttribute(var, PageContext.PAGE_SCOPE);
        }

        return EVAL_BODY_INCLUDE;
    }

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() throws JspException {
        try {

            HttpServletRequest servletRequest = (HttpServletRequest) pageContext.getRequest();
            HstRequestContext reqContext = HstRequestUtils.getHstRequestContext(servletRequest);

            final String webResourcePath = PathUtils.normalizePath(path);
            final HstSite hstSite;
            if (reqContext == null ||
                    (hstSite = reqContext.getResolvedMount().getMount().getHstSite()) == null ||
                    hstSite.getSiteMap() == null ) {
                log.debug("Although there is no HstRequestContext/HstSite/Sitemap for request, a link for path='{}' is created " +
                        "similar to how the c:url tag would do it", webResourcePath);
                String pathInfo = createPathInfoWithoutRequestContext(webResourcePath, parametersMap, removedParametersList, servletRequest);
                writeOrSetVar(pathInfo, var, pageContext, scope);
                return EVAL_PAGE;
            }

            final HstSiteMapItem webResourceItem = hstSite.getSiteMap().getSiteMapItemByRefId(WEB_RESOURCES_SITEMAP_ITEM_ID);

            if (webResourceItem == null ||
                    !WEB_RESOURCE_NAMED_PIPELINE_NAME.equals(webResourceItem.getNamedPipeline())){
                log.warn("Cannot create webresource link for site '{}' because it does not have a sitemap " +
                        "that contains a sitemap item with properties '{} = {}' and '{} = {}'",
                        hstSite.getConfigurationPath(), HstNodeTypes.SITEMAPITEM_PROPERTY_REF_ID, WEB_RESOURCES_SITEMAP_ITEM_ID,
                        HstNodeTypes.SITEMAPITEM_PROPERTY_NAMEDPIPELINE, WEB_RESOURCE_NAMED_PIPELINE_NAME);
                return EVAL_PAGE;
            }

            // check whether no wildcards presents in webResourceItem or ancestor
            HstSiteMapItem current = webResourceItem;
            StringBuilder fullWebResourcesPath = new StringBuilder();
            while (current != null) {
                if (current.containsAny() || current.containsWildCard() ||
                        current.isAny() || current.isWildCard()) {
                    log.warn("Cannot create webresource link for site '{}' because the sitemap item " +
                                    "for the webresources '{}' contains wildcards (or one of its parents).",
                            hstSite.getConfigurationPath(), webResourceItem.getQualifiedId());
                    return EVAL_PAGE;
                }
                fullWebResourcesPath.insert(0, current.getValue()).insert(0, "/");
                current = current.getParentItem();
            }

            final String fullWebResourcePath = fullWebResourcesPath.append("/").append(webResourcePath).toString();

            WebResourcesService service = HippoServiceRegistry.getService(WebResourcesService.class);
            if (service == null) {
                log.error("Missing service for '{}'. Cannot create webresource url.", WebResourcesService.class.getName());
                return EVAL_PAGE;
            }
            try {
                final Session session = reqContext.getSession();
                final WebResource webResource = service.getJcrWebResources(session).get(path);

                Content content;
                if (reqContext.isCmsRequest() || reqContext.getResolvedMount().getMount().isPreview()) {
                    content = webResource.getContent();
                } else {
                    content = webResource.getContent(webResource.getLatestRevisionId());
                }

                if (content.getRevisionId() == null) {
                    // include cache busting and make sure visitors cannot guess url of css/js of trunk
                    final String hash = content.getHash();
                    if (hash != null) {
                        parametersMap.put("hash", ImmutableList.of(hash));
                    }
                } else {
                    // possibly a version is already set explicitly through ParamTag. In that case we skip it
                    if (!parametersMap.containsKey("version")) {
                        parametersMap.put("version", ImmutableList.of(content.getRevisionId()));
                    }
                }

            } catch (RepositoryException e) {
                log.error("Exception while trying to retrieve the node path for the edit location", e);
                return EVAL_PAGE;
            } catch (WebResourceException e) {
                if (log.isDebugEnabled()) {
                    log.info("Cannot create resource link '{}'", fullWebResourcePath, e);
                } else {
                    log.info("Cannot create resource link '{}' : cause '{}'", fullWebResourcePath, e.toString());
                }
                return EVAL_PAGE;
            }

            HstLink link = reqContext.getHstLinkCreator().create(fullWebResourcePath, reqContext.getResolvedMount().getMount(), true);
            String urlString = link.toUrlForm(reqContext, false);

            try {
                if (!parametersMap.isEmpty()) {
                    String queryString = getQueryString(reqContext.getBaseURL().getCharacterEncoding(), parametersMap, removedParametersList);
                    urlString += queryString;
                }
            } catch (UnsupportedEncodingException e) {
                throw new JspException("UnsupportedEncodingException on the base url", e);
            }

            writeOrSetVar(urlString, var, pageContext, scope);

            return EVAL_PAGE;
        } finally {
            cleanup();
        }
    }


    @Override
    protected void cleanup() {
        super.cleanup();
        var = null;
        scope = null;
        path = null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#release()
     */
    @Override
    public void release() {
        super.release();
    }


    /**
     * Returns the var property.
     *
     * @return String
     */
    public String getVar() {
        return var;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    /**
     * Sets the var property.
     *
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
