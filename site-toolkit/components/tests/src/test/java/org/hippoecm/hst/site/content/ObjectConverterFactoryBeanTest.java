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

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.ContentTypesProvider;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.tool.DefaultContentBeansTool;
import org.hippoecm.hst.site.container.SpringMetadataReaderClasspathResourceScanner;
import org.hippoecm.hst.site.content.beans.NewsArticleBean;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.onehippo.cms7.services.contenttype.EffectiveNodeType;
import org.onehippo.cms7.services.contenttype.EffectiveNodeTypes;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.web.MockServletContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        ContentTypes contentType = new ContentTypes() {
            @Override
            public EffectiveNodeTypes getEffectiveNodeTypes() {
                return null;
            }

            @Override
            public long version() {
                return 0;
            }

            @Override
            public ContentType getType(final String name) {
                return null;
            }

            @Override
            public SortedMap<String, Set<ContentType>> getTypesByPrefix() {
                return null;
            }

            @Override
            public ContentType getContentTypeForNode(final Node node) throws RepositoryException {
                return null;
            }

            @Override
            public ContentType getContentTypeForNodeByUuid(final Session session, final String uuid) throws ItemNotFoundException, RepositoryException {
                return null;
            }

            @Override
            public ContentType getContentTypeForNodeByPath(final Session session, final String path) throws PathNotFoundException, RepositoryException {
                return null;
            }
        };
        ObjectConverterFactoryBean factory = new ObjectConverterFactoryBean();
        factory.setContentTypesProvider(new ContentTypesProvider() {
            @Override
            public ContentTypes getContentTypes() {
                return contentType;
            }
        });
        factory.setClasspathResourceScanner(classpathResourceScanner);
        factory.setServletContext(servletContext);
        factory.afterPropertiesSet();
        ObjectConverter converter = factory.getObject();
        assertNotNull(converter);
        assertEquals(NewsArticleBean.class, converter.getClassFor("testsite:newsarticle"));
        assertEquals("testsite:newsarticle", converter.getPrimaryNodeTypeNameFor(NewsArticleBean.class));
    }
}
