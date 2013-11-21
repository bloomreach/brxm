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

import javax.jcr.Item;
import javax.jcr.Node;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.JcrModel;
import org.onehippo.cms7.essentials.dashboard.model.PersistentHandler;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.AnnotationUtils;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import static org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentNode.*;

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
        final PersistentNode node = AnnotationUtils.getClassAnnotation(model.getClass(), PersistentNode.class);
        if (node == null) {
            log.error("No @PersistentNode annotation found for object: {}", model);
            return null;
        }

        final String type = node.type();
        if(Strings.isNullOrEmpty(type)){
            log.error("@PersistentNode type must have a value but was empty");
            return null;
        }

        PersistentHandler<PersistentNode, Node> handler = ProcessAnnotation.INSTANCE;
        return handler.execute(context, model, node);
    }
}
