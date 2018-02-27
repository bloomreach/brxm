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

import java.util.Locale;

import org.hippoecm.hst.content.beans.standard.HippoAvailableTranslationsBean;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonAppend;

@JsonAppend(props = {
        @JsonAppend.Prop(
                name = "_links",
                value = HippoBeanLinksVirtualBeanPropertyWriter.class
                )
        }
    )
public interface HippoDocumentBeanMixin extends HippoDocumentBean, HippoBeanMixin {

    @JsonProperty("handlePath")
    @Override
    String getCanonicalHandlePath();

    @JsonIgnore
    @Override
    public <T extends HippoBean> HippoAvailableTranslationsBean<T> getAvailableTranslations(Class<T> beanMappingClass);

    @JsonIgnore
    @Override
    public Locale getLocale();

}
