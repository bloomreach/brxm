/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.dynamic;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.onehippo.cms7.services.contenttype.ContentType;

/**
 * A service for dynamic bean definition generation to (re)generate document beans on the fly
 * after a document type update is done in the jackrabbit.
 */
public interface DynamicBeanService {

    /**
     * Creates a bean/class definition for given content type
     * 
     * @param parentBeanDef if the bean is inherited from another bean this parameter is used to describe the parent bean
     * @param contentType of the document type which is read from contentTypeService
     * @return Class definition for a given {@link ContentType}
     */
    Class<? extends HippoBean> createDocumentBeanDef(final Class<? extends HippoBean> parentBeanDef, final ContentType contentType);

}
