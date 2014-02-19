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
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_CONTAINER_COMPONENT_LOCKED_BY;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_CONTAINER_COMPONENT_LOCKED_BY_CURRENT_USER;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_CONTAINER_COMPONENT_LOCKED_ON;
import static org.hippoecm.hst.utils.TagUtils.encloseInHTMLComment;
import static org.hippoecm.hst.utils.TagUtils.toJSONMap;

public class HstCmsEditMenuTag extends TagSupport {

    private final static Logger log = LoggerFactory.getLogger(HstCmsEditMenuTag.class);

    private final static long serialVersionUID = 1L;

    private final static String MENU_ATTR_NAME = "menu";

    protected HstSiteMenu menu;

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

            if (!requestContext.isCmsRequest()) {
                log.debug("Skipping cms edit menu because not cms preview.");
                return EVAL_PAGE;
            }


            final HstSite hstSite = requestContext.getResolvedMount().getMount().getHstSite();
            if (hstSite == null) {
                log.debug("Skipping cms edit menu because no hst site for matched mount '{}'.",
                        requestContext.getResolvedMount().getMount().toString());
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
            CanonicalInfo canonicalInfo = (CanonicalInfo) siteMenuConfiguration;
            if (!canonicalInfo.isWorkspaceConfiguration()) {
                log.debug("Skipping cms edit menu because siteMenuConfiguration found not part of workspace " +
                        "for matched mount '{}'.", requestContext.getResolvedMount().getMount().toString());
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

    private void write(HstSiteMenuConfiguration siteMenuConfiguration) throws IOException {
        JspWriter writer = pageContext.getOut();
        final String comment = encloseInHTMLComment(toJSONMap(getAttributeMap(siteMenuConfiguration)));
        writer.print(comment);
    }

    private Map<?, ?> getAttributeMap(final HstSiteMenuConfiguration siteMenuConfiguration) {
        final String canonicalIdentifier = ((CanonicalInfo) siteMenuConfiguration).getCanonicalIdentifier();
        return new HashMap<String, Object>() {
            {
                put("type", "menu");
                put("uuid", canonicalIdentifier);
                final String lockedBy = siteMenuConfiguration.getLockedBy();
                if (lockedBy != null) {
                    put(HST_CONTAINER_COMPONENT_LOCKED_BY, lockedBy);
                    put(HST_CONTAINER_COMPONENT_LOCKED_BY_CURRENT_USER, lockedBy.equals(RequestContextProvider.get().getCmsUserID()));
                    put(HST_CONTAINER_COMPONENT_LOCKED_ON, siteMenuConfiguration.getLockedOn().getTimeInMillis());
                }
            }
        };
    }


    public HstSiteMenu getMenu() {
        return menu;
    }

    public void setMenu(HstSiteMenu menu) {
        this.menu = menu;
    }

    /* -------------------------------------------------------------------*/

    public static class TEI extends TagExtraInfo {

        public VariableInfo[] getVariableInfo(TagData tagData) {
            VariableInfo vi[] = new VariableInfo[1];
            vi[0] = new VariableInfo(MENU_ATTR_NAME, "org.hippoecm.hst.core.sitemenu.HstSiteMenu", true,
                    VariableInfo.AT_BEGIN);

            return vi;
        }

    }
}





   
