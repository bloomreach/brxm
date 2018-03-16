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

package org.onehippo.cms7.essentials.plugin.sdk.utils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.JcrUtils;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateQueryUtils {

    public static final String TEMPLATE_QUERIES_ROOTPATH = "/hippo:configuration/hippo:queries/hippo:templates";
    public static final String DOCUMENT_NAME = "new-%s-document";
    public static final String FOLDER_NAME = "new-%s-folder";
    public static final String XPATH_QUERY_DOCUMENT = "//element(*,hipposysedit:namespacefolder)" +
            "/element(*,mix:referenceable)" +
            "/element(*,hipposysedit:templatetype)/hipposysedit:prototypes/element(hipposysedit:prototype,%s:%s)";
    public static final String[] MODIFY_VALUES_DOCUMENT = {"./_name", "$name", "./hippotranslation:locale", "$inherited",
            "./hippotranslation:id", "$uuid", "./hippostdpubwf:createdBy", "$holder", "./hippostdpubwf:creationDate",
            "$now", "./hippostdpubwf:lastModifiedBy", "$holder", "./hippostdpubwf:lastModificationDate", "$now",
            "./hippostd:holder", "$holder"};
    public static final String XPATH_QUERY_FOLDER = "/jcr:root/hippo:configuration/hippo:queries/hippo:templates/" +
            "new-%s-folder/hippostd:templates/node()";
    public static final String[] MODIFY_VALUES_FOLDER = {"./_name", "$name", "./hippotranslation:id", "$uuid",
            "./hippotranslation:locale", "$inherited"};

    private static Logger log = LoggerFactory.getLogger(GalleryUtils.class);

    public static boolean createDocumentTemplateQuery(final JcrService jcrService, final String projectNamespace,
                                               final String documentName) {
        final Session session = jcrService.createSession();

        try {
            final String nodePath = TEMPLATE_QUERIES_ROOTPATH + "/" + String.format(DOCUMENT_NAME, documentName);
            if (session.nodeExists(nodePath)) {
                log.warn("Node already exists at path '{}'", nodePath);
                return false;
            }
            final Node templateQueryNode = JcrUtils.getOrCreateByPath(nodePath, "hippostd:templatequery", session);
            templateQueryNode.setProperty("hippostd:icon", "adddocument_ico"); // TODO: necessary?
            templateQueryNode.setProperty("hippostd:modify", MODIFY_VALUES_DOCUMENT);
            templateQueryNode.setProperty("jcr:language", "xpath");
            templateQueryNode.setProperty("jcr:statement", String.format(XPATH_QUERY_DOCUMENT, projectNamespace, documentName));

            session.save();
            return true;
        } catch (RepositoryException e) {
            log.error("Error creating template query", e);

        } finally {
            jcrService.destroySession(session);
        }
        return false;
    }

    public static boolean createFolderTemplateQuery(final JcrService jcrService, final String projectNamespace, final String documentName) {
        final Session session = jcrService.createSession();

        try {
            final String nodePath = TEMPLATE_QUERIES_ROOTPATH + "/" + String.format(FOLDER_NAME, documentName);
            if (session.nodeExists(nodePath)) {
                log.warn("Node already exists at path '{}'", nodePath);
                return false;
            }
            createFolderTemplateQueryNodes(session, nodePath, projectNamespace, documentName);

            session.save();
            return true;
        } catch (RepositoryException e) {
            log.error("Error creating template query folders", e);

        } finally {
            jcrService.destroySession(session);
        }
        return false;
    }

    private static void createFolderTemplateQueryNodes(final Session session, final String nodePath,
                                                final String projectNamespace, final String documentName)
            throws RepositoryException {

        final Node templateQueryNode = JcrUtils.getOrCreateByPath(nodePath, "hippostd:templatequery", session);
        templateQueryNode.setProperty("hippostd:icon", "adddocument_ico"); // TODO: necessary?
        templateQueryNode.setProperty("hippostd:modify", MODIFY_VALUES_FOLDER);
        templateQueryNode.setProperty("jcr:language", "xpath");
        templateQueryNode.setProperty("jcr:statement", String.format(XPATH_QUERY_FOLDER, documentName));

        final Node templatesNode = templateQueryNode.addNode("hippostd:templates", "hippostd:templates");

        final Node folderNode = templatesNode.addNode("hippostd:folder", "hippostd:folder");
        folderNode.setProperty("hippotranslation:id", "generated id");
        folderNode.setProperty("hippotranslation:locale", "inherited locale");
        String[] folderTypes = {String.format(DOCUMENT_NAME, documentName), String.format(FOLDER_NAME, documentName)};
        folderNode.setProperty("hippostd:foldertype", folderTypes);
    }

}
