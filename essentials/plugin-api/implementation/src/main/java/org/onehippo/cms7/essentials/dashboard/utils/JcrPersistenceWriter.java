/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.lang.reflect.Field;
import java.util.Collection;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.essentials.dashboard.config.Document;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.PersistentHandler;
import org.onehippo.cms7.essentials.dashboard.model.hst.SimplePropertyModel;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.AnnotationUtils;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.Persistent;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentCollection;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentMultiProperty;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentNode;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public class JcrPersistenceWriter implements AutoCloseable {

    public static final char PATH_SEPARATOR = '/';
    public static final String ERROR_PROCESSING_VALUE = "Error processing value";
    private static Logger log = LoggerFactory.getLogger(JcrPersistenceWriter.class);
    private final PluginContext context;
    private final Session session;

    public JcrPersistenceWriter(final Session session, final PluginContext context) {
        this.context = context;
        this.session = session;

    }


    public Item write(final Document model) {
        return writeNode(model);
    }

    private Node writeNode(final Document model) {

        try {
            createSubfolders(model.getParentPath());
        } catch (RepositoryException e) {
            log.error("Error creating subfolders", e);
            return null;
        }


        final PersistentNode node = AnnotationUtils.getClassAnnotation(model.getClass(), PersistentNode.class);
        if (node == null) {
            log.error("No @PersistentNode annotation found for object: {}", model);
            return null;
        }

        final String type = node.type();
        if (Strings.isNullOrEmpty(type)) {
            log.error("@PersistentNode type must have a value but was empty");
            return null;
        }

        final PersistentHandler<PersistentNode, Node> nodeWriter = PersistentNode.ProcessAnnotation.NODE;
        final Node jcrNode = nodeWriter.execute(session, model, node);
        if (jcrNode == null) {
            return null;
        }
        // process single properties:
        final PersistentHandler<PersistentProperty, Property> propWriter = PersistentProperty.ProcessAnnotation.PROPERTY;
        final Collection<Field> fields = AnnotationUtils.getAnnotatedFields(model.getClass(), PersistentProperty.class);
        for (Field field : fields) {
            try {
                final Object value = field.get(model);
                if (value != null) {
                    final PersistentProperty property = field.getAnnotation(PersistentProperty.class);
                    final String name = property.name();
                    final Document myModel = new SimplePropertyModel(value);
                    myModel.setParentPath(jcrNode.getPath());
                    myModel.setName(name);
                    // write single property:
                    propWriter.execute(session, myModel, property);
                }
            } catch (IllegalAccessException e) {
                log.error(ERROR_PROCESSING_VALUE, e);
            } catch (RepositoryException e) {
                log.error("Error fetching parent path", e);
            }
        }

        final PersistentMultiProperty.ProcessAnnotation multiWriter = PersistentMultiProperty.ProcessAnnotation.MULTI_PROPERTY;
        final Collection<Field> multiFields = AnnotationUtils.getAnnotatedFields(model.getClass(), PersistentMultiProperty.class);
        for (Field multiField : multiFields) {
            try {
                final Object value = multiField.get(model);
                if (value != null) {
                    final PersistentMultiProperty p = multiField.getAnnotation(PersistentMultiProperty.class);
                    final String name = p.name();
                    final Document myModel = new SimplePropertyModel(value);
                    myModel.setParentPath(jcrNode.getPath());
                    myModel.setName(name);
                    // write single property:
                    multiWriter.execute(session, myModel, p);
                }
            } catch (IllegalAccessException e) {
                log.error(ERROR_PROCESSING_VALUE, e);
            } catch (RepositoryException e) {
                log.error("Error fetching parent path", e);
            }
        }
        // process persistent nodes:
        final Collection<Field> persistentFields = AnnotationUtils.getAnnotatedFields(model.getClass(), Persistent.class);
        for (Field persistentField : persistentFields) {
            try {
                final Object value = persistentField.get(model);
                if (value instanceof Document) {
                    final PersistentNode myNode = AnnotationUtils.getClassAnnotation(value.getClass(), PersistentNode.class);
                    if (myNode == null) {
                        log.error("Item is not annotated by @PersistentNode annotation: {}", value.getClass());
                        continue;
                    }
                    writeNode((Document) value);
                }
            } catch (IllegalAccessException e) {
                log.error(ERROR_PROCESSING_VALUE, e);
            }
        }
        // process collections:
        final Collection<Field> collectionFields = AnnotationUtils.getAnnotatedFields(model.getClass(), PersistentCollection.class);
        for (Field collectionField : collectionFields) {
            try {
                final Object value = collectionField.get(model);
                if (value instanceof Collection) {
                    Iterable<?> collection = (Iterable<?>) value;
                    for (Object o : collection) {
                        if (o instanceof Document) {
                            writeNode((Document) o);
                        } else {
                            if (o != null) {
                                log.error("Value is within collection is not JcrModel type:  {}", o.getClass());
                            }
                        }
                    }
                } else {
                    log.error("Field is annotated by @PersistableCollection however it is not collection type {}", value.getClass());
                }
            } catch (IllegalAccessException e) {
                log.error(ERROR_PROCESSING_VALUE, e);
            }

        }


        return jcrNode;

    }

    private Node createSubfolders(final String path) throws RepositoryException {

        final String[] pathParts = StringUtils.split(path, PATH_SEPARATOR);
        final StringBuilder parent = new StringBuilder();
        Node parentNode = session.getRootNode();
        for (final String pathPart : pathParts) {
            parent.append(PATH_SEPARATOR);
            String folderPath = parent.append(pathPart).toString();
            if (session.itemExists(folderPath)) {
                log.info("folderPath {}", folderPath);
                parentNode = parentNode.getNode(pathPart);
                continue;
            }
            log.info("folderPath {}", folderPath);
            parentNode = parentNode.addNode(pathPart, "essentials:folder");
            session.save();

        }


        return parentNode;
    }

    @Override
    public void close() throws Exception {
        GlobalUtils.cleanupSession(session);
    }
}

