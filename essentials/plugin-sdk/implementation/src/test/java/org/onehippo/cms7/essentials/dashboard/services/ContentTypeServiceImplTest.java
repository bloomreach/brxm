/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

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
import org.onehippo.testutils.log4j.Log4jInterceptor;
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
    public void extract_prefix() {
        assertEquals("foo", contentTypeService.extractPrefix("foo:bar"));
        assertEquals("system", contentTypeService.extractPrefix("foobar"));
    }

    @Test
    public void extract_short_name() {
        assertEquals("bar", contentTypeService.extractShortName("foo:bar"));
        assertEquals("foobar", contentTypeService.extractShortName("foobar"));
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

    @Test
    public void add_mixin_to_content_type_and_content() throws Exception {
        final List<String> superTypes = Arrays.asList("hippo:document", "hippostd:publishableSummary");
        final List<String> mixins = Collections.singletonList("mix:referenceable");
        setupTestContentType(superTypes, mixins, null);
        final Session session = jcrService.createSession();

        assertTrue(contentTypeService.addMixinToContentType(PROJECT_NAMESPACE_TEST + ":basedocument", "hippostd:container", session, true));

        // check supertypes
        final Node nodeTypeNode = session.getNode("/hippo:namespaces/" + PROJECT_NAMESPACE_TEST + "/basedocument/hipposysedit:nodetype/hipposysedit:nodetype");
        final Set<String> sts = new HashSet<>();
        for (Value v : nodeTypeNode.getProperty("hipposysedit:supertype").getValues()) {
            sts.add(v.getString());
        }
        assertEquals(3, sts.size());
        assertTrue(sts.contains("hippostd:container"));

        // check prototype mixins
        final Node prototypeNode = session.getNode("/hippo:namespaces/" + PROJECT_NAMESPACE_TEST + "/basedocument/hipposysedit:prototypes/hipposysedit:prototype");
        final Set<String> nts = new HashSet<>();
        for (NodeType nt : prototypeNode.getMixinNodeTypes()) {
            nts.add(nt.getName());
        }
        assertEquals(2, nts.size());
        assertTrue(nts.contains("hippostd:container"));

        // check mixins of content nodes
        final Node instanceNoMixins = session.getNode("/content/instanceNoMixins");
        assertEquals(1, instanceNoMixins.getMixinNodeTypes().length);
        assertEquals("hippostd:container", instanceNoMixins.getMixinNodeTypes()[0].getName());

        // check mixins of content nodes
        final Node instanceWithMixins = session.getNode("/content/instanceWithMixins");
        final Set<String> nts2 = new HashSet<>();
        for (NodeType nt : instanceWithMixins.getMixinNodeTypes()) {
            nts2.add(nt.getName());
        }
        assertEquals(2, nts2.size());
        assertTrue(nts2.contains("hippostd:container"));

        jcrService.destroySession(session);
    }

    @Test
    public void add_mixin_to_content_type_only() throws Exception {
        final List<String> superTypes = Arrays.asList("hippo:document", "hippostd:publishableSummary");
        final List<String> mixins = Collections.singletonList("mix:referenceable");
        setupTestContentType(superTypes, mixins, null);
        final Session session = jcrService.createSession();

        assertTrue(contentTypeService.addMixinToContentType(PROJECT_NAMESPACE_TEST + ":basedocument", "hippostd:container", session, false));

        // check mixins of content nodes
        final Node instanceNoMixins = session.getNode("/content/instanceNoMixins");
        assertEquals(0, instanceNoMixins.getMixinNodeTypes().length);

        // check mixins of content nodes
        final Node instanceWithMixins = session.getNode("/content/instanceWithMixins");
        assertEquals(1, instanceWithMixins.getMixinNodeTypes().length);
        assertEquals("mix:referenceable", instanceWithMixins.getMixinNodeTypes()[0].getName());

        jcrService.destroySession(session);
    }

    @Test
    public void add_existing_mixin_to_content_type() throws Exception {
        final List<String> superTypes = Arrays.asList("hippo:document", "hippostd:publishableSummary");
        final List<String> mixins = Collections.singletonList("mix:referenceable");
        setupTestContentType(superTypes, mixins, null);
        final Session session = jcrService.createSession();

        assertTrue(contentTypeService.addMixinToContentType(PROJECT_NAMESPACE_TEST + ":basedocument", "mix:referenceable", session, false));

        final Node nodeTypeNode = session.getNode("/hippo:namespaces/" + PROJECT_NAMESPACE_TEST + "/basedocument/hipposysedit:nodetype/hipposysedit:nodetype");
        assertEquals(3, nodeTypeNode.getProperty("hipposysedit:supertype").getValues().length);
        final Node prototypeNode = session.getNode("/hippo:namespaces/" + PROJECT_NAMESPACE_TEST + "/basedocument/hipposysedit:prototypes/hipposysedit:prototype");
        assertEquals(1, prototypeNode.getMixinNodeTypes().length);
        jcrService.destroySession(session);
    }

    @Test
    public void add_invalid_mixin() throws Exception {
        final List<String> superTypes = Arrays.asList("hippo:document", "hippostd:publishableSummary");
        final List<String> mixins = Collections.singletonList("mix:referenceable");
        setupTestContentType(superTypes, mixins, null);
        final Session session = jcrService.createSession();

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(ContentTypeServiceImpl.class).build()) {
            assertFalse(contentTypeService.addMixinToContentType(PROJECT_NAMESPACE_TEST + ":basedocument", "foo:bar", session, false));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Failed to add mixin 'foo:bar' to content type 'testnamespace:basedocument'.")));
        }

        final Node nodeTypeNode = session.getNode("/hippo:namespaces/" + PROJECT_NAMESPACE_TEST + "/basedocument/hipposysedit:nodetype/hipposysedit:nodetype");
        assertEquals(2, nodeTypeNode.getProperty("hipposysedit:supertype").getValues().length);
        jcrService.destroySession(session);
    }

    @Test
    public void determine_default_field_position() throws Exception {
        setupTestContentType(null, null, null);
        assertEquals("${cluster.id}.field", contentTypeService.determineDefaultFieldPosition(PROJECT_NAMESPACE_TEST + ":basedocument"));
    }

    @Test
    public void determine_default_field_position_when_set() throws Exception {
        setupTestContentType(null, null, "test");
        assertEquals("test.item", contentTypeService.determineDefaultFieldPosition(PROJECT_NAMESPACE_TEST + ":basedocument"));
    }

    @Test
    public void determine_default_position_of_invalid_content_type() throws Exception {
        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(ContentTypeServiceImpl.class).build()) {
            assertNull(contentTypeService.determineDefaultFieldPosition("foo:bar"));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Failed to determine default field position for content type 'foo:bar'.")));
        }
    }

    private void setupTestContentType(final List<String> superTypes, final List<String> mixins, final String fieldPosition) throws Exception {
        final Session session = jcrService.createSession();

        final Node baseDocument = session.getNode("/hippo:namespaces/" + PROJECT_NAMESPACE_TEST)
                .addNode("basedocument", "hipposysedit:templatetype");
        baseDocument.addMixin("editor:editable");

        final Node nodeTypeNode = baseDocument.addNode("hipposysedit:nodetype", "hippo:handle")
                .addNode("hipposysedit:nodetype", "hipposysedit:nodetype");
        if (superTypes != null) {
            nodeTypeNode.setProperty("hipposysedit:supertype", superTypes.toArray(new String[superTypes.size()]));
        }

        final Node prototypeNode = baseDocument.addNode("hipposysedit:prototypes", "hipposysedit:prototypeset")
                .addNode("hipposysedit:prototype", PROJECT_NAMESPACE_TEST + ":basedocument");
        prototypeNode.setProperty("hippostd:stateSummary", "foo");
        if (mixins != null) {
            for (String mixin : mixins) {
                prototypeNode.addMixin(mixin);
            }

            final Node instanceNoMixins = session.getNode("/content")
                    .addNode("instanceNoMixins", PROJECT_NAMESPACE_TEST + ":basedocument");
            instanceNoMixins.setProperty("hippostd:stateSummary", "foo");
            final Node instanceWithMixins = session.getNode("/content")
                    .addNode("instanceWithMixins", PROJECT_NAMESPACE_TEST + ":basedocument");
            instanceWithMixins.setProperty("hippostd:stateSummary", "foo");
            for (String mixin : mixins) {
                instanceWithMixins.addMixin(mixin);
            }
        }

        final Node editorTemplateNode = baseDocument.addNode("editor:templates", "editor:templateset")
                .addNode("_default_", "frontend:plugincluster")
                .addNode("root", "frontend:plugin");
        if (fieldPosition != null) {
            editorTemplateNode.setProperty("wicket.extensions", new String[]{"blabla"});
            editorTemplateNode.setProperty("blabla", fieldPosition);
        }

        session.save();
        jcrService.destroySession(session);
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
