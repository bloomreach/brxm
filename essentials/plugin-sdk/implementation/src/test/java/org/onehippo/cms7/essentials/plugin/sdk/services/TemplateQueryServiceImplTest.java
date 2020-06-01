/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.services;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.sdk.api.service.ContentTypeService;
import org.onehippo.cms7.essentials.sdk.api.service.TemplateQueryService;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TemplateQueryServiceImplTest extends BaseRepositoryTest {

    @Inject private TemplateQueryService templateQueryService;
    @Inject private ContentTypeService contentTypeService;

    @Test
    public void invalid_input() {
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(TemplateQueryServiceImpl.class).build()) {
            assertFalse(templateQueryService.documentTypeTemplateQueryExists("foo:bar"));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Unexpected exception while checking template query for document type 'foo:bar'.")));
        }
    }

    @Test
    public void no_prototype() {
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(TemplateQueryServiceImpl.class).build()) {
            assertFalse(templateQueryService.documentTypeTemplateQueryExists("testnamespace:basedocument"));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Failed checking document type template query. Document type 'testnamespace:basedocument' has no prototype.")));
        }
    }

    @Test
    public void no_template_query() throws Exception {
        createPrototypeNode("testnamespace:basedocument");
        assertFalse(templateQueryService.documentTypeTemplateQueryExists("testnamespace:basedocument"));

        assertTrue(templateQueryService.createDocumentTypeTemplateQuery("mytestproject:textdocument"));
        assertFalse(templateQueryService.documentTypeTemplateQueryExists("testnamespace:basedocument"));

        assertTrue(templateQueryService.createDocumentTypeTemplateQuery("testnamespace:basedocument"));
        assertTrue(templateQueryService.documentTypeTemplateQueryExists("testnamespace:basedocument"));

        assertTrue(templateQueryService.createDocumentTypeTemplateQuery("testnamespace:basedocument"));
        assertTrue(templateQueryService.documentTypeTemplateQueryExists("testnamespace:basedocument"));
    }

    @Test
    public void choose_alternative_template_query_node_name() throws Exception {
        createPrototypeNode("mytestproject:textdocument");
        createPrototypeNode("testnamespace:basedocument");

        assertTrue(templateQueryService.createDocumentTypeTemplateQuery("mytestproject:textdocument"));

        final Session session = jcrService.createSession();
        final String fromPath = "/hippo:configuration/hippo:queries/hippo:templates/new-textdocument-document";
        final String toPath = "/hippo:configuration/hippo:queries/hippo:templates/new-basedocument-document";
        session.move(fromPath, toPath);
        session.save();

        assertTrue(templateQueryService.createDocumentTypeTemplateQuery("testnamespace:basedocument"));

        session.refresh(false);
        final String alternative = "/hippo:configuration/hippo:queries/hippo:templates/new-basedocument-document2";
        assertTrue(session.nodeExists(alternative));
        jcrService.destroySession(session);
    }

    @Test
    public void problem_creating_template_node() throws Exception {
        createPrototypeNode("testnamespace:basedocument");

        final Session session = jcrService.createSession();
        session.getNode("/hippo:configuration/hippo:translations/hippo:templates").remove();
        session.save();
        jcrService.destroySession(session);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(TemplateQueryServiceImpl.class).build()) {
            assertFalse(templateQueryService.createDocumentTypeTemplateQuery("testnamespace:basedocument"));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Unexpected exception while creating template query for document type 'testnamespace:basedocument'.")));
        }
    }

    @Test
    public void invalid_input_folder_template_query() {
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(TemplateQueryServiceImpl.class).build()) {
            assertFalse(templateQueryService.folderTemplateQueryExists("foo:bar"));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Unexpected exception while checking template query for document type 'foo:bar'.")));
        }
    }

    @Test
    public void no_folder_queries() throws Exception {
        createPrototypeNode("testnamespace:basedocument");
        templateQueryService.createDocumentTypeTemplateQuery("testnamespace:basedocument");

        assertFalse(templateQueryService.folderTemplateQueryExists("testnamespace:basedocument"));
    }

    @Test
    public void folder_query_with_incorrect_number_of_folder_types() throws Exception {
        createPrototypeNode("testnamespace:basedocument");
        templateQueryService.createDocumentTypeTemplateQuery("testnamespace:basedocument");
        assertTrue(templateQueryService.createFolderTemplateQuery("testnamespace:basedocument"));

        final Session session = jcrService.createSession();
        final Node template = session.getNode("/hippo:configuration/hippo:queries/hippo:templates/new-basedocument-folder/hippostd:templates/hippostd:folder");
        template.setProperty("hippostd:foldertype", new String[]{"new-basedocument-folder"});
        session.save();
        jcrService.destroySession(session);

        assertFalse(templateQueryService.folderTemplateQueryExists("testnamespace:basedocument"));
    }

    @Test
    public void folder_query_without_document_type_template_query() throws Exception {
        createPrototypeNode("testnamespace:basedocument");
        templateQueryService.createDocumentTypeTemplateQuery("testnamespace:basedocument");
        assertTrue(templateQueryService.createFolderTemplateQuery("testnamespace:basedocument"));

        final Session session = jcrService.createSession();
        final Node template = session.getNode("/hippo:configuration/hippo:queries/hippo:templates/new-basedocument-folder/hippostd:templates/hippostd:folder");
        template.setProperty("hippostd:foldertype", new String[]{"new-basedocument-folder", "bla-bla-bla"});
        session.save();
        jcrService.destroySession(session);

        assertFalse(templateQueryService.folderTemplateQueryExists("testnamespace:basedocument"));
    }

    @Test
    public void folder_query_without_folder_template_query() throws Exception {
        createPrototypeNode("testnamespace:basedocument");
        templateQueryService.createDocumentTypeTemplateQuery("testnamespace:basedocument");
        assertTrue(templateQueryService.createFolderTemplateQuery("testnamespace:basedocument"));

        final Session session = jcrService.createSession();
        final Node template = session.getNode("/hippo:configuration/hippo:queries/hippo:templates/new-basedocument-folder/hippostd:templates/hippostd:folder");
        template.setProperty("hippostd:foldertype", new String[]{"new-basedocument-document", "bla-bla-bla"});
        session.save();
        jcrService.destroySession(session);

        assertFalse(templateQueryService.folderTemplateQueryExists("testnamespace:basedocument"));
    }

    @Test
    public void folder_query_without_folder_type_property() throws Exception {
        createPrototypeNode("testnamespace:basedocument");
        templateQueryService.createDocumentTypeTemplateQuery("testnamespace:basedocument");
        assertTrue(templateQueryService.createFolderTemplateQuery("testnamespace:basedocument"));

        final Session session = jcrService.createSession();
        final Node template = session.getNode("/hippo:configuration/hippo:queries/hippo:templates/new-basedocument-folder/hippostd:templates/hippostd:folder");
        template.getProperty("hippostd:foldertype").remove();
        session.save();
        jcrService.destroySession(session);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(TemplateQueryServiceImpl.class).build()) {
            assertFalse(templateQueryService.folderTemplateQueryExists("testnamespace:basedocument"));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Unexpected exception while checking folder template query for document type 'testnamespace:basedocument'.")));
        }
    }

    @Test
    public void folder_query_success() throws Exception {
        createPrototypeNode("testnamespace:basedocument");
        templateQueryService.createDocumentTypeTemplateQuery("testnamespace:basedocument");
        assertTrue(templateQueryService.createFolderTemplateQuery("testnamespace:basedocument"));
        assertTrue(templateQueryService.folderTemplateQueryExists("testnamespace:basedocument"));
    }

    @Test
    public void folder_query_duplicate() throws Exception {
        createPrototypeNode("testnamespace:basedocument");
        templateQueryService.createDocumentTypeTemplateQuery("testnamespace:basedocument");
        assertTrue(templateQueryService.createFolderTemplateQuery("testnamespace:basedocument"));
        assertTrue(templateQueryService.folderTemplateQueryExists("testnamespace:basedocument"));

        assertTrue(templateQueryService.createFolderTemplateQuery("testnamespace:basedocument"));
        assertTrue(templateQueryService.folderTemplateQueryExists("testnamespace:basedocument"));
    }

    @Test
    public void problem_creating_folder_template_node() throws Exception {
        createPrototypeNode("testnamespace:basedocument");
        templateQueryService.createDocumentTypeTemplateQuery("testnamespace:basedocument");

        final Session session = jcrService.createSession();
        session.getNode("/hippo:configuration/hippo:translations/hippo:templates").remove();
        session.save();
        jcrService.destroySession(session);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(TemplateQueryServiceImpl.class).build()) {
            assertFalse(templateQueryService.createFolderTemplateQuery("testnamespace:basedocument"));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Unexpected exception while creating folder query template for document type 'testnamespace:basedocument'.")));
        }
    }

    private void createPrototypeNode(final String jcrDocumentType) throws Exception {
        final String prefix = contentTypeService.extractPrefix(jcrDocumentType);
        final Session session = jcrService.createSession();
        final Node documentTypeNode = session.getNode("/hippo:namespaces/" + prefix)
                .addNode("basedocument", "hipposysedit:templatetype");
        documentTypeNode.addNode("hipposysedit:nodetype", "hippo:handle");
        final Node prototypes = documentTypeNode.addNode("hipposysedit:prototypes", "hipposysedit:prototypeset");
        final Node prototype = prototypes.addNode("hipposysedit:prototype", jcrDocumentType);
        prototype.setProperty("hippostd:stateSummary", "foo");
        session.save();
        jcrService.destroySession(session);
    }
}
