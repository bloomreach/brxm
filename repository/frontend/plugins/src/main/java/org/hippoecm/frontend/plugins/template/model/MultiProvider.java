/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.template.model;

import java.util.LinkedList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.template.config.FieldDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides FieldModel instances based on a field descriptor and a
 * particular node.  Multiple models will be produced for same-name siblings.
 */
public class MultiProvider extends AbstractProvider implements IDataProvider {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(FieldProvider.class);

    private FieldDescriptor descriptor;

    public MultiProvider(FieldDescriptor descriptor, JcrNodeModel nodeModel) {
        super(nodeModel);
        this.descriptor = descriptor;
    }

    private void addNode(Node parent, int childIndex) {
        // create a new descriptor that is not multiple
        FieldDescriptor subDescriptor = descriptor.clone();

        // the subdescriptor identifies an entry in a series of fields. 
        subDescriptor.setMultiple(false);
        subDescriptor.setMandatory(true);

        FieldModel model = new FieldModel(subDescriptor, new JcrItemModel(parent));
        model.setIndex(childIndex);
        fields.addLast(model);
    }

    @Override
    protected void load() {
        if (fields != null) {
            return;
        }

        Node node = getNodeModel().getNode();
        fields = new LinkedList<FieldModel>();
        if (node != null) {
            try {
                NodeIterator iterator = node.getNodes(descriptor.getPath());
                while (iterator.hasNext()) {
                    Node child = iterator.nextNode();
                    addNode(node, child.getIndex());
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }
}
