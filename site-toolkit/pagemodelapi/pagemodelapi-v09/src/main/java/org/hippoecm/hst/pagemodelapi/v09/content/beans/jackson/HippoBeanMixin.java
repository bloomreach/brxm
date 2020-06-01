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

import java.util.Map;

import javax.jcr.Node;

import org.hippoecm.hst.content.beans.standard.HippoAvailableTranslationsBean;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface HippoBeanMixin extends HippoBean {

    @JsonProperty("id")
    String getRepresentationId();

    @Override
    @JsonIgnore
    String getIdentifier();

    @Override
    @JsonIgnore
    Node getNode();

    @Override
    @JsonIgnore
    JCRValueProvider getValueProvider();

    @Override
    @JsonIgnore
    String getPath();

    @Override
    @JsonIgnore
    Map<String, Object> getProperties();

    @Override
    @JsonIgnore
    Map<String, Object> getProperty();

    @Override
    @JsonIgnore
    HippoBean getParentBean();

    @Override
    @JsonIgnore
    <T extends HippoBean> T getCanonicalBean();

    @Override
    @JsonIgnore
    String getCanonicalPath();

    @Override
    @JsonIgnore
    String getCanonicalUUID();

    @Override
    @JsonIgnore
    boolean isHippoDocumentBean();

    @Override
    @JsonIgnore
    boolean isHippoFolderBean();

    @Override
    @JsonIgnore
    boolean isLeaf();

    @Override
    @JsonIgnore
    <T extends HippoBean> HippoAvailableTranslationsBean<T> getAvailableTranslations();

    @Override
    @JsonIgnore
    Map<Object, Object> getEqualComparator();

    @Override
    @JsonIgnore
    String getComparePath();
}
