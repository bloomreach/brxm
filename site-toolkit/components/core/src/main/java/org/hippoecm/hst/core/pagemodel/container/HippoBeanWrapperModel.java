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
package org.hippoecm.hst.core.pagemodel.container;

import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.pagemodel.model.IdentifiableLinkableMetadataBaseModel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * HippoBean Wrapper Model to include properties of the unwrapped {@code bean} as well as links and metadata.
 */
class HippoBeanWrapperModel extends IdentifiableLinkableMetadataBaseModel {

    static final String HIPPO_BEAN_PROP = "bean";

    private final HippoBean bean;
    private final int hashValue;

    public HippoBeanWrapperModel(final String id, final HippoBean bean) {
        super(id);

        if (bean == null) {
            throw new IllegalArgumentException("bean must not be null.");
        }

        this.bean = bean;
        hashValue = new HashCodeBuilder().append(id).append(bean.getPath()).toHashCode();
    }

    //@JsonUnwrapped not working as HippoBeanSerializer does a custom serialization.
    @JsonProperty(HIPPO_BEAN_PROP)
    public HippoBean getBean() {
        return bean;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof HippoBeanWrapperModel)) {
            return false;
        }

        HippoBeanWrapperModel that = (HippoBeanWrapperModel) o;

        return StringUtils.equals(getId(), that.getId()) && Objects.equals(bean, that.bean);
    }

    @Override
    public int hashCode() {
        return hashValue;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", getId()).append("bean", bean).toString();
    }
}
