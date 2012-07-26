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
package org.hippoecm.frontend.model.map;

import java.util.AbstractList;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrList extends AbstractList<IHippoMap> implements IDetachable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrList.class);

    private String path;
    private String name;
    private transient Node node;

    public JcrList(JcrNodeModel nodeModel, String name) {
        this.path = nodeModel.getItemModel().getPath();
        this.name = name;
    }

    protected Node getNode() throws PathNotFoundException, RepositoryException {
        if (node == null) {
            UserSession sessionProvider = (UserSession) Session.get();
            node = (Node) sessionProvider.getJcrSession().getItem(path);
        }
        return node;
    }

    @Override
    public IHippoMap get(int index) {
        try {
            return new JcrMap(new JcrNodeModel(getNode().getNode(name + (index + 1 > 1 ? "[" + (index + 1) + "]" : ""))));
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public IHippoMap set(int index, IHippoMap element) {
        IHippoMap previous = remove(index);
        add(index, element);
        return previous;
    }

    @Override
    public int size() {
        try {
            return (int) getNode().getNodes(name).getSize();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return 0;
    }

    @Override
    public void add(int index, IHippoMap element) {
        try {
            Node child = getNode().addNode(name, element.getPrimaryType());
            for (String mixin : element.getMixinTypes()) {
                child.addMixin(mixin);
            }
            JcrMap map = new JcrMap(new JcrNodeModel(child));
            map.putAll(element);

            Node node = getNode();
            if (node.getPrimaryNodeType().hasOrderableChildNodes() && (index < (size() - 1))) {
                Node predecessor = node.getNode(name + (index + 1 > 1 ? "[" + (index + 1) + "]" : ""));
                node.orderBefore(name + (child.getIndex() > 1 ? "[" + child.getIndex() + "]" : ""), name + (predecessor.getIndex() > 1 ? "[" + predecessor.getIndex() + "]" : ""));
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    @Override
    public IHippoMap remove(int index) {
        IHippoMap current = new HippoMap(get(index));
        try {
            getNode().getNode(name + (index + 1 > 1 ? "[" + (index + 1) + "]" : "")).remove();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return current;
    }

    public void detach() {
        node = null;
    }

}
