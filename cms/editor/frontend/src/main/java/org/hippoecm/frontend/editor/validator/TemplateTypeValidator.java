/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.editor.validator;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.validation.ModelPath;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator for the hipposysedit:templatetype node type.  It verifies that the same child item
 * path is not used more than once.
 */
public class TemplateTypeValidator implements ITypeValidator {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TemplateTypeValidator.class);

    public final static String PATH_USED_MULTIPLE_TIMES = "path-is-used-multiple-times";
    
    public Set<Violation> validate(IModel model) throws ValidationException {
        Set<Violation> violations = new HashSet<Violation>();
        try {
            Node node = (Node) model.getObject();
            if (node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                NodeIterator ntNodes = node.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE).getNodes(
                        HippoNodeType.HIPPOSYSEDIT_NODETYPE);
                Node ntNode = null;
                while (ntNodes.hasNext()) {
                    Node child = ntNodes.nextNode();
                    if (!child.isNodeType(HippoNodeType.NT_REMODEL)) {
                        ntNode = child;
                        break;
                    }
                }
                if (ntNode != null) {
                    HashSet<String> paths = new HashSet<String>();
                    NodeIterator fieldIter = ntNode.getNodes();
                    while (fieldIter.hasNext()) {
                        Node field = fieldIter.nextNode();
                        if (!field.isNodeType(HippoNodeType.NT_FIELD)) {
                            continue;
                        }
                        String path = field.getProperty(HippoNodeType.HIPPO_PATH).getString();
                        if (paths.contains(path)) {
                            // TODO: add actual paths
                            violations.add(new Violation(new HashSet<ModelPath>(), PATH_USED_MULTIPLE_TIMES, null));
                        }
                        if (!path.equals("*")) {
                            paths.add(path);
                        }
                    }
                } else {
                    throw new ValidationException("Draft nodetype not found");
                }
            } else {
                throw new ValidationException("Unknown node type " + node.getPrimaryNodeType().getName());
            }
        } catch (RepositoryException ex) {
            throw new ValidationException("Exception while validating template", ex);
        }
        return violations;
    }
}
