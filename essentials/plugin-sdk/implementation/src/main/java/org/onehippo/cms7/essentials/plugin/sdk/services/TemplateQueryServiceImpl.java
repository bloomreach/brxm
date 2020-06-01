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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.essentials.sdk.api.service.ContentTypeService;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.onehippo.cms7.essentials.sdk.api.service.TemplateQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TemplateQueryServiceImpl implements TemplateQueryService {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateQueryServiceImpl.class);
    private static final String TEMPLATE_QUERIES_ROOTPATH = "/hippo:configuration/hippo:queries/hippo:templates";
    private static final String[] MODIFY_VALUES_DOCUMENT = {"./_name", "$name", "./hippotranslation:locale",
            "$inherited", "./hippotranslation:id", "$uuid", "./hippostdpubwf:createdBy", "$holder",
            "./hippostdpubwf:creationDate", "$now", "./hippostdpubwf:lastModifiedBy", "$holder",
            "./hippostdpubwf:lastModificationDate", "$now", "./hippostd:holder", "$holder"};
    private static final String[] MODIFY_VALUES_FOLDER = {"./_name", "$name", "./hippotranslation:id", "$uuid",
            "./hippotranslation:locale", "$inherited"};
    private static final String XPATH_QUERY_DOCUMENT = "//element(*,hipposysedit:namespacefolder)" +
            "/element(*,mix:referenceable)" +
            "/element(*,hipposysedit:templatetype)/hipposysedit:prototypes/element(hipposysedit:prototype,%s)";
    private static final String XPATH_QUERY_FOLDER = "/jcr:root/hippo:configuration/hippo:queries/hippo:templates/" +
            "%s/hippostd:templates/node()";
    private static final String TEMPLATE_QUERIES_EN_TRANSLATIONS_PATH =
            "/hippo:configuration/hippo:translations/hippo:templates/en";

    private final JcrService jcrService;
    private final ContentTypeService contentTypeService;

    @Inject
    public TemplateQueryServiceImpl(final JcrService jcrService, final ContentTypeService contentTypeService) {
        this.jcrService = jcrService;
        this.contentTypeService = contentTypeService;
    }

    @Override
    public boolean createDocumentTypeTemplateQuery(final String jcrDocumentType) {
        if (documentTypeTemplateQueryExists(jcrDocumentType)) {
            return true;
        }

        final String shortName = contentTypeService.extractShortName(jcrDocumentType);

        final Session session = jcrService.createSession();
        try {
            final Node templatesRoot = session.getNode(TEMPLATE_QUERIES_ROOTPATH);
            final String templateQueryNodeName = determineTemplateQueryNodeName(templatesRoot, shortName, true);

            createDocumentTypeTemplateQueryNode(templatesRoot, templateQueryNodeName, jcrDocumentType);
            addQueryTemplateTranslation(session, templateQueryNodeName, shortName, true);

            session.save();
            return true;
        } catch (RepositoryException e) {
            LOG.warn("Unexpected exception while creating template query for document type '{}'.", jcrDocumentType, e);
        } finally {
            jcrService.destroySession(session);
        }

        return false;
    }

    private void createDocumentTypeTemplateQueryNode(final Node templatesRoot, final String templateQueryNodeName,
                                                     final String jcrDocumentType) throws RepositoryException {
        final Node templateQueryNode = templatesRoot.addNode(templateQueryNodeName, "hippostd:templatequery");
        templateQueryNode.setProperty("hippostd:modify", MODIFY_VALUES_DOCUMENT);
        templateQueryNode.setProperty("jcr:language", "xpath");
        templateQueryNode.setProperty("jcr:statement", String.format(XPATH_QUERY_DOCUMENT, jcrDocumentType));
    }

    @Override
    public boolean createFolderTemplateQuery(final String jcrDocumentType) {
        if (folderTemplateQueryExists(jcrDocumentType)) {
            return true;
        }

        final String documentTypeTemplateQueryNodeName = findDocumentTypeTemplateQueryNodeName(jcrDocumentType);
        final String shortName = contentTypeService.extractShortName(jcrDocumentType);

        final Session session = jcrService.createSession();
        try {
            final Node templatesRoot = session.getNode(TEMPLATE_QUERIES_ROOTPATH);
            final String folderTemplateQueryNodeName = determineTemplateQueryNodeName(templatesRoot, shortName, false);

            createFolderTemplateQueryNode(templatesRoot, folderTemplateQueryNodeName, documentTypeTemplateQueryNodeName);
            addQueryTemplateTranslation(session, folderTemplateQueryNodeName, shortName, false);

            session.save();
            return true;
        } catch (RepositoryException e) {
            LOG.warn("Unexpected exception while creating folder query template for document type '{}'.", jcrDocumentType, e);
        } finally {
            jcrService.destroySession(session);
        }

        return false;
    }

    private void createFolderTemplateQueryNode(final Node templatesRoot, final String folderTemplateQueryNodeName,
                                               final String documentTypeTemplateQueryNodeName) throws RepositoryException {
        final Node templateQueryNode = templatesRoot.addNode(folderTemplateQueryNodeName, "hippostd:templatequery");
        templateQueryNode.setProperty("hippostd:modify", MODIFY_VALUES_FOLDER);
        templateQueryNode.setProperty("jcr:language", "xpath");
        templateQueryNode.setProperty("jcr:statement", String.format(XPATH_QUERY_FOLDER, folderTemplateQueryNodeName));

        final Node templatesNode = templateQueryNode.addNode("hippostd:templates", "hippostd:templates");

        final Node folderNode = templatesNode.addNode("hippostd:folder", "hippostd:folder");
        folderNode.addMixin("hippotranslation:translated");
        folderNode.setProperty("hippotranslation:id", "generated id");
        folderNode.setProperty("hippotranslation:locale", "inherited locale");
        String[] folderTypes = {documentTypeTemplateQueryNodeName, folderTemplateQueryNodeName};
        folderNode.setProperty("hippostd:foldertype", folderTypes);
    }

    private String determineTemplateQueryNodeName(final Node templatesRoot, final String shortName,
                                                  final boolean forDocument) throws RepositoryException {
        final String baseCandidate = String.format("new-%s-%s", shortName, forDocument ? "document" : "folder");

        String candidate = baseCandidate;
        for (int i = 2; templatesRoot.hasNode(candidate); i++) {
            candidate = String.format("%s%d", baseCandidate, i);
        }

        return candidate;
    }

    private void addQueryTemplateTranslation(final Session session, final String templateQueryNodeName,
                                             final String shortName, final boolean forDocument) throws RepositoryException {
        final String translation = String.format("new %s %s", shortName, forDocument ? "document" : "folder");
        final Node translationsNode = session.getNode(TEMPLATE_QUERIES_EN_TRANSLATIONS_PATH);
        translationsNode.setProperty(templateQueryNodeName, translation);
    }

    @Override
    public boolean documentTypeTemplateQueryExists(final String jcrDocumentType) {
        return findDocumentTypeTemplateQueryNodeName(jcrDocumentType) != null;
    }

    private String findDocumentTypeTemplateQueryNodeName(final String jcrDocumentType) {
        final Session session = jcrService.createSession();
        try {
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final Node prototypeNode = findPrototypeNode(queryManager, jcrDocumentType);
            if (prototypeNode != null) {
                for (final Node query : getAllTemplateQueries(session)) {
                    if (prototypeNode.isSame(findTemplateNode(queryManager, query))) {
                        return query.getName();
                    }
                }
            }
        } catch (final RepositoryException e) {
            LOG.warn("Unexpected exception while checking template query for document type '{}'.", jcrDocumentType, e);
        } finally {
            jcrService.destroySession(session);
        }

        return null;
    }

    private Node findPrototypeNode(final QueryManager queryManager, final String jcrDocumentType) throws RepositoryException {
        final String xPathQueryForProtoType = String.format("//element(hipposysedit:prototype,%s)", jcrDocumentType);
        final NodeIterator prototypeIterator = queryManager.createQuery(xPathQueryForProtoType, "xpath").execute().getNodes();

        if (prototypeIterator.hasNext()) {
            return prototypeIterator.nextNode();
        }

        LOG.warn("Failed checking document type template query. Document type '{}' has no prototype.", jcrDocumentType);
        return null;
    }

    private Node findTemplateNode(final QueryManager queryManager, final Node query) throws RepositoryException {
        final String statement = query.getProperty("jcr:statement").getString().trim();
        final String language = query.getProperty("jcr:language").getString().trim();
        final NodeIterator templateIterator = queryManager.createQuery(statement, language).execute().getNodes();
        return templateIterator.hasNext() ? templateIterator.nextNode() : null;
    }

    private NodeIterable getAllTemplateQueries(final Session session) throws RepositoryException {
        return new NodeIterable(session.getNode(TEMPLATE_QUERIES_ROOTPATH).getNodes());
    }

    @Override
    public boolean folderTemplateQueryExists(final String jcrDocumentType) {
        final String documentTypeTemplateQueryNodeName = findDocumentTypeTemplateQueryNodeName(jcrDocumentType);
        if (documentTypeTemplateQueryNodeName == null) {
            return false;
        }

        final Session session = jcrService.createSession();
        try {
            for (Node folderTemplate : getAllFolderTemplates(session)) {
                if (isTemplateForFolderTemplateQuery(folderTemplate, documentTypeTemplateQueryNodeName)) {
                    return true;
                }
            }
        } catch (RepositoryException e) {
            LOG.warn("Unexpected exception while checking folder template query for document type '{}'.", jcrDocumentType, e);
        } finally {
            jcrService.destroySession(session);
        }
        return false;
    }

    private NodeIterable getAllFolderTemplates(final Session session) throws RepositoryException {
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final String folderTemplateQueryStatement = "//element(*,hippostd:templates)/element(*,hippostd:folder)";
        final Query folderTemplatesQuery = queryManager.createQuery(folderTemplateQueryStatement, "xpath");
        return new NodeIterable(folderTemplatesQuery.execute().getNodes());
    }

    /**
     * We define a "folder template" as a template with exactly two values in the hippostd:foldertype property:
     *
     * 1) the template's own query node name (parent of parent)
     * 2) the document type template query node name
     *
     * The order of these two entries can be arbitrary.
     */
    private boolean isTemplateForFolderTemplateQuery(final Node folderTemplate, final String documentTypeTemplateQueryNodeName) throws RepositoryException {
        final Value[] folderTypeValues = folderTemplate.getProperty("hippostd:foldertype").getValues();
        if (folderTypeValues.length != 2) {
            return false;
        }

        boolean documentTypeTemplateQueryFound = false;
        boolean folderTemplateQueryFound = false;
        for (final Value folderTypeValue : folderTypeValues) {
            final String folderType = folderTypeValue.getString();
            if (folderType.equals(documentTypeTemplateQueryNodeName)) {
                documentTypeTemplateQueryFound = true;
            } else {
                final String folderTemplateQueryNodeName = folderTemplate.getParent().getParent().getName();
                if (folderType.equals(folderTemplateQueryNodeName)) {
                    folderTemplateQueryFound = true;
                }
            }
        }

        return folderTemplateQueryFound && documentTypeTemplateQueryFound;
    }
}
