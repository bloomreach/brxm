/*
 *  Copyright 2014-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.internal.ConfigurationLockInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.channelmanager.ChannelManagerConstants;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.sitemenu.CommonMenu;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_LOCKED_BY;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_LOCKED_BY_CURRENT_USER;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_LOCKED_ON;
import static org.hippoecm.hst.core.container.ContainerConstants.CMS_REQUEST_USER_ID_ATTR;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_WEBMASTER_PRIVILEGE_NAME;
import static org.hippoecm.hst.util.JcrSessionUtils.isInRole;
import static org.hippoecm.hst.utils.TagUtils.encloseInHTMLComment;
import static org.hippoecm.hst.utils.TagUtils.toJSONMap;

public class HstCmsEditMenuTag extends TagSupport {

    private final static Logger log = LoggerFactory.getLogger(HstCmsEditMenuTag.class);

    private final static long serialVersionUID = 1L;

    private final static String MENU_ATTR_NAME = "menu";

    protected CommonMenu menu;

    @Override
    public int doStartTag() throws JspException {
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        try {

            if (menu == null) {
                log.warn("Cannot create a cms edit menu because no menu present");
                return EVAL_PAGE;
            }

            HstRequestContext requestContext = RequestContextProvider.get();

            if (requestContext == null) {
                log.warn("Cannot create a cms edit menu outside the hst request processing");
                return EVAL_PAGE;
            }

            if (!requestContext.isChannelManagerPreviewRequest()) {
                log.debug("Skipping cms edit menu because not cms preview.");
                return EVAL_PAGE;
            }


            final HstSite hstSite = requestContext.getResolvedMount().getMount().getHstSite();
            if (hstSite == null) {
                log.debug("Skipping cms edit menu because no hst site for matched mount '{}'.",
                        requestContext.getResolvedMount().getMount().toString());
                return EVAL_PAGE;
            }

            final Channel channel = hstSite.getChannel();
            if (channel != null && channel.isConfigurationLocked()) {
                log.debug("Channel '{}' is locked", channel.getName());
                return EVAL_PAGE;
            }

            final HstSiteMenuConfiguration siteMenuConfiguration = hstSite.getSiteMenusConfiguration().getSiteMenuConfiguration(menu.getName());

            if (siteMenuConfiguration == null) {
                log.debug("Skipping cms edit menu because no siteMenuConfiguration '{}' found for matched mount '{}'.",
                        menu.getName(), requestContext.getResolvedMount().getMount().toString());
                return EVAL_PAGE;
            }

            if (!(siteMenuConfiguration instanceof CanonicalInfo)) {
                log.debug("Skipping cms edit menu because siteMenuConfiguration found not instanceof CanonicalInfo " +
                        "for matched mount '{}'.", requestContext.getResolvedMount().getMount().toString());
                return EVAL_PAGE;
            }
            if (!(siteMenuConfiguration instanceof ConfigurationLockInfo)) {
                log.debug("Skipping cms edit menu because siteMenuConfiguration found not instanceof ConfigurationLockInfo " +
                        "for matched mount '{}'.", requestContext.getResolvedMount().getMount().toString());
                return EVAL_PAGE;
            }
            final CanonicalInfo canonicalInfo = (CanonicalInfo) siteMenuConfiguration;
            if (!canonicalInfo.isWorkspaceConfiguration()) {
                log.debug("Skipping cms edit menu because siteMenuConfiguration found not part of workspace " +
                        "for matched mount '{}'.", requestContext.getResolvedMount().getMount().toString());
                return EVAL_PAGE;
            }
            if (!canonicalInfo.getCanonicalPath().startsWith(hstSite.getConfigurationPath() + "/")) {
                log.debug("Skipping cms edit menu because siteMenuConfiguration found is inherited from other configuration " +
                        "for matched mount '{}'.", requestContext.getResolvedMount().getMount().toString());
                return EVAL_PAGE;
            }

            final HippoSession cmsUser = (HippoSession) requestContext.getAttribute(ContainerConstants.CMS_USER_SESSION_ATTR_NAME);
            if (cmsUser == null) {
                log.warn("For Channel Manager preview requests there is expected to be a CMS user Session available");
                return EVAL_PAGE;
            }

            final boolean inRole = isInRole(cmsUser, canonicalInfo.getCanonicalPath(), CHANNEL_WEBMASTER_PRIVILEGE_NAME);

            if (!inRole) {
                log.debug("Cms User '{}' does not have required role '{}' to modify menu '{}'", cmsUser.getUserID(),
                        CHANNEL_WEBMASTER_PRIVILEGE_NAME, canonicalInfo.getCanonicalPath());
                return EVAL_PAGE;
            }

            try {
               write(siteMenuConfiguration);
            } catch (IOException ioe) {
                throw new JspException("ResourceURL-Tag Exception: cannot write to the output writer.");
            }
            return EVAL_PAGE;
        } finally {
            cleanup();
        }
    }

    protected void cleanup() {
        menu = null;
    }

    private void write(final HstSiteMenuConfiguration siteMenuConfiguration) throws IOException {
        JspWriter writer = pageContext.getOut();
        final String comment = encloseInHTMLComment(toJSONMap(getAttributeMap(siteMenuConfiguration)));
        writer.print(comment);
    }

    private Map<?, ?> getAttributeMap(final HstSiteMenuConfiguration siteMenuConfiguration) {
        final String canonicalIdentifier = ((CanonicalInfo) siteMenuConfiguration).getCanonicalIdentifier();
        final Map<String, Object> result = new HashMap<>();
        result.put(ChannelManagerConstants.HST_TYPE, "EDIT_MENU_LINK");
        result.put("uuid", canonicalIdentifier);
        final String lockedBy = ((ConfigurationLockInfo)siteMenuConfiguration).getLockedBy();
        if (lockedBy != null) {
            result.put(HST_LOCKED_BY, lockedBy);
            result.put(HST_LOCKED_BY_CURRENT_USER, lockedBy.equals(getCurrentCmsUser()));
            result.put(HST_LOCKED_ON, ((ConfigurationLockInfo)siteMenuConfiguration).getLockedOn().getTimeInMillis());
        }
        return result;
    }


    private static String getCurrentCmsUser() {
        return (String)RequestContextProvider.get().getServletRequest().getAttribute(CMS_REQUEST_USER_ID_ATTR);
    }

    public CommonMenu getMenu() {
        return menu;
    }

    public void setMenu(CommonMenu menu) {
        this.menu = menu;
    }

    /* -------------------------------------------------------------------*/

    public static class TEI extends TagExtraInfo {

        public VariableInfo[] getVariableInfo(TagData tagData) {
            VariableInfo vi[] = new VariableInfo[1];
            vi[0] = new VariableInfo(MENU_ATTR_NAME, "org.hippoecm.hst.core.sitemenu.CommonMenu", true,
                    VariableInfo.AT_BEGIN);

            return vi;
        }

    }
}





   
