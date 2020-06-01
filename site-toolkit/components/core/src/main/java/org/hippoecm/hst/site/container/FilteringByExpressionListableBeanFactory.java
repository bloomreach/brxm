/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlHelper;
import org.apache.commons.jexl.Script;
import org.apache.commons.jexl.ScriptFactory;
import org.apache.commons.lang.BooleanUtils;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class FilteringByExpressionListableBeanFactory extends DefaultListableBeanFactory
{
    private static Logger log = LoggerFactory.getLogger(FilteringByExpressionListableBeanFactory.class);
    
    private JexlContext jexlContext;
    
    @SuppressWarnings("unchecked")
    public FilteringByExpressionListableBeanFactory(BeanFactory parentBeanFactory, ContainerConfiguration containerConfiguration)
    {
        super(parentBeanFactory);
        jexlContext = JexlHelper.createContext();
        jexlContext.getVars().put("sys", System.class);
        jexlContext.getVars().put("config", containerConfiguration);
    }

    /**
     * Override of the registerBeanDefinition method to optionally filter out a BeanDefinition and
     * if requested dynamically register an bean alias
     */
    public void registerBeanDefinition(String beanName, BeanDefinition bd)
            throws BeanDefinitionStoreException
    {
        boolean registrable = true;
        String expression = (String) bd.getAttribute(SpringComponentManager.BEAN_REGISTER_CONDITION);
        
        if (expression != null) {
            try {
                Script jexlScript = ScriptFactory.createScript(expression);
                Object result = jexlScript.execute(jexlContext);
                
                if (result == null) {
                    registrable = false;
                } else if (result instanceof Boolean) {
                    registrable = BooleanUtils.toBoolean((Boolean) result);
                } else if (result instanceof String) {
                    registrable = BooleanUtils.toBoolean((String) result);
                } else if (result instanceof Integer) {
                    registrable = BooleanUtils.toBoolean(((Integer) result).intValue());
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Expression execution error: " + expression, e);
                } else {
                    log.warn("Expression execution error: {}. {}", expression, e);
                }
            }
        }
        
        if (registrable) {
            super.registerBeanDefinition(beanName, bd);
        } else {
            log.debug("Skipping the bean definition: " + bd);
        }
    }
}
