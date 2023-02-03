/*
 *  Copyright 2017-2023 Bloomreach
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
package org.hippoecm.hst.site.container;

import org.springframework.beans.factory.config.MethodInvokingFactoryBean;

/**
 * HSTTWO-3953: Type pre-determined method invoking factory bean.
 */
public class TypeDeterminedMethodInvokingFactoryBean extends MethodInvokingFactoryBean {

    /**
     * Pre-determined object type. If null, fall back to super.getObjectType().
     */
    private final Class<?> objectType;

    public TypeDeterminedMethodInvokingFactoryBean(final Class<?> objectType) {
        super();
        this.objectType = objectType;
    }

    /**
     * {@inheritDoc}
     * <P>
     * Returns pre-determined object type. If null, fall back to super.getObjectType().
     * </P>
     */
    @Override
    public Class<?> getObjectType() {
        if (objectType != null) {
            return objectType;
        }

        return super.getObjectType();
    }
}