/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.standard;

import javax.jcr.Node;

/**
 * Classes that want to use dynamic bean interceptors should extend this class. 
 */
public abstract class DynamicBeanInterceptor {

    protected final String propertyName;
    protected final boolean multiple;
    protected final Node documentTypeNode;

    /**
     * 
     * @param propertyName field name in the document (eg: myproject:author)
     * @param multiple whether the field type is multiple
     * @param documentTypeNode node of the document type (eg: /hippo:namespaces/myproject/newsdocument)
     */
    protected DynamicBeanInterceptor(final String propertyName, final boolean multiple, final Node documentTypeNode) {
        this.propertyName = propertyName;
        this.multiple = multiple;
        this.documentTypeNode = documentTypeNode;
    }

    /**
     * 
     * @return cmsType class definition
     */
    public abstract Class<?> getCmsType();

}
