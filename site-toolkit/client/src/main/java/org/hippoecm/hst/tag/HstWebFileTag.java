/*
 *  Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.hst.util.WebFileUtils;
import org.hippoecm.hst.utils.TagUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.webfiles.WebFileBundle;
import org.onehippo.cms7.services.webfiles.WebFileException;
import org.onehippo.cms7.services.webfiles.WebFilesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.utils.TagUtils.createPathInfoWithoutRequestContext;
import static org.hippoecm.hst.utils.TagUtils.getQueryString;
import static org.hippoecm.hst.utils.TagUtils.writeOrSetVar;

/**
 * Abstract supporting class for Hst Link tags
 */

public class HstWebFileTag extends ParamContainerTag {

    private final static Logger log = LoggerFactory.getLogger(HstWebFileTag.class);

    private final static String WEB_FILES_SITEMAP_ITEM_ID = "WEB-FILES-ID";
    private final static String WEB_FILE_NAMED_PIPELINE_NAME = "WebFilePipeline";

    private static final long serialVersionUID = 1L;

    protected String path;

    protected String var;

    protected String scope;

    protected boolean fullyQualified;

    protected Boolean escapeXml = true;

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {

        if (var != null) {
            TagUtils.removeVar(var, pageContext, scope);
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

            final String webFilePath = PathUtils.normalizePath(path);

            final HstSite hstSite;
            if (reqContext == null ||
                    (hstSite = reqContext.getResolvedMount().getMount().getHstSite()) == null ||
                    hstSite.getSiteMap() == null ) {
                log.debug("Although there is no HstRequestContext/HstSite/Sitemap for request, a link for path='{}' is created " +
                        "similar to how the c:url tag would do it", webFilePath);
                String pathInfo = createPathInfoWithoutRequestContext(webFilePath, parametersMap, removedParametersList, servletRequest);
                writeOrSetVar(pathInfo, var, pageContext, scope);
                return EVAL_PAGE;
            }

            final HstSiteMapItem webFileSiteMapItem = hstSite.getSiteMap().getSiteMapItemByRefId(WEB_FILES_SITEMAP_ITEM_ID);

            if (webFileSiteMapItem == null ||
                    !WEB_FILE_NAMED_PIPELINE_NAME.equals(webFileSiteMapItem.getNamedPipeline())){
                log.warn("Cannot create web file link for site '{}' because it does not have a sitemap " +
                                "that contains a sitemap item with properties '{} = {}' and '{} = {}'",
                        hstSite.getConfigurationPath(), HstNodeTypes.SITEMAPITEM_PROPERTY_REF_ID, WEB_FILES_SITEMAP_ITEM_ID,
                        HstNodeTypes.SITEMAPITEM_PROPERTY_NAMEDPIPELINE, WEB_FILE_NAMED_PIPELINE_NAME);
                return EVAL_PAGE;
            }

            // check whether no wildcards presents in webFileSiteMapItem or ancestor
            HstSiteMapItem current = webFileSiteMapItem;
            StringBuilder webFilesPrefix = new StringBuilder("/");
            while (current != null) {
                if (current.containsAny() || current.containsWildCard() ||
                        current.isAny() || current.isWildCard()) {
                    log.warn("Cannot create web file link for site '{}' because the sitemap item " +
                                    "for the web file '{}' contains wildcards (or one of its parents).",
                            hstSite.getConfigurationPath(), webFileSiteMapItem.getQualifiedId());
                    return EVAL_PAGE;
                }
                webFilesPrefix.insert(0, current.getValue()).insert(0, "/");
                current = current.getParentItem();
            }

            WebFilesService service = HippoServiceRegistry.getService(WebFilesService.class);
            if (service == null) {
                log.error("Missing service for '{}'. Cannot create web file url.", WebFilesService.class.getName());
                return EVAL_PAGE;
            }

            final ResolvedMount resolvedMount = reqContext.getResolvedMount();
            try {
                final Session session = reqContext.getSession();

                final String bundleName = WebFileUtils.getBundleName(reqContext);
                try {
                    final WebFileBundle webFileBundle = service.getJcrWebFileBundle(session, bundleName);
                    webFilesPrefix.append(webFileBundle.getAntiCacheValue());
                } catch (WebFileException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Cannot find web file bundle '{}'", bundleName, e);
                    } else {
                        log.info("Cannot find web file bundle '{}' : {}", bundleName, e.toString());
                    }
                    return EVAL_PAGE;
                }
            } catch (RepositoryException e) {
                log.error("Exception while trying to retrieve the node path for the edit location", e);
                return EVAL_PAGE;
            }

            final String fullWebFilePath = webFilesPrefix.append("/").append(webFilePath).toString();
            HstLink link = reqContext.getHstLinkCreator().create(fullWebFilePath, resolvedMount.getMount(), true);
            String urlString = link.toUrlForm(reqContext, fullyQualified);

            try {
                if (!parametersMap.isEmpty()) {
                    String queryString = getQueryString(reqContext.getBaseURL().getURIEncoding(), parametersMap, removedParametersList);
                    urlString += queryString;
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


    @Override
    protected void cleanup() {
        super.cleanup();
        var = null;
        escapeXml = true;
        fullyQualified = false;
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

    public boolean isFullyQualified() {
        return fullyQualified;
    }

    /**
     * Returns escapeXml property.
     * @return Boolean
     */
    public Boolean getEscapeXml() {
        return escapeXml;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setFullyQualified(boolean fullyQualified) {
        this.fullyQualified = fullyQualified;
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

}
