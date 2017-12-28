/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.services;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.After;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.ContentType;
import org.onehippo.cms7.essentials.dashboard.service.ContentTypeService;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.onehippo.cms7.services.contenttype.EffectiveNodeType;
import org.onehippo.cms7.services.contenttype.EffectiveNodeTypes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@ActiveProfiles("content-beans-test")
@Profile("content-beans-test")
@Configuration
public class ContentTypeServiceImplTest extends BaseRepositoryTest {

    @Inject private ContentTypeService contentTypeService;

    private TestContentTypeService testContentTypeService;
    private ContentTypes contentTypes;

    @Test
    public void base_path_for_content_type() {
        assertEquals("/hippo:namespaces/system/foo", contentTypeService.jcrBasePathForContentType("foo"));
        assertEquals("/hippo:namespaces/namespace/bar", contentTypeService.jcrBasePathForContentType("namespace:bar"));
    }

    @Test
    public void fetch_own_document_types() throws Exception {
        final PluginContext context = getContext();
        setupTestContentTypes();

        final List<ContentType> cts = contentTypeService.fetchContentTypesFromOwnNamespace(context, null);

        assertEquals(4, cts.size());

        assertEquals("ContentType{fullName='testnamespace:compoundWithBean', name='compoundWithBean', prefix='testnamespace', mixin=false, compoundType=true, superTypes=[]}", cts.get(0).toString());
        assertFalse(cts.get(0).isDraftMode());
        assertEquals("Compound.java", cts.get(0).getJavaName());
        assertEquals("/path/to/Compound.java", cts.get(0).getFullPath());
        assertEquals("testnamespace:compoundWithBean", cts.get(0).getDisplayName());

        assertEquals("ContentType{fullName='testnamespace:compoundWithoutBean', name='compoundWithoutBean', prefix='testnamespace', mixin=false, compoundType=true, superTypes=[]}", cts.get(1).toString());
        assertTrue(cts.get(1).isDraftMode());
        assertNull(cts.get(1).getJavaName());
        assertNull(cts.get(1).getFullPath());

        assertEquals("ContentType{fullName='testnamespace:documentWithoutBean', name='documentWithoutBean', prefix='testnamespace', mixin=false, compoundType=false, superTypes=[hippo:document]}", cts.get(3).toString());
    }

    @Test
    public void fetch_filtered_document_types() throws Exception {
        final PluginContext context = getContext();
        final Predicate<ContentType> filter = ContentType::isCompoundType;
        setupTestContentTypes();

        final List<ContentType> cts = contentTypeService.fetchContentTypes(context, filter, false);

        assertEquals(3, cts.size());

        assertEquals("ContentType{fullName='testnamespace:compoundWithBean', name='compoundWithBean', prefix='testnamespace', mixin=false, compoundType=true, superTypes=[]}", cts.get(0).toString());
        assertEquals("ContentType{fullName='testnamespace:compoundWithoutBean', name='compoundWithoutBean', prefix='testnamespace', mixin=false, compoundType=true, superTypes=[]}", cts.get(1).toString());
        assertEquals("ContentType{fullName='other:otherCompound', name='otherCompound', prefix='other', mixin=false, compoundType=true, superTypes=[]}", cts.get(2).toString());
    }

    private void setupTestContentTypes() throws Exception {
        final SortedMap<String, Set<org.onehippo.cms7.services.contenttype.ContentType >> typesByPrefix = new TreeMap<>();

        // own namespace
        final TestContentType ownCompoundWithBean = new TestContentType("compoundWithBean", PROJECT_NAMESPACE_TEST, false, true, new TreeSet<>());
        final TestContentType ownCompoundWithoutBean = new TestContentType("compoundWithoutBean", PROJECT_NAMESPACE_TEST, false, true, new TreeSet<>());
        final TestContentType ownDocumentWithBean = new TestContentType("documentWithBean", PROJECT_NAMESPACE_TEST, false, false, new TreeSet<>(Collections.singletonList("hippo:document")));
        final TestContentType ownDocumentWithoutBean = new TestContentType("documentWithoutBean", PROJECT_NAMESPACE_TEST, false, false, new TreeSet<>(Collections.singletonList("hippo:document")));
        typesByPrefix.put(PROJECT_NAMESPACE_TEST, new HashSet<>(Arrays.asList(ownCompoundWithBean, ownCompoundWithoutBean, ownDocumentWithBean, ownDocumentWithoutBean)));

        // other namespace
        final TestContentType otherCompound = new TestContentType("otherCompound", "other", false, true, new TreeSet<>());
        final TestContentType otherDocument = new TestContentType("otherDocument", "other", false, false, new TreeSet<>());
        typesByPrefix.put("other", new HashSet<>(Arrays.asList(otherCompound, otherDocument)));

        contentTypes = new TestContentTypes(typesByPrefix);

        // trigger Draft mode for compound-without-bean:
        final Session session = jcrService.createSession();
        final Node compoundWithoutBean = session.getNode("/hippo:namespaces/" + PROJECT_NAMESPACE_TEST)
                .addNode("compoundWithoutBean", "hipposysedit:templatetype");
        compoundWithoutBean.addNode("hipposysedit:nodetype", "hippo:handle");
        final Node prototypes = compoundWithoutBean.addNode("hipposysedit:prototypes", "hipposysedit:prototypeset");
        final Node prototype1 = prototypes.addNode("hipposysedit:prototype", PROJECT_NAMESPACE_TEST + ":basedocument");
        prototype1.setProperty("hippostd:stateSummary", "foo");
        final Node prototype2 = prototypes.addNode("hipposysedit:prototype", PROJECT_NAMESPACE_TEST + ":basedocument");
        prototype2.setProperty("hippostd:stateSummary", "foo");
        session.save();
        jcrService.destroySession(session);

        testContentTypeService = new TestContentTypeService();
        HippoServiceRegistry.registerService(testContentTypeService, org.onehippo.cms7.services.contenttype.ContentTypeService.class);
    }

    @After
    public void cleanup() {
        if (testContentTypeService != null) {
            HippoServiceRegistry.unregisterService(testContentTypeService, org.onehippo.cms7.services.contenttype.ContentTypeService.class);
            testContentTypeService = null;
        }
    }

    @Bean
    @Primary
    public ContentBeansService getContentBeansService() {
        return new ContentBeansServiceImpl() {
            @Override
            public Map<String, Path> findBeans(final PluginContext context) {
                final Map<String, Path> beansMap = new HashMap<>();

                beansMap.put(PROJECT_NAMESPACE_TEST + ":compoundWithBean", Paths.get("/path/to/Compound.java"));
                beansMap.put(PROJECT_NAMESPACE_TEST + ":documentWithBean", Paths.get("/path/to/Document.java"));
                beansMap.put(PROJECT_NAMESPACE_TEST + ":imageWithBean", Paths.get("/path/to/Image.java"));

                return beansMap;
            }
        };
    }

    private class TestContentTypes implements ContentTypes  {
        private final SortedMap<String, Set<org.onehippo.cms7.services.contenttype.ContentType >> typesByPrefix;

        private TestContentTypes(SortedMap<String, Set<org.onehippo.cms7.services.contenttype.ContentType >> typesByPrefix) {
            this.typesByPrefix = typesByPrefix;
        }

        @Override
        public EffectiveNodeTypes getEffectiveNodeTypes() {
            return null;
        }

        @Override
        public long version() {
            return 0;
        }

        @Override
        public org.onehippo.cms7.services.contenttype.ContentType getType(final String s) {
            return null;
        }

        @Override
        public SortedMap<String, Set<org.onehippo.cms7.services.contenttype.ContentType >> getTypesByPrefix() {
            return typesByPrefix;
        }

        @Override
        public org.onehippo.cms7.services.contenttype.ContentType getContentTypeForNode(final Node node) {
            return null;
        }

        @Override
        public org.onehippo.cms7.services.contenttype.ContentType getContentTypeForNodeByUuid(final Session session, final String s) {
            return null;
        }

        @Override
        public org.onehippo.cms7.services.contenttype.ContentType getContentTypeForNodeByPath(final Session session, final String s) {
            return null;
        }
    }

    private class TestContentType implements org.onehippo.cms7.services.contenttype.ContentType  {
        private final String name;
        private final String prefix;
        private final boolean mixin;
        private final boolean compound;
        private final SortedSet<String> superTypes;

        private TestContentType(String name, String prefix, boolean mixin, boolean compound, SortedSet<String> superTypes) {
            this.name = prefix + ":" + name;
            this.prefix = prefix;
            this.mixin = mixin;
            this.compound = compound;
            this.superTypes = superTypes;
        }

        @Override
        public long version() {
            return 0;
        }

        @Override
        public boolean isDerivedType() {
            return false;
        }

        @Override
        public boolean isAggregate() {
            return false;
        }

        @Override
        public EffectiveNodeType getEffectiveNodeType() {
            return null;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public SortedSet<String> getSuperTypes() {
            return superTypes;
        }

        @Override
        public SortedSet<String> getAggregatedTypes() {
            return null;
        }

        @Override
        public boolean isContentType(final String s) {
            return false;
        }

        @Override
        public boolean isDocumentType() {
            return false;
        }

        @Override
        public boolean isCompoundType() {
            return compound;
        }

        @Override
        public boolean isMixin() {
            return mixin;
        }

        @Override
        public boolean isCascadeValidate() {
            return false;
        }

        @Override
        public Map<String, ContentTypeProperty> getProperties() {
            return null;
        }

        @Override
        public Map<String, ContentTypeChild> getChildren() {
            return null;
        }

        @Override
        public ContentTypeItem getItem(final String s) {
            return null;
        }
    }

    private class TestContentTypeService implements org.onehippo.cms7.services.contenttype.ContentTypeService {
        @Override
        public EffectiveNodeTypes getEffectiveNodeTypes() throws RepositoryException {
            return null;
        }

        @Override
        public ContentTypes getContentTypes() throws RepositoryException {
            return contentTypes;
        }
    }
}
