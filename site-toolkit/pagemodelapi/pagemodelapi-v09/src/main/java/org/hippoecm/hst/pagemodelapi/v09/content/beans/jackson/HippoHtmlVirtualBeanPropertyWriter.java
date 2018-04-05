/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.beans.standard.HippoHtmlBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagemodelapi.v09.content.rewriter.HtmlContentRewriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Annotations;

import static org.hippoecm.hst.site.HstServices.getComponentManager;

public class HippoHtmlVirtualBeanPropertyWriter extends AbstractVirtualBeanPropertyWriter<HippoHtmlBean, String> {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(HippoHtmlVirtualBeanPropertyWriter.class);

    public HippoHtmlVirtualBeanPropertyWriter() {
        super();
    }

    protected HippoHtmlVirtualBeanPropertyWriter(BeanPropertyDefinition propDef, Annotations contextAnnotations,
                                                 JavaType type) {
        super(propDef, contextAnnotations, type);
    }

    @Override
    protected String createValue(final HstRequestContext requestContext, final HippoHtmlBean hippoHtmlBean) throws Exception {
        if (requestContext == null) {
            return hippoHtmlBean.getContent();
        }

        final Mount siteMount = getSiteMountForCurrentPageModelApiRequest(requestContext);

        if (siteMount == null) {
            log.info("Cannot do content rewriting properly as there's no proper site mount for this request.");
            return hippoHtmlBean.getContent();
        }

        final HtmlContentRewriter htmlContentRewriter = getComponentManager()
                .getComponent(HtmlContentRewriter.class, "org.hippoecm.hst.pagemodelapi.v09");

        return htmlContentRewriter.rewrite(hippoHtmlBean.getContent(), hippoHtmlBean.getNode(), requestContext,
                siteMount);

    }

    @Override
    public VirtualBeanPropertyWriter createInstanceWithConfig(MapperConfig<?> config, AnnotatedClass declaringClass,
            BeanPropertyDefinition propDef, JavaType type) {
        return new HippoHtmlVirtualBeanPropertyWriter(propDef, declaringClass.getAnnotations(), type);
    }
}
