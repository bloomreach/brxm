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

package org.onehippo.cms7.essentials.dashboard.utils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.NotImplementedException;
import org.onehippo.cms7.essentials.dashboard.config.Document;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.PersistentHandler;
import org.onehippo.cms7.essentials.dashboard.model.hst.SimplePropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * Processes JCR property item
 *
 * @version "$Id$"
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface PersistentProperty {

    String name();

    enum ProcessAnnotation implements PersistentHandler<PersistentProperty, Property> {
        PROPERTY {
            @Override
            public Property read(final PluginContext context, final Node parent, final String path, final PersistentProperty annotation) {
                try {
                    if (parent == null) {
                        log.error("Parent node was null for path: {}", path);
                        return null;
                    }
                    if (Strings.isNullOrEmpty(path)) {
                        log.error("Path was null for parent: {}", parent.getPath());
                        return null;
                    }
                    if (!parent.hasProperty(path)) {

                        return null;
                    }
                    return parent.getProperty(path);
                } catch (RepositoryException e) {
                    log.error("Error loading property", e);
                }


                return null;
            }
        };
        private static final Logger log = LoggerFactory.getLogger(ProcessAnnotation.class);

        @Override
        public Property execute(final PluginContext context, final Document model, final PersistentProperty annotation) {

            final SimplePropertyModel ourModel = (SimplePropertyModel) model;
            final Object value = ourModel.getValue();
            final String name = ourModel.getName();
            final Session session = context.createSession();
            try {
                final String parentPath = ourModel.getParentPath();
                if (session.itemExists(parentPath)) {
                    final Node node = session.getNode(parentPath);
                    if (value instanceof String) {
                        node.setProperty(name, (String) value);
                    } else if (value instanceof Boolean) {
                        node.setProperty(name, (Boolean) value);
                    } else if (value instanceof Integer) {
                        node.setProperty(name, (Integer) value);
                    } else if (value instanceof Long) {
                        node.setProperty(name, (Long) value);
                    } else if (value instanceof Double) {
                        node.setProperty(name, (Double) value);
                    } else if (value instanceof Calendar) {
                        node.setProperty(name, (Calendar) value);
                    } else {
                        throw new NotImplementedException("Property writer not implemented for: " + value.getClass());
                    }

                    return node.getProperty(name);
                } else {
                    log.error("Parent couldn't be found for path: {}", parentPath);
                }
            } catch (RepositoryException e) {
                log.error("Error writing property", e);
            }

            return null;

        }
    }
}
