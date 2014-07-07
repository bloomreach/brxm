/*
 * Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.site.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.tool.DefaultContentBeansTool;
import org.hippoecm.hst.site.container.SpringMetadataReaderClasspathResourceScanner;
import org.hippoecm.hst.site.content.beans.NewsArticleBean;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.web.MockServletContext;

/**
 * ObjectConverterFactoryBeanTest
 */
public class ObjectConverterFactoryBeanTest {

    private SpringMetadataReaderClasspathResourceScanner classpathResourceScanner;
    private MockServletContext servletContext;
    private String annotatedClassesInitParam = "classpath*:org/hippoecm/hst/site/content/beans/**/*.class";

    @Before
    public void before() throws Exception {
        classpathResourceScanner = new SpringMetadataReaderClasspathResourceScanner();
        classpathResourceScanner.setResourceLoader(new DefaultResourceLoader());
        servletContext = new MockServletContext();
        servletContext.addInitParameter(DefaultContentBeansTool.BEANS_ANNOTATED_CLASSES_CONF_PARAM, annotatedClassesInitParam);
    }

    @Test
    public void testCreation() throws Exception {
        ObjectConverterFactoryBean factory = new ObjectConverterFactoryBean();
        factory.setClasspathResourceScanner(classpathResourceScanner);
        factory.setServletContext(servletContext);
        factory.afterPropertiesSet();
        ObjectConverter converter = factory.getObject();
        assertNotNull(converter);
        assertEquals(NewsArticleBean.class, converter.getAnnotatedClassFor("testsite:newsarticle"));
        assertEquals("testsite:newsarticle", converter.getPrimaryNodeTypeNameFor(NewsArticleBean.class));
    }
}
