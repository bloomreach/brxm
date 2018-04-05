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

import java.math.BigDecimal;
import java.util.Calendar;

import org.hippoecm.hst.content.beans.standard.HippoGalleryImageBean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface HippoGalleryImageBeanMixin extends HippoBeanMixin, HippoGalleryImageBean {

    @JsonIgnore
    @Override
    String getRepresentationId();

    @JsonIgnore
    @Override
    String getName();

    @JsonIgnore
    @Override
    String getDisplayName();

    @JsonProperty
    @Override
    String getMimeType();

    @JsonProperty
    @Override
    String getFilename();

    @JsonProperty
    @Override
    long getLength();

    @JsonIgnore
    @Override
    BigDecimal getLengthKB();

    @JsonIgnore
    @Override
    BigDecimal getLengthMB();

    @JsonProperty
    @Override
    Calendar getLastModified();

    @JsonIgnore
    @Override
    boolean isBlank();

}
