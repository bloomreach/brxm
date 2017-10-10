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
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.channelmanager.ChannelManagerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.utils.TagUtils.encloseInHTMLComment;
import static org.hippoecm.hst.utils.TagUtils.toJSONMap;

/**
 * This tag creates a manage content button in the channel manager.
 */
public class HstManageContentTag extends TagSupport {

    private static final Logger log = LoggerFactory.getLogger(HstManageContentTag.class);

    private String componentParameter;
    private String defaultPath;
    private HippoBean document;
    private String rootPath;
    private String templateQuery;

    @Override
    public int doStartTag() throws JspException {
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            final HstRequestContext requestContext = RequestContextProvider.get();

            if (requestContext == null) {
                log.warn("Cannot create a manage content button outside the hst request.");
                return EVAL_PAGE;
            }

            if (!requestContext.isCmsRequest()) {
                log.debug("Skipping manage content tag because not in cms preview.");
                return EVAL_PAGE;
            }

            try {
                write();
            } catch (final IOException ignore) {
                throw new JspException("Manage content tag exception: cannot write to the output writer.");
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
        writeToMap(result, ChannelManagerConstants.HST_TYPE, "MANAGE_CONTENT_LINK");
        writeToMap(result, "templateQuery", templateQuery);
        writeToMap(result, "rootPath", rootPath);
        writeToMap(result, "defaultPath", defaultPath);
        writeToMap(result, "componentParameter", componentParameter);
        return result;
    }

    private static void writeToMap(final Map<String, Object> result, final String key, final String value) {
        if (StringUtils.isNotEmpty(value)) {
            result.put(key, value);
        }
    }

    public void setComponentParameter(final String componentParameter) {
        this.componentParameter = componentParameter;
    }

    public void setDefaultPath(final String defaultPath) {
        this.defaultPath = defaultPath;
    }

    public void setDocument(final HippoBean document) {
        this.document = document;
    }

    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
    }

    public void setTemplateQuery(final String templateQuery) {
        this.templateQuery = templateQuery;
    }

}






