/*
 * Copyright 2010-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.contentblocks.model;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.compare.IComparer;
import org.hippoecm.frontend.editor.compare.NodeComparer;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentBlockComparer implements IComparer<Node> {
    private static final Logger log = LoggerFactory.getLogger(ContentBlockComparer.class);

    private final ITemplateEngine engine;

    public ContentBlockComparer(final ITemplateEngine engine) {
        this.engine = engine;
    }

    public boolean areEqual(final Node baseNode, final Node targetNode) {
        if (baseNode == null || targetNode == null) {
            return baseNode == null && targetNode == null;
        }

        try {
            final String basePrimaryNodeTypeName = baseNode.getPrimaryNodeType().getName();
            final String targetPrimaryNodeTypeName = targetNode.getPrimaryNodeType().getName();
            if (!basePrimaryNodeTypeName.equals(targetPrimaryNodeTypeName)) {
                return false;
            }

            final ITypeDescriptor type = engine.getType(basePrimaryNodeTypeName);
            final NodeComparer comparer = new NodeComparer(type, engine);
            return comparer.areEqual(baseNode, targetNode);
        } catch (final RepositoryException | TemplateEngineException ex) {
            log.error(ex.getMessage(), ex);
            return false;
        }
    }

    public int getHashCode(final Node node) {
        if (node == null) {
            return 0;
        }

        try {
            final ITypeDescriptor type = engine.getType(node.getPrimaryNodeType().getName());
            final NodeComparer comparer = new NodeComparer(type, engine);
            return comparer.getHashCode(node);
        } catch (final RepositoryException | TemplateEngineException ex) {
            log.error(ex.getMessage(), ex);
        }
        return 0;
    }

}
