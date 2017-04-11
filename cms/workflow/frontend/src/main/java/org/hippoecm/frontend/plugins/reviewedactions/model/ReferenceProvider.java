/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider of links of a document to other documents.
 *
 * @deprecated use {@link org.hippoecm.frontend.plugins.reviewedactions.UnpublishedReferenceNodeProvider}
 */
@Deprecated
public class ReferenceProvider extends NodeModelWrapper implements IDataProvider<String> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ReferenceProvider.class);
    
    private transient List<String> entries;

    public ReferenceProvider(IModel<Node> nodeModel) {
        super(nodeModel);
    }

    @Override
    public Iterator<String> iterator(long first, long count) {
        load();
        return entries.subList((int) first, (int) (first + count)).iterator();
    }

    @Override
    public IModel<String> model(String object) {
        return new Model<String>(object);
    }

    @Override
    public long size() {
        load();
        return entries.size();
    }

    @Override
    public void detach() {
        entries = null;
        super.detach();
    }

    protected void load() {
        if (entries == null) {
            try {
                entries = new ArrayList<String>();
                Node node = getNode();
                if (node != null) {
                    node.accept(new ItemVisitor() {

                        public void visit(Property property) throws RepositoryException {
                            if (property.getType() == PropertyType.STRING) {
                                String uuid = property.getString();
                                entries.add(uuid);
                            }
                        }

                        public void visit(Node node) throws RepositoryException {
                            if (!JcrHelper.isVirtualNode(node)) {
                                if (node.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                                    visit(node.getProperty(HippoNodeType.HIPPO_DOCBASE));
                                }
                                for (NodeIterator children = node.getNodes(); children.hasNext();) {
                                    visit(children.nextNode());
                                }
                            }
                        }
                        
                    });
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }
    
}
