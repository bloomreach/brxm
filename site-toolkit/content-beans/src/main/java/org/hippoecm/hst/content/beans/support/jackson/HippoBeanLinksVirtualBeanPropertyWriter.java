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
package org.hippoecm.hst.content.beans.support.jackson;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Annotations;

import static org.hippoecm.hst.core.container.ContainerConstants.LINK_NAME_SELF;
import static org.hippoecm.hst.core.container.ContainerConstants.LINK_NAME_SITE;

public class HippoBeanLinksVirtualBeanPropertyWriter extends VirtualBeanPropertyWriter {

    private static final long serialVersionUID = 1L;
    public static final String PAGE_MODEL_PIPELINE_NAME = "PageModelPipeline";

    private static Logger log = LoggerFactory.getLogger(HippoBeanLinksVirtualBeanPropertyWriter.class);

    @SuppressWarnings("unused")
    public HippoBeanLinksVirtualBeanPropertyWriter() {
        super();
    }

    protected HippoBeanLinksVirtualBeanPropertyWriter(BeanPropertyDefinition propDef, Annotations contextAnnotations,
            JavaType type) {
        super(propDef, contextAnnotations, type);
    }

    @Override
    protected Object value(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
        final HstRequestContext requestContext = RequestContextProvider.get();

        if (requestContext == null) {
            return null;
        }

        final HippoBean hippoBean = (HippoBean) bean;

        if (!hippoBean.isHippoDocumentBean() && !hippoBean.isHippoFolderBean()) {
            return null;
        }

        Map<String, String> linksMap = new LinkedHashMap<>();

        final HstLinkCreator linkCreator = requestContext.getHstLinkCreator();

        final Mount selfMount = requestContext.getResolvedMount().getMount();
        final HstLink selfLink = linkCreator.create(hippoBean.getNode(), selfMount);

        if (selfLink!= null && !selfLink.isNotFound()) {
            linksMap.put(LINK_NAME_SELF, selfLink.toUrlForm(requestContext, false));

            // admittedly a bit of a dirty check to check on PageModelPipeline. Can this be improved?
            if (PAGE_MODEL_PIPELINE_NAME.equals(selfMount.getNamedPipeline())) {
                final Mount siteMount = selfMount.getParent();
                if (siteMount == null) {
                    log.warn("Expected a 'PageModelPipeline' always to be nested below a parent site mount. This is not the " +
                            "case for '{}'. Cannot add site links", selfMount);
                } else {
                    // since the selfLink could be resolved, the site link also must be possible to resolve
                    final HstLink siteLink = linkCreator.create(hippoBean.getNode(), siteMount);
                    if (siteLink!= null && !siteLink.isNotFound()) {
                        linksMap.put(LINK_NAME_SITE, siteLink.toUrlForm(requestContext, false));
                    }
                }
            }

        }

        return linksMap;
    }

    @Override
    public VirtualBeanPropertyWriter withConfig(MapperConfig<?> config, AnnotatedClass declaringClass,
            BeanPropertyDefinition propDef, JavaType type) {
        // Ref: jackson-databind-master/src/test/java/com/fasterxml/jackson/databind/ser/TestVirtualProperties.java
        return new HippoBeanLinksVirtualBeanPropertyWriter(propDef, declaringClass.getAnnotations(), type);
    }
}
