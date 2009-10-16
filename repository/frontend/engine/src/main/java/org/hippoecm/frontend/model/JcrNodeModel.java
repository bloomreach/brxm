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
package org.hippoecm.frontend.model;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.JcrEventListener;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrNodeModel extends ItemModelWrapper implements IObservable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrNodeModel.class);

    private IObservationContext context;
    private JcrEventListener listener;
    private transient boolean parentCached = false;
    private transient JcrNodeModel parent;

    public JcrNodeModel(JcrItemModel model) {
        super(model);
    }

    public JcrNodeModel(Node node) {
        super(node);
    }

    public JcrNodeModel(String path) {
        super(path);
    }

    public Node getNode() {
        Node node = (Node)itemModel.getObject();
        try {
            if (node != null) {
                node.getParent();
            }
            return node;
        } catch (RepositoryException ex) {
            itemModel.detach();
            return (Node)itemModel.getObject();
        }
    }

    public JcrNodeModel getParentModel() {
        if (!parentCached) {
            JcrItemModel parentModel = itemModel.getParentModel();
            if (parentModel != null) {
                parent = new JcrNodeModel(parentModel);
            } else {
                parent = null;
            }
            parentCached = true;
        }
        return parent;
    }

    @Override
    public void detach() {
        parent = null;
        parentCached = false;
        super.detach();
    }

    public boolean isVirtual() {
        Node node = getNode();
        if (node == null || !(node instanceof HippoNode)) {
            return false;
        }
        try {
            HippoNode hippoNode = (HippoNode) node;
            Node canonical = hippoNode.getCanonicalNode();
            if (canonical == null) {
                return true;
            }
            return !canonical.isSame(hippoNode);
        } catch (ItemNotFoundException e) {
            // canonical node no longer exists
            return true;
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    // implement IObservable

    public void setObservationContext(IObservationContext context) {
        this.context = context;
    }

    public void startObservation() {
        if (itemModel.getObject() == null) {
            log.warn("skipping observation for null node");
            return;
        }
        if (itemModel.getRelativePath() == null) {
            listener = new JcrEventListener(context, Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED
                    | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED, "/", true,
                    new String[] { itemModel.getUuid() }, null);
        } else {
            listener = new JcrEventListener(context, Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED
                    | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED, itemModel.getPath(), false, null, null);
        }
        listener.start();
    }

    public void stopObservation() {
        if (listener != null) {
            listener.stop();
            listener = null;
        }
    }

    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("itemModel", itemModel.toString())
                .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrNodeModel == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrNodeModel nodeModel = (JcrNodeModel) object;
        return itemModel.equals(nodeModel.getItemModel());
        //return new EqualsBuilder().append(itemModel, nodeModel.itemModel).isEquals();
    }

    @Override
    public int hashCode() {
        return itemModel.hashCode();
        //return new HashCodeBuilder(57, 433).append(itemModel).toHashCode();
    }

}
