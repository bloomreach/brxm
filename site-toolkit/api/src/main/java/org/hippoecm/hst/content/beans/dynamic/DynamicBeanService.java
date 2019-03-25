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

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.onehippo.cms7.services.contenttype.ContentType;

/**
 * Contains services for dynamic bean generation to regenerate the beans on the fly
 * after a document type update is done in the jackrabbit.
 *
 */
public interface DynamicBeanService {

    /**
     * If a document type is modified, removes initialized dynamic beans of all
     * document types from {@link ObjectConverter} for the regeneration of the beans.
     */
    void invalidateDynamicBeans();

    /**
     * Creates dynamic beans for compound document types
     * 
     * @param namespace of the document type
     * @param contentType of the document type which is read from contentTypeService
     * @returns created bean
     */
    Class<? extends HippoBean> createDynamicCompoundBean(final String namespace, final ContentType contentType);

    /**
     * Creates dynamic beans for standard document types
     * 
     * @param parentBean if the bean is inherited from another bean this paramer is used to describe the parent bean
     * @param namespace of the document type
     * @param contentType of the document type which is read from contentTypeService
     * @return
     */
    Class<? extends HippoBean> createDynamicDocumentBean(final Class<? extends HippoBean> parentBean,
            final String namespace, final ContentType contentType);

}
