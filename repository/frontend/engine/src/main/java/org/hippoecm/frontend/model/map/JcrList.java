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
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrList extends AbstractList<IHippoMap> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrList.class);

    private Node item;
    private String name;

    public JcrList(Node node, String name) {
        this.item = node;
        this.name = name;
    }

    @Override
    public IHippoMap get(int index) {
        try {
            return new JcrMap(item.getNode(name + "[" + (index + 1) + "]"));
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public int size() {
        try {
            return (int) item.getNodes(name).getSize();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return 0;
    }

    @Override
    public void add(int index, IHippoMap element) {
        try {
            Node child = item.addNode(name, element.getPrimaryType());
            for (String mixin : element.getMixinTypes()) {
                child.addMixin(mixin);
            }
            JcrMap map = new JcrMap(child);
            map.putAll(element);

            if (item.getPrimaryNodeType().hasOrderableChildNodes() && (index < (size() - 1))) {
                Node predecessor = item.getNode(name + "[" + (index + 1) + "]");
                item.orderBefore(name + "[" + child.getIndex() + "]", name + "[" + predecessor.getIndex() + "]");
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public IHippoMap remove(int index) {
        // FIXME: make deep copy of node, before it's removed
        IHippoMap current = get(index);
        try {
            item.getNode(name + "[" + (index + 1) + "]").remove();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return current;
    }

}
