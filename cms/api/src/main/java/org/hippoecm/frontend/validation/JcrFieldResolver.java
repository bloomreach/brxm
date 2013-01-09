/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.validation;

import java.util.Iterator;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.ChildNodeProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.PropertyValueProvider;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a model from a container model, using a {@link ModelPath}.
 */
public class JcrFieldResolver implements IFieldResolver {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrFieldResolver.class);

    public IModel resolve(IModel model, ModelPath path) throws EditorException {
        if (model instanceof JcrNodeModel) {
            JcrNodeModel nodeModel = (JcrNodeModel) model;
            Node node = nodeModel.getNode();
            for (ModelPathElement element : path.getElements()) {
                IFieldDescriptor field = element.getField();
                ITypeDescriptor fieldType = field.getTypeDescriptor();
                Iterator<? extends IModel> iter;
                if (fieldType.isNode()) {
                    ChildNodeProvider provider = new ChildNodeProvider(field, null, nodeModel.getItemModel());
                    iter = provider.iterator(element.getIndex(), 1);
                } else {
                    PropertyValueProvider provider = new PropertyValueProvider(field, null, nodeModel.getItemModel());
                    iter = provider.iterator(element.getIndex(), 1);
                }
                if (iter.hasNext()) {
                    if (fieldType.isNode()) {
                        JcrNodeModel childModel = (JcrNodeModel) iter.next();
                        node = childModel.getNode();
                    } else {
                        return iter.next();
                    }
                } else {
                    throw new EditorException("Field is not available in model");
                }
            }
            return new JcrNodeModel(node);
        } else {
            throw new EditorException("Unknown model type");
        }
    }

}
