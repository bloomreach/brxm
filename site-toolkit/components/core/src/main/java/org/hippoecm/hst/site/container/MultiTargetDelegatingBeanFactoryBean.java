/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * MultiTargetDelegatingBeanFactoryBean.
 * <P>
 * This factory bean simply tries to get a bean from the configured {@code targetBeanNames} as ordered
 * to delegate to. The first resolved target bean will be used as delegatee.
 * </P>
 */
public class MultiTargetDelegatingBeanFactoryBean implements FactoryBean<Object>, BeanFactoryAware {

    private String[] targetBeanNames;
    private Class<?> objectType;
    private BeanFactory beanFactory;
    private boolean singleton = true;
    private Object singletonBean;

    public MultiTargetDelegatingBeanFactoryBean(String[] targetBeanNames) {
        this(targetBeanNames, null);
    }

    public MultiTargetDelegatingBeanFactoryBean(String[] targetBeanNames, Class<?> objectType) {
        this.targetBeanNames = targetBeanNames;
        this.objectType = objectType;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object getObject() throws Exception {
        if (singleton) {
            if (singletonBean == null) {
                singletonBean = createInstance();
            }

            return singletonBean;
        } else {
            return createInstance();
        }
    }

    @Override
    public Class<?> getObjectType() {
        return objectType != null ? objectType : Object.class;
    }

    @Override
    public boolean isSingleton() {
        return singleton;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    protected Object createInstance() throws Exception {
        Object bean = null;

        try {
            if (targetBeanNames != null) {
                for (String targetBeanName : targetBeanNames) {
                    try {
                        bean = beanFactory.getBean(targetBeanName);
                    } catch (NoSuchBeanDefinitionException ignore) {
                    }
                }
            }
        } catch (Throwable th) {
            throw new BeanCreationException(
                    "Failed to get a bean from target bean names: " + StringUtils.join(targetBeanNames, ", "), th);
        }

        if (bean == null) {
            throw new BeanCreationException(
                    "No bean found from target bean names: " + StringUtils.join(targetBeanNames, ", "));
        }

        return bean;
    }

}
