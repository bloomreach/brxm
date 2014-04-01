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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;


public class TestResourceFactoryBean {

    private ClassPathXmlApplicationContext appContext;

    @Before
    public void setUp() throws Exception {
        appContext = new ClassPathXmlApplicationContext("META-INF/assembly/example-resource-factory-beans.xml");
    }

    @Test
    public void testResourceFactoryBean() throws Exception {
        String resourcePath = "classpath:/" + ResourceFactoryBean.class.getName().replace('.', '/') + ".class";

        FactoryBean factoryBean = new ResourceFactoryBean(resourcePath);
        ((ResourceLoaderAware) factoryBean).setResourceLoader(appContext);
        Object bean = factoryBean.getObject();
        assertTrue(bean instanceof Resource);
        assertEquals(Resource.class, factoryBean.getObjectType());

        factoryBean = new ResourceFactoryBean(resourcePath, URL.class);
        ((ResourceLoaderAware) factoryBean).setResourceLoader(appContext);
        bean = factoryBean.getObject();
        assertTrue(bean instanceof URL);
        assertTrue(bean.toString().endsWith("/ResourceFactoryBean.class"));
        assertEquals(URL.class, factoryBean.getObjectType());

        factoryBean = new ResourceFactoryBean(resourcePath, URI.class);
        ((ResourceLoaderAware) factoryBean).setResourceLoader(appContext);
        bean = factoryBean.getObject();
        assertTrue(bean instanceof URI);
        assertTrue(bean.toString().endsWith("/ResourceFactoryBean.class"));
        assertEquals(URI.class, factoryBean.getObjectType());

        factoryBean = new ResourceFactoryBean(resourcePath, String.class);
        ((ResourceLoaderAware) factoryBean).setResourceLoader(appContext);
        bean = factoryBean.getObject();
        assertTrue(bean instanceof String);
        assertTrue(bean.toString().endsWith("/ResourceFactoryBean.class"));
        assertEquals(String.class, factoryBean.getObjectType());

        factoryBean = new ResourceFactoryBean(resourcePath, File.class);
        ((ResourceLoaderAware) factoryBean).setResourceLoader(appContext);
        bean = factoryBean.getObject();
        assertTrue(bean instanceof File);
        assertEquals(((File) bean).getName(), "ResourceFactoryBean.class");
        assertEquals(File.class, factoryBean.getObjectType());
    }

    @Test
    public void testDefaultHstJaasLoginModuleConfigUrlInSpringAssembly() throws Exception {
        Object configUrl = appContext.getBean("defaultHstJaasLoginModuleConfigUrlString");
        assertEquals("java.lang.String", configUrl.getClass().getName());
        assertTrue(configUrl.toString().endsWith("/META-INF/example-login.conf"));

        List<?> resourceBeansList = (List<?>) appContext.getBean("resourceBeansList");
        assertEquals(1, resourceBeansList.size());
        Object configUrl2 = resourceBeansList.get(0);
        assertEquals("java.lang.String", configUrl2.getClass().getName());
        assertTrue(configUrl2.toString().endsWith("/META-INF/example-login.conf"));
    }

    @Test
    public void testFallbackingHstJaasLoginModuleConfigUrlInSpringAssembly() throws Exception {
        Object defaultConfigUrl = appContext.getBean("defaultHstJaasLoginModuleConfigUrlString");
        assertEquals("java.lang.String", defaultConfigUrl.getClass().getName());
        assertTrue(((String) defaultConfigUrl).endsWith("/META-INF/example-login.conf"));

        Object fallbackingConfigUrl = appContext.getBean("fallbackingHstJaasLoginModuleConfigUrlString");
        assertEquals("java.lang.String", fallbackingConfigUrl.getClass().getName());
        assertTrue(fallbackingConfigUrl.toString().endsWith("/META-INF/example-login.conf"));

        assertEquals(defaultConfigUrl, fallbackingConfigUrl);
    }

    @Test
    public void testResourceFactoryBeanFallbackToDefaultObject() throws Exception {
        String resourcePath = "classpath:/some/non/existing/resource/path/really.no";
        String defaultResourceValue = "file:/a/default/path/really.yes";

        ResourceFactoryBean factoryBean = new ResourceFactoryBean(resourcePath, String.class, defaultResourceValue);
        ((ResourceLoaderAware) factoryBean).setResourceLoader(appContext);
        factoryBean.setIgnoreCreationError(true);

        Object bean = factoryBean.getObject();
        assertEquals("java.lang.String", bean.getClass().getName());
        assertEquals(String.class, factoryBean.getObjectType());
        assertEquals(defaultResourceValue, bean);
    }
}
