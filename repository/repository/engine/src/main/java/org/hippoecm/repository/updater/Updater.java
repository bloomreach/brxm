/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.updater;

import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

public class Updater {
    Session session;

    public Updater(Session session) {
        this.session = session;
    }

    public void accept(ItemVisitor visitor) throws RepositoryException {
        UpdaterSession updaterSession = new UpdaterSession(session);
        updaterSession.getRootNode().accept(visitor);
        updaterSession.getRootNode().accept(new Cleaner());
        updaterSession.commit();
    }

    public static NodeType getNewType(Session session, String type) throws RepositoryException {
        return ((UpdaterSession) session).getNewType(type);
    }

    public static void setName(Item item, String name) throws RepositoryException {
        ((UpdaterItem) item).setName(name);
    }

    public static NodeType[] getNodeTypes(Node node) throws RepositoryException {
        return ((UpdaterNode) node).getNodeTypes();
    }

    public static boolean isMultiple(Property property) {
        return ((UpdaterProperty) property).isMultiple();
    }

    private class Cleaner extends UpdaterItemVisitor.Converted {

        void update(UpdaterSession session) throws RepositoryException {
            session.getRootNode().accept(this);
        }

        @Override
        public void entering(Node visit, int level) throws RepositoryException {
            UpdaterNode node = (UpdaterNode) visit;
            NodeType[] nodeTypes = node.getNodeTypes();
            for(PropertyIterator iter = node.getProperties(); iter.hasNext(); ) {
                    Property property = iter.nextProperty();
                    boolean isValid = false;
                    for (int i = 0; i < nodeTypes.length; i++) {
                        if (Updater.isMultiple(property)) {
                            if (nodeTypes[i].canSetProperty(property.getName(), property.getValues())) {
                                isValid = true;
                                break;
                            }
                        } else {
                            if (nodeTypes[i].canSetProperty(property.getName(), property.getValue())) {
                                isValid = true;
                                break;
                            }
                        }
                    }
                    if (!isValid) {
                        property.remove();
                    }
                }
            }
        }
    }

