/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.util.JcrUtils.getNodePathQuietly;

public class NodeNameModel extends LoadableDetachableModel<String> implements IObservable {

    private static final Logger log = LoggerFactory.getLogger(NodeNameModel.class);

    private final IModel<Node> nodeModel;
    private IObservationContext<NodeNameModel> context;
    private IObserver observer;

    public NodeNameModel(IModel<Node> nodeModel) {
        this.nodeModel = nodeModel;
    }

    @Override
    public void detach() {
        nodeModel.detach();
        super.detach();
    }

    @Override
    protected String load() {
        if (nodeModel != null) {
            HippoNode node = (HippoNode) nodeModel.getObject();
            if (node != null) {
                try {
                    if (!node.isNodeType(HippoNodeType.NT_NAMED) &&
                            node.isNodeType(HippoNodeType.NT_DOCUMENT) &&
                            node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                        node = (HippoNode)node.getParent();
                    }
                    return node.getDisplayName();
                } catch (RepositoryException e) {
                    log.error("Failed to load display name of node {}", getNodePathQuietly(node), e);
                }
            } else if (nodeModel instanceof JcrNodeModel) {
                String path = ((JcrNodeModel ) nodeModel).getItemModel().getPath();
                if (path != null) {
                    String name = path.substring(path.lastIndexOf('/') + 1);
                    if (name.indexOf('[') > 0) {
                        name = name.substring(0, name.indexOf('['));
                    }
                    name = NodeNameCodec.decode(name);
                    return name;
                }

            }
        }
        return null;
    }

    @Override
    public void setObservationContext(final IObservationContext<? extends IObservable> context) {
        this.context = (IObservationContext<NodeNameModel>) context;
    }

    @Override
    public void startObservation() {
        if (nodeModel instanceof IObservable) {
            context.registerObserver(observer = new IObserver<IObservable>() {
                @Override
                public IObservable getObservable() {
                    return (IObservable) nodeModel;
                }

                @Override
                public void onEvent(final Iterator events) {
                    final IEvent<NodeNameModel> event = new IEvent<NodeNameModel>() {
                        public NodeNameModel getSource() {
                            return NodeNameModel.this;
                        }

                    };
                    final EventCollection<IEvent<NodeNameModel>> collection = new EventCollection<>();
                    collection.add(event);
                    context.notifyObservers(collection);
                }
            });
        }
    }

    @Override
    public void stopObservation() {
        if (observer != null) {
            context.unregisterObserver(observer);
        }
    }
}
