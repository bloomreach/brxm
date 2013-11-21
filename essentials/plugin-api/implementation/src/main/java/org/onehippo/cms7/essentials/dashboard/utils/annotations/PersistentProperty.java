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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.NotImplementedException;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.JcrModel;
import org.onehippo.cms7.essentials.dashboard.model.PersistentHandler;
import org.onehippo.cms7.essentials.dashboard.model.hst.SimplePropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        PROPERTY_WRITER;
        private static final Logger log = LoggerFactory.getLogger(ProcessAnnotation.class);

        @Override
        public Property execute(final PluginContext context, final JcrModel model, final PersistentProperty annotation) {

            final SimplePropertyModel ourModel = (SimplePropertyModel) model;
            final Object value = ourModel.getValue();
            final String name = ourModel.getName();
            final Session session = context.getSession();
            try {
                final String parentPath = ourModel.getParentPath();
                if(session.itemExists(parentPath)){
                    final Node node = session.getNode(parentPath);
                    if(value instanceof String){
                        node.setProperty(name, (String) value);
                    }else{
                        throw new NotImplementedException("Property writer not implemented for: " + value.getClass());
                    }

                } else{
                    log.error("Parent couldn't be found for path: {}", parentPath);
                }
            } catch (RepositoryException e) {
                log.error("Error writing property", e);
            }

            return null;

        }
    }
}
