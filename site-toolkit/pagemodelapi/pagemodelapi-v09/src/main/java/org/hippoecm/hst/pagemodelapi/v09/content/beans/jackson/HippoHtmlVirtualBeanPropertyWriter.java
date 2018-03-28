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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Annotations;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoHtmlBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagemodelapi.v09.content.rewriter.HtmlContentRewriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.container.ContainerConstants.PAGE_MODEL_PIPELINE_NAME;
import static org.hippoecm.hst.site.HstServices.getComponentManager;

public class HippoHtmlVirtualBeanPropertyWriter extends VirtualBeanPropertyWriter {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(HippoHtmlVirtualBeanPropertyWriter.class);

    @SuppressWarnings("unused")
    public HippoHtmlVirtualBeanPropertyWriter() {
        super();
    }

    protected HippoHtmlVirtualBeanPropertyWriter(BeanPropertyDefinition propDef, Annotations contextAnnotations,
                                                 JavaType type) {
        super(propDef, contextAnnotations, type);
    }

    @Override
    protected Object value(Object htmlBean, JsonGenerator gen, SerializerProvider prov) throws Exception {
        final HstRequestContext requestContext = RequestContextProvider.get();


        final HippoHtmlBean hippoHtmlBean = (HippoHtmlBean) htmlBean;

        if (requestContext == null) {
            return hippoHtmlBean.getContent();
        }

        Mount siteMount;
        Mount pageModelApiMount = requestContext.getResolvedMount().getMount();
        if (!PAGE_MODEL_PIPELINE_NAME.equals(pageModelApiMount.getNamedPipeline())) {
            log.warn("Expected request mount have named pipeline '{}' but was '{}'. Cannot do content rewriting properly.",
                    PAGE_MODEL_PIPELINE_NAME, pageModelApiMount.getNamedPipeline());
            return hippoHtmlBean.getContent();
        } else {
            siteMount = pageModelApiMount.getParent();
            if (siteMount == null) {
                log.info("Expected a 'PageModelPipeline' always to be nested below a parent site mount. This is not the " +
                        "case for '{}'. Cannot do content rewriting properly.", pageModelApiMount);
                return hippoHtmlBean.getContent();
            }
        }

        final HtmlContentRewriter htmlContentRewriter = getComponentManager()
                .getComponent(HtmlContentRewriter.class, "org.hippoecm.hst.pagemodelapi.v09");

        return htmlContentRewriter.rewrite(hippoHtmlBean.getContent(), hippoHtmlBean.getNode(), requestContext,
                siteMount);

    }

    @Override
    public VirtualBeanPropertyWriter withConfig(MapperConfig<?> config, AnnotatedClass declaringClass,
            BeanPropertyDefinition propDef, JavaType type) {
        // Ref: jackson-databind-master/src/test/java/com/fasterxml/jackson/databind/ser/TestVirtualProperties.java
        return new HippoHtmlVirtualBeanPropertyWriter(propDef, declaringClass.getAnnotations(), type);
    }
}
