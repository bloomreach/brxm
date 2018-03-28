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
package org.hippoecm.hst.pagemodelapi.v09.core.container;

import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoHtmlBean;
import org.hippoecm.hst.core.pagemodel.container.MetadataDecorator;
import org.hippoecm.hst.pagemodelapi.v09.core.content.HtmlContentRewriter;
import org.htmlcleaner.HtmlCleaner;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

class HippoBeanModelsSerializerModifier extends BeanSerializerModifier {

    private final List<MetadataDecorator> metadataDecorators;
    private HtmlContentRewriter htmlContentRewriter;

    public HippoBeanModelsSerializerModifier(final List<MetadataDecorator> metadataDecorators, final HtmlCleaner htmlCleaner) {
        this.metadataDecorators = metadataDecorators;

        // htmlCleaner is thread-safe, see http://htmlcleaner.sourceforge.net/release.php  thus is single instance is ok
        htmlContentRewriter = new HtmlContentRewriter(htmlCleaner);
    }

    @SuppressWarnings("unchecked")
    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
            JsonSerializer<?> serializer) {
        final Class<?> beanClazz = beanDesc.getBeanClass();

        if (beanClazz != null) {
            if (HippoHtmlBean.class.isAssignableFrom(beanClazz)) {
                // Custom JsonSerializer for HippoHtmlBean type since we need content rewriting for links
                return new HippoHtmlBeanSerializer(htmlContentRewriter);
            } else if (HippoBean.class.isAssignableFrom(beanClazz)) {
                // Customize JsonSerializer for HippoBean type as we need to accumulate HippoBeans and add JSON Pointer
                // references in the first phase, and later serialize the HippoBeans in content section in the end.
                return new HippoBeanSerializer((JsonSerializer<Object>) serializer, metadataDecorators);
            }
        }

        return serializer;
    }

}
