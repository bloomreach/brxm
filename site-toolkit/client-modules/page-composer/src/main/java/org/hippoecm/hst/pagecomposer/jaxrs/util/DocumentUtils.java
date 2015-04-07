/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.ConfigurationUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.model.DocumentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ParameterType;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentUtils {

    private static final Logger log = LoggerFactory.getLogger(DocumentUtils.class);

    public static Set<DocumentRepresentation> findAvailableDocumentRepresentations(final PageComposerContextService pageComposerContextService,
                                                                                   final HstComponentConfiguration page,
                                                                                   final DocumentRepresentation primaryDocumentRepresentation,
                                                                                   final boolean documentsOnly) throws RepositoryException {
        Set<DocumentRepresentation> documentRepresentations = new HashSet<>();
        if (primaryDocumentRepresentation != null) {
            // regardless whether current primaryDocumentRepresentation is document or not, we add it as it is the currently
            // selected one
            documentRepresentations.add(primaryDocumentRepresentation);
        }
        if (page == null) {
            return documentRepresentations;
        }
        populateDocumentRepresentationsRecursive(pageComposerContextService, page, documentRepresentations, documentsOnly);
        return documentRepresentations;
    }

    public static void populateDocumentRepresentationsRecursive(final PageComposerContextService pageComposerContextService,
                                                                final HstComponentConfiguration item,
                                                                final Set<DocumentRepresentation> documentRepresentations,
                                                                final boolean documentsOnly) throws RepositoryException {
        populateDocumentRepresentations(pageComposerContextService, item, documentRepresentations, documentsOnly);
        for (HstComponentConfiguration child : item.getChildren().values()) {
            populateDocumentRepresentationsRecursive(pageComposerContextService, child, documentRepresentations, documentsOnly);
        }
    }

    public static void populateDocumentRepresentations(final PageComposerContextService pageComposerContextService,
                                                       final HstComponentConfiguration item,
                                                       final Set<DocumentRepresentation> documentRepresentations,
                                                       final boolean documentsOnly) throws RepositoryException {

        if (item.getComponentClassName() == null) {
            return;
        }
        try {
            Class<?> componentClass = Class.forName(item.getComponentClassName());
            ParametersInfo info = componentClass.getAnnotation(ParametersInfo.class);
            if (info != null) {

                // we require a hst config user session that can read everywhere because we need to get the document names
                // for all component picked documents, and the current webmaster might not have read access everywhere.

                final String contentPath = pageComposerContextService.getEditingMount().getContentPath();
                final Class<?> classType = info.type();
                if (classType == null) {
                    return;
                }
                for (Method method : classType.getMethods()) {
                    if (method.isAnnotationPresent(Parameter.class)) {
                        final Parameter propAnnotation = method.getAnnotation(Parameter.class);
                        final Annotation annotation = ParameterType.getTypeAnnotation(method);
                        final String propertyName;
                        boolean absolutePath;
                        if (annotation instanceof DocumentLink) {
                            absolutePath = false;
                            // for DocumentLink we need some extra processing
                            final DocumentLink documentLink = (DocumentLink) annotation;
                            propertyName = propAnnotation.name();
                        } else if (annotation instanceof JcrPath) {
                            // for JcrPath we need some extra processing too
                            final JcrPath jcrPath = (JcrPath) annotation;
                            propertyName = propAnnotation.name();
                            absolutePath = !jcrPath.isRelative();
                        } else {
                            continue;
                        }

                        if (propertyName != null) {
                            List<String> propertyNames = new ArrayList<>();
                            propertyNames.add(propertyName);
                            for (String prefix : item.getParameterPrefixes()) {
                                if (StringUtils.isNotEmpty(prefix)) {
                                    propertyNames.add(ConfigurationUtils.createPrefixedParameterName(prefix, propertyName));
                                }
                            }
                            for (String parameterName : propertyNames) {
                                String documentLocation = item.getParameter(parameterName);
                                if (StringUtils.isEmpty(documentLocation) || "/".equals(documentLocation)) {
                                    continue;
                                }

                                if (absolutePath) {
                                    if (!documentLocation.startsWith(contentPath + "/")) {
                                        log.debug("Skipping absolute document path '{}' outside or equal to current root content '{}'",
                                                documentLocation, contentPath);
                                        continue;
                                    }
                                    documentLocation = documentLocation.substring(contentPath.length() + 1);
                                }
                                final DocumentRepresentation presentation = getDocumentRepresentationHstConfigUser(documentLocation, contentPath);
                                if (documentsOnly) {
                                    if (presentation.isDocument() && presentation.isExists()) {
                                        documentRepresentations.add(presentation);
                                    } else {
                                        log.debug("Skipping document represention '{}' because is not a document or does not exist.",
                                                presentation);
                                    }
                                } else {
                                    documentRepresentations.add(presentation);
                                }
                            }

                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            log.info("Cannot load component class '{}' for '{}'", item.getComponentClassName(), item);
        }

    }


    /**
     * @throws java.lang.IllegalArgumentException is <code>relPath</code> does not start with <code>mount.getContentPath()
     *                                            + '/'</code>
     * @throws RuntimeRepositoryException         in case some repository exception happens
     */
    public static DocumentRepresentation getDocumentRepresentationHstConfigUser(final String relPath, final String rootContentPath) {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
        Credentials configUser = HstServices.getComponentManager().getComponent(Credentials.class.getName() + ".hstconfigreader");
        Session session = null;

        try {
            // pooled hst config user session which has in general read access everywhere
            session = repository.login(configUser);
            final Node node;
            if (StringUtils.isEmpty(relPath)) {
               node = session.getNode(rootContentPath);
            } else {
                node = session.getNode(rootContentPath + "/" + relPath);
            }
            final boolean isDocument;
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                isDocument = true;
            } else if (!node.isSame(node.getSession().getRootNode()) &&
                    node.isNodeType(HippoNodeType.NT_DOCUMENT) &&
                    node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                isDocument = true;
            } else {
                isDocument = false;
            }
            String displayName = ((HippoNode) node).getLocalizedName();
            return new DocumentRepresentation(relPath, rootContentPath, displayName, isDocument, true);
        } catch (PathNotFoundException e) {
            return new DocumentRepresentation(relPath, rootContentPath, null, false, false);
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException("Could not obtain hst config user session", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

}
