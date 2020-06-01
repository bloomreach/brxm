/*
 *  Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.v09.core.container;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.hippoecm.hst.content.beans.standard.HippoHtmlBean;
import org.hippoecm.hst.content.beans.standard.HippoResourceBean;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.sitemenu.EditableMenuItem;
import org.hippoecm.hst.core.sitemenu.HstSiteMenuItem;
import org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson.DefaultJsonIgnoreTypeMixin;
import org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson.EditableMenuItemMixin;
import org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson.HippoHtmlBeanMixin;
import org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson.HippoResourceBeanMixin;
import org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson.HstLinkMixin;
import org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson.HstSiteMenuItemMixin;
import org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson.HstURLMixin;

/**
 * {@link ObjectMapper} decorator with mixins, supposed to be configured in an Spring bean assembly.
 */
class HstBeansObjectMapperDecorator {

    private static final Map<Class<?>, Class<?>> defaultMixins = new LinkedHashMap<>();

    static {
        defaultMixins.put(Session.class, DefaultJsonIgnoreTypeMixin.class);
        defaultMixins.put(Node.class, DefaultJsonIgnoreTypeMixin.class);

        defaultMixins.put(HippoResourceBean.class, HippoResourceBeanMixin.class);
        defaultMixins.put(HippoHtmlBean.class, HippoHtmlBeanMixin.class);

        defaultMixins.put(HstURL.class, HstURLMixin.class);
        defaultMixins.put(HstLink.class, HstLinkMixin.class);
        defaultMixins.put(HstSiteMenuItem.class, HstSiteMenuItemMixin.class);
        defaultMixins.put(EditableMenuItem.class, EditableMenuItemMixin.class);
    }

    private HstBeansObjectMapperDecorator() {
    }

    static void decorate(final ObjectMapper objectMapper, final Map<Class<?>, Class<?>> extraMixins) {
        if (defaultMixins != null) {
            defaultMixins.forEach((clazz, mixin) -> {
                objectMapper.addMixIn(clazz, mixin);
            });
        }

        if (extraMixins != null) {
            extraMixins.forEach((clazz, mixin) -> {
                objectMapper.addMixIn(clazz, mixin);
            });
        }
    }
}
