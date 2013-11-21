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
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.JcrModel;
import org.onehippo.cms7.essentials.dashboard.model.PersistentHandler;
import org.onehippo.cms7.essentials.dashboard.model.hst.SimplePropertyModel;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.AnnotationUtils;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentNode;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public class JcrPersistenceWriter {

    private static Logger log = LoggerFactory.getLogger(JcrPersistenceWriter.class);
    private final PluginContext context;

    public JcrPersistenceWriter(final PluginContext context) {
        this.context = context;
    }

    public Item write(final JcrModel model) {
        final Node rootNode = writeNode(model);
        if (rootNode == null) {
            return null;
        }
        //save changes
        try {
            context.getSession().save();
            return rootNode;
        } catch (RepositoryException e) {
            log.error("Error saving model", e);
            GlobalUtils.refreshSession(context, false);
        }
        return null;
    }

    private Node writeNode(final JcrModel model) {
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

        final PersistentHandler<PersistentNode, Node> nodeWriter = PersistentNode.ProcessAnnotation.NODE_WRITER;
        final Node jcrNode = nodeWriter.execute(context, model, node);
        if (jcrNode == null) {
            return null;
        }
        // process single properties:
        final PersistentHandler<PersistentProperty, Property> propWriter = PersistentProperty.ProcessAnnotation.PROPERTY_WRITER;
        final Collection<Field> fields = AnnotationUtils.getAnnotatedFields(model.getClass(), PersistentProperty.class);
        for (Field field : fields) {
            try {
                final Object value = field.get(model);
                if (value != null) {
                    final PersistentProperty property = field.getAnnotation(PersistentProperty.class);
                    final String name = property.name();
                    final JcrModel myModel = new SimplePropertyModel(value);
                    myModel.setParentPath(jcrNode.getPath());
                    myModel.setName(name);
                    // write single property:
                    propWriter.execute(context, myModel, property);
                }
            } catch (IllegalAccessException e) {
                log.error("Error processing value", e);
            } catch (RepositoryException e) {
                log.error("Error fetching parent path", e);
            }
        }
        // TODO write multi properties:
        // process kids:
        final List<JcrModel> children = model.getChildren();
        for (JcrModel child : children) {
            writeNode(child);
        }

        return jcrNode;
    }

}

