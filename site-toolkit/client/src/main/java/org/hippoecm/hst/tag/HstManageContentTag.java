/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.channelmanager.ChannelManagerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.utils.TagUtils.encloseInHTMLComment;
import static org.hippoecm.hst.utils.TagUtils.toJSONMap;

/**
 * This tag creates a cms manage content link for a template query or a root path.
 */
public class HstManageContentTag extends TagSupport {

    private static final Logger log = LoggerFactory.getLogger(HstManageContentTag.class);

    private String componentParameter;
    private String defaultPath;
    private HippoBean document;
    private String rootPath;
    private String templateQuery;

    /**
     * @return Tag.EVAL_BODY_INCLUDE
     * @throws JspException should never be thrown
     * @see TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {
        return EVAL_BODY_INCLUDE;
    }

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() throws JspException {
        try {
            final HstRequestContext requestContext = RequestContextProvider.get();

            if (StringUtils.isEmpty(templateQuery) && StringUtils.isEmpty(rootPath)) {
                log.info("Cannot create a manage content url if both template query and root path are empty.");
                return EVAL_PAGE;
            }

            if (requestContext == null) {
                log.warn("Cannot create a manage content url outside the hst request processing for '{}'");
                return EVAL_PAGE;
            }

            if (!requestContext.isCmsRequest()) {
                log.debug("Skipping manage content url because not cms preview.");
                return EVAL_PAGE;
            }

            final Mount mount = requestContext.getResolvedMount().getMount();

            // cmsBaseUrl is something like : http://localhost:8080
            // try to find find the best cms location in case multiple ones are configured
            if (mount.getCmsLocations().isEmpty()) {
                log.warn("Skipping manage content url since no cms locations are configured in hst hostgroup configuration");
                return EVAL_PAGE;
            }

            try {
                write();
            } catch (final IOException ignore) {
                throw new JspException("ResourceURL-Tag Exception: cannot write to the output writer.");
            }
            return EVAL_PAGE;
        } finally {
            cleanup();
        }
    }

    protected void cleanup() {
        templateQuery = null;
        rootPath = null;
        defaultPath = null;
        componentParameter = null;
        document = null;
    }

    private void write() throws IOException {
        final JspWriter writer = pageContext.getOut();
        final String comment = encloseInHTMLComment(toJSONMap(getAttributeMap()));
        writer.print(comment);
    }

    private Map<?, ?> getAttributeMap() {
        final Map<String, Object> result = new HashMap<>();
        result.put(ChannelManagerConstants.HST_TYPE, "CONTENT_LINK");
        result.put("templateQuery", templateQuery);
        result.put("rootPath", rootPath);
        result.put("defaultPath", defaultPath);
        result.put("componentParameter", componentParameter);
        return result;
    }

    public void setDocument(final HippoBean document) {
        this.document = document;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
    }

    public String getTemplateQuery() {
        return templateQuery;
    }

    public void setTemplateQuery(final String templateQuery) {
        this.templateQuery = templateQuery;
    }

}





   
