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
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.JcrModel;
import org.onehippo.cms7.essentials.dashboard.model.PersistentHandler;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * Processes JCR Node item
 *
 * @version "$Id$"
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface PersistentNode {

    /**
     * Represents primary node type   e.g. {@code foo:bar}
     *
     * @return primary node type name
     */
    String type();

    String[] mixins() default {"mix:referenceable", "hst:descriptive", "mix:simpleVersionable"};

    enum ProcessAnnotation implements PersistentHandler<PersistentNode, Node> {
        NODE_WRITER;
        private static final Logger log = LoggerFactory.getLogger(ProcessAnnotation.class);

        @Override
        public Node execute(final PluginContext context, final JcrModel model, final PersistentNode annotation) {
            log.info("Executing node persisting {}", model);
            final String parentPath = model.getParentPath();
            if (Strings.isNullOrEmpty(parentPath)) {
                log.error("Parent path was null for model: {}", model);
                return null;
            }
            final Session session = context.getSession();
            try {
                if (session.itemExists(parentPath)) {
                    final Node parent = session.getNode(parentPath);
                    final Node node = parent.addNode(model.getName(), annotation.type());
                    final String[] mixins = annotation.mixins();
                    for (String mixin : mixins) {
                        node.addMixin(mixin);
                    }
                    return node;
                } else {
                    log.error("Parent doesn't exist: {}", parentPath);
                }


            } catch (RepositoryException e) {
                log.error("Error saving model", e);
                GlobalUtils.refreshSession(session, false);
            }
            return null;
        }
    }
}
