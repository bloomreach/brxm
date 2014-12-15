/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

/**
 * @version "$Id$"
 */
public class ContentBlockComparer implements IComparer<Node> {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ContentBlockComparer.class);

    private ITemplateEngine engine;
    
    public ContentBlockComparer(ITemplateEngine engine) {
        this.engine = engine;
    }
    
    public boolean areEqual(Node baseNode, Node targetNode) {
        if (baseNode == null && targetNode == null) {
            return true;
        } else if (baseNode == null || targetNode == null) {
            return false;
        }

        try {
            if (!baseNode.getPrimaryNodeType().getName().equals(targetNode.getPrimaryNodeType().getName())) {
                return false;
            }
            ITypeDescriptor type = engine.getType(baseNode.getPrimaryNodeType().getName());
            NodeComparer comparer = new NodeComparer(type);
            return comparer.areEqual(baseNode, targetNode);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
            return false;
        } catch (TemplateEngineException ex) {
            log.error(ex.getMessage(), ex);
            return false;
        }
    }

    public int getHashCode(Node node) {
        if (node == null) {
            return 0;
        }
        try {
            ITypeDescriptor type = engine.getType(node.getPrimaryNodeType().getName());
            NodeComparer comparer = new NodeComparer(type);
            return comparer.getHashCode(node);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        } catch (TemplateEngineException ex) {
            log.error(ex.getMessage(), ex);
        }
        return 0;
    }

}
