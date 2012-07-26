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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNodeRenderer implements IListCellRenderer<Node> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(AbstractNodeRenderer.class);

    public Component getRenderer(String id, IModel<Node> model) {
        if (model instanceof JcrNodeModel) {
            try {
                Node node = (Node) model.getObject();
                if (node != null) {
                    return getViewer(id, node);
                } else {
                    log.warn("Cannot render a null node");
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return new Label(id);
    }

    public IObservable getObservable(final IModel<Node> model) {
        if (model instanceof IObservable) {
            return (IObservable) model;
        }
        return null;
    }
    
    protected abstract Component getViewer(String id, Node node) throws RepositoryException;

}
