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
package org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson.jackson;

import java.util.List;
import java.util.Locale;

import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface HippoFolderBeanMixin extends HippoFolderBean, HippoBeanMixin {

    @JsonIgnore
    @Override
    List<HippoFolderBean> getFolders();

    @JsonIgnore
    @Override
    List<HippoDocumentBean> getDocuments();

    @JsonIgnore
    @Override
    Locale getLocale();

}
