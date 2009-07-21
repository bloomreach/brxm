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

package org.hippoecm.hst.plugins.frontend.editor.validators;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.validation.IValidatable;
import org.hippoecm.hst.plugins.frontend.editor.domain.BeanProvider;
import org.hippoecm.hst.plugins.frontend.editor.domain.EditorBean;

public class NodeUniqueValidator<K extends EditorBean> extends EditorValidator<K> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public NodeUniqueValidator(BeanProvider<K> provider) {
        super(provider);
    }

    @Override
    protected void onValidate(IValidatable validatable, K bean) {
        String newNodeName = getNewNodeName(validatable, bean);
        Node node = bean.getModel().getNode();
        try {
            if (newNodeName.equals(node.getName())) {
                return;
            }
            if (node.getParent().hasNode(newNodeName)) {
                error("node-unique-validator.error");
            }
        } catch (RepositoryException e) {
            log.error("Error validating node name", e);
        }
    }

    protected String getNewNodeName(IValidatable validatable, K bean) {
        return (String) validatable.getValue();
    }

}
