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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.sitemenu.CommonMenuItem;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Annotations;

import static org.hippoecm.hst.core.container.ContainerConstants.LINK_NAME_SITE;

public class MenuItemLinksVirtualBeanPropertyWriter extends AbstractLinksVirtualBeanPropertyWriter<CommonMenuItem> {

    private static final long serialVersionUID = 1L;

    public MenuItemLinksVirtualBeanPropertyWriter() {
        super();
    }

    protected MenuItemLinksVirtualBeanPropertyWriter(BeanPropertyDefinition propDef, Annotations contextAnnotations,
                                                    JavaType type) {
        super(propDef, contextAnnotations, type);
    }

    @Override
    public VirtualBeanPropertyWriter createInstanceWithConfig(MapperConfig<?> config, AnnotatedClass declaringClass,
            BeanPropertyDefinition propDef, JavaType type) {
        return new MenuItemLinksVirtualBeanPropertyWriter(propDef, declaringClass.getAnnotations(), type);
    }

    @Override
    protected Map<String, LinkModel> createLinksMap(final HstRequestContext requestContext, final CommonMenuItem menuItem) throws Exception {
        final Map<String, LinkModel> linksMap = new LinkedHashMap<>();

        final HstLink menuItemLink = menuItem.getHstLink();

        if (menuItemLink == null || menuItemLink.isNotFound() || menuItemLink.getMount() == null) {
            if (StringUtils.isNotBlank(menuItem.getExternalLink())) {
                linksMap.put(LINK_NAME_SITE, new LinkModel(menuItem.getExternalLink(), "external"));
            }
            return linksMap;
        }

        final LinkModel linkModel = LinkModel.convert(menuItemLink, requestContext);
        linksMap.put(LINK_NAME_SITE, linkModel);
        return linksMap;
    }
}
