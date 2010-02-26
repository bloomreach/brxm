/*
 *  Copyright 2008 Hippo.
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

import java.util.HashSet;
import java.util.Properties;

import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.util.StringValueResolver;

/**
 * OverridingPropertyPlaceholderConfigurer
 * 
 * @version $Id$
 */
public class OverridingByAttributesPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {
    
    public static final String IGNORE_UNRESOLVABLE_PLACEHOLDERS_ATTRIBUTE = OverridingByAttributesPropertyPlaceholderConfigurer.class.getName() + ".ignoreUnresolvablePlaceholders";
    
    private boolean ignoreUnresolvablePlaceholders = false;

    private String nullValue;

    private String beanName;

    private BeanFactory beanFactory;

    public OverridingByAttributesPropertyPlaceholderConfigurer() {
        super();
    }

    @Override
    public void setIgnoreUnresolvablePlaceholders(boolean ignoreUnresolvablePlaceholders) {
        super.setIgnoreUnresolvablePlaceholders(ignoreUnresolvablePlaceholders);
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
    }

    @Override
    public void setNullValue(String nullValue) {
        super.setNullValue(nullValue);
        this.nullValue = nullValue;
    }

    @Override
    public void setBeanName(String beanName) {
        super.setBeanName(beanName);
        this.beanName = beanName;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory);
        this.beanFactory = beanFactory;
    }

    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props)
            throws BeansException {

        StringValueResolver valueResolver = new PlaceholderResolvingStringValueResolver(props);
        BeanDefinitionVisitor visitor = new BeanDefinitionVisitor(valueResolver);

        String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
        for (int i = 0; i < beanNames.length; i++) {
            // Check that we're not parsing our own bean definition,
            // to avoid failing on unresolvable placeholders in properties file locations.
            if (!(beanNames[i].equals(this.beanName) && beanFactoryToProcess.equals(this.beanFactory))) {
                BeanDefinition bd = beanFactoryToProcess.getBeanDefinition(beanNames[i]);
                boolean switchingIgnoreUnresolvablePlaceholders = false;
                
                try {
                    Boolean overridenIgnoreUnresolvablePlaceholders = BooleanUtils.toBooleanObject((String) bd.getAttribute(IGNORE_UNRESOLVABLE_PLACEHOLDERS_ATTRIBUTE));
                    switchingIgnoreUnresolvablePlaceholders = (overridenIgnoreUnresolvablePlaceholders != null && overridenIgnoreUnresolvablePlaceholders.booleanValue() != ignoreUnresolvablePlaceholders);
                    if (switchingIgnoreUnresolvablePlaceholders) {
                        super.setIgnoreUnresolvablePlaceholders(overridenIgnoreUnresolvablePlaceholders.booleanValue());
                    }
                    
                    visitor.visitBeanDefinition(bd);
                } catch (BeanDefinitionStoreException ex) {
                    throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanNames[i], ex.getMessage());
                } finally {
                    if (switchingIgnoreUnresolvablePlaceholders) {
                        super.setIgnoreUnresolvablePlaceholders(ignoreUnresolvablePlaceholders);
                    }
                }
            }
        }

        // New in Spring 2.5: resolve placeholders in alias target names and aliases as well.
        beanFactoryToProcess.resolveAliases(valueResolver);
    }

    /**
     * BeanDefinitionVisitor that resolves placeholders in String values,
     * delegating to the <code>parseStringValue</code> method of the
     * containing class.
     */
    private class PlaceholderResolvingStringValueResolver implements StringValueResolver {

        private final Properties props;

        public PlaceholderResolvingStringValueResolver(Properties props) {
            this.props = props;
        }

        public String resolveStringValue(String strVal) throws BeansException {
            String value = parseStringValue(strVal, this.props, new HashSet());
            return (value.equals(nullValue) ? null : value);
        }
    }

}
