/**
 * Copyright 2013-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.container;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.tool.ContentBeansTool;
import org.hippoecm.hst.content.tool.DefaultContentBeansTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * ContentBeansResolvingValve
 * <P>
 * This valve sets <code>ContentBeansTool</code> instance in the request for application codes.
 * And, also retrieves a content bean resolved by the sitemap item configuration and set the bean in the request as well.
 * </P>
 */
public class ContentBeansResolvingValve extends AbstractBaseOrderableValve {

    private static Logger log = LoggerFactory.getLogger(ContentBeansResolvingValve.class);

    private static final String CONTENT_BEANS_TOOL_ATTR_NAME = ContentBeansTool.class.getName();

    private ContentBeansTool contentBeansTool = new DefaultContentBeansTool();
    private String resolvedContentBeanAttributeName;

    public ContentBeansTool getContentBeansTool() {
        return contentBeansTool;
    }

    public void setContentBeansTool(ContentBeansTool contentBeansTool) {
        this.contentBeansTool = contentBeansTool;
    }

    public String getResolvedContentBeanAttributeName() {
        return resolvedContentBeanAttributeName;
    }

    public void setResolvedContentBeanAttributeName(String resolvedContentBeanAttributeName) {
        this.resolvedContentBeanAttributeName = resolvedContentBeanAttributeName;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        if (contentBeansTool != null) {
            // In order to allow external applications to get access to the default content beans tool instance.
            context.getServletRequest().setAttribute(CONTENT_BEANS_TOOL_ATTR_NAME, contentBeansTool);

            if (StringUtils.isNotEmpty(resolvedContentBeanAttributeName)) {
                HippoBean resolvedContentBean = contentBeansTool.getResolvedContentBean();

                if (resolvedContentBean != null) {
                    context.getServletRequest().setAttribute(resolvedContentBeanAttributeName, resolvedContentBean);
                    log.debug("ContentBeansResolvingValve sets content bean attribute by name, '{}': {}", resolvedContentBeanAttributeName, resolvedContentBean);
                }
            }
        }

        context.invokeNext();
    }

}
