/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagemodelapi.common.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocument;

@Node(jcrType="unittestproject:newspage")
public class NewsBean extends HippoDocument {

    public HippoBean getTextDocument() {
        // hard-coded uuid of text bean to make sure serialization of linked bean also works
        HippoBean beanByUUID = getBeanByUUID("1ca07af9-cdf5-4bdd-9908-f991245bb062", HippoBean.class);
        return beanByUUID;
    }

}
