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

import java.util.LinkedHashMap;
import java.util.Map;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageBean;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Annotations;

import static org.hippoecm.hst.core.container.ContainerConstants.LINK_NAME_SITE;

public class HippoGalleryImageBeanLinksVirtualBeanPropertyWriter extends AbstractLinksVirtualBeanPropertyWriter<HippoGalleryImageBean> {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(HippoGalleryImageBeanLinksVirtualBeanPropertyWriter.class);

    public HippoGalleryImageBeanLinksVirtualBeanPropertyWriter() {
        super();
    }

    protected HippoGalleryImageBeanLinksVirtualBeanPropertyWriter(BeanPropertyDefinition propDef,
            Annotations contextAnnotations, JavaType type) {
        super(propDef, contextAnnotations, type);
    }

    @Override
    public VirtualBeanPropertyWriter createInstanceWithConfig(MapperConfig<?> config, AnnotatedClass declaringClass,
            BeanPropertyDefinition propDef, JavaType type) {
        return new HippoGalleryImageBeanLinksVirtualBeanPropertyWriter(propDef, declaringClass.getAnnotations(), type);
    }

    @Override
    protected Map<String, LinkModel> createLinksMap(final HstRequestContext requestContext,
            final HippoGalleryImageBean imageBean) throws Exception {
        final Mount siteMount = getSiteMountForCurrentPageModelApiRequest(requestContext);

        if (siteMount == null) {
            log.info("Cannot add linkes properly as there's no proper site mount for this request.");
            return null;
        }

        final Map<String, LinkModel> linksMap = new LinkedHashMap<>();
        final HstLink imageHstLink = requestContext.getHstLinkCreator().create(imageBean.getNode(), siteMount);
        final LinkModel linkModel = LinkModel.convert(imageHstLink, requestContext);
        linksMap.put(LINK_NAME_SITE, linkModel);

        return linksMap;
    }
}
