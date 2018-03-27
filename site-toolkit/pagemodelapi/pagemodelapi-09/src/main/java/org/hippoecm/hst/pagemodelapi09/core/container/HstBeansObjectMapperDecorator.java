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
package org.hippoecm.hst.pagemodelapi09.core.container;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageBean;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;
import org.hippoecm.hst.content.beans.standard.HippoHtmlBean;
import org.hippoecm.hst.content.beans.standard.HippoMirrorBean;
import org.hippoecm.hst.content.beans.standard.HippoRequestBean;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.CommonMenuItemMixin;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.CommonMenuMixin;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.DefaultJsonIgnoreTypeMixin;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.EditableMenuItemMixin;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.EditableMenuMixin;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.HippoBeanMixin;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.HippoDocumentBeanMixin;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.HippoFolderBeanMixin;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.HippoGalleryImageBeanMixin;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.HippoGalleryImageSetBeanMixin;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.HippoHtmlBeanMixin;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.HippoMirrorBeanMixin;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.HippoRequestBeanMixin;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.HstLinkMixin;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.HstSiteMenuItemMixin;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.HstSiteMenuMixin;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.HstURLMixin;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.sitemenu.CommonMenu;
import org.hippoecm.hst.core.sitemenu.CommonMenuItem;
import org.hippoecm.hst.core.sitemenu.EditableMenu;
import org.hippoecm.hst.core.sitemenu.EditableMenuItem;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.hippoecm.hst.core.sitemenu.HstSiteMenuItem;
import org.hippoecm.hst.provider.ValueProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link ObjectMapper} decorator with mixins, supposed to be configured in an Spring bean assembly.
 */
class HstBeansObjectMapperDecorator {

    private static final Map<Class<?>, Class<?>> defaultMixins = new LinkedHashMap<>();

    static {
        defaultMixins.put(Session.class, DefaultJsonIgnoreTypeMixin.class);
        defaultMixins.put(Node.class, DefaultJsonIgnoreTypeMixin.class);

        defaultMixins.put(ValueProvider.class, DefaultJsonIgnoreTypeMixin.class);
        defaultMixins.put(ObjectConverter.class, DefaultJsonIgnoreTypeMixin.class);

        defaultMixins.put(HippoGalleryImageBean.class, HippoGalleryImageBeanMixin.class);
        defaultMixins.put(HippoGalleryImageSetBean.class, HippoGalleryImageSetBeanMixin.class);
        defaultMixins.put(HippoHtmlBean.class, HippoHtmlBeanMixin.class);
        defaultMixins.put(HippoRequestBean.class, HippoRequestBeanMixin.class);
        defaultMixins.put(HippoMirrorBean.class, HippoMirrorBeanMixin.class);
        defaultMixins.put(HippoFolderBean.class, HippoFolderBeanMixin.class);
        defaultMixins.put(HippoDocumentBean.class, HippoDocumentBeanMixin.class);
        defaultMixins.put(HippoBean.class, HippoBeanMixin.class);

        defaultMixins.put(HstURL.class, HstURLMixin.class);
        defaultMixins.put(HstLink.class, HstLinkMixin.class);
        defaultMixins.put(HstSiteMenuItem.class, HstSiteMenuItemMixin.class);
        defaultMixins.put(HstSiteMenu.class, HstSiteMenuMixin.class);
        defaultMixins.put(EditableMenuItem.class, EditableMenuItemMixin.class);
        defaultMixins.put(EditableMenu.class, EditableMenuMixin.class);
        defaultMixins.put(CommonMenuItem.class, CommonMenuItemMixin.class);
        defaultMixins.put(CommonMenu.class, CommonMenuMixin.class);
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
