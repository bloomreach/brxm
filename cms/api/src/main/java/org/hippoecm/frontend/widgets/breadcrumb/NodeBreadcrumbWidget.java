/*
 *  Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.widgets.breadcrumb;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeNameModel;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;

public abstract class NodeBreadcrumbWidget extends BreadcrumbWidget<Node> {

    public NodeBreadcrumbWidget(final String id, final IModel<Node> model, final String... roots) {
        super(id, new NodeBreadcrumbModel(model, roots));
    }

    public void update(final IModel<Node> newModel) {
        getNodeBreadcrumbModel().update(newModel);
        final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            target.add(this);
        }
    }

    @Override
    protected IModel<String> getName(final IModel<Node> model) {
        final String name = new NodeNameModel(model).getObject();
        if (StringUtils.isEmpty(name)) {
            final String path = JcrUtils.getNodePathQuietly(model.getObject());
            if ("/".equals(path)) {
                return Model.of("/");
            }
            return null;
        }
        return Model.of(NodeNameCodec.decode(name));
    }

    private NodeBreadcrumbModel getNodeBreadcrumbModel() {
        return (NodeBreadcrumbModel) getModel();
    }

    private static class NodeBreadcrumbModel extends LoadableDetachableModel<List<IModel<Node>>> {

        private final HashSet<String> rootPaths = new HashSet<>();
        private IModel<Node> model;

        NodeBreadcrumbModel(final IModel<Node> model, final String... roots) {
            this.model = model;

            if (roots == null || roots.length == 0) {
                rootPaths.add("/");
            } else {
                Collections.addAll(rootPaths, roots);
            }
        }

        @Override
        protected void onDetach() {
            if (model != null) {
                model.detach();
            }
            super.onDetach();
        }

        @Override
        protected List<IModel<Node>> load() {
            if (model == null) {
                return null;
            }

            final Node node = model.getObject();
            if (node == null) {
                return null;
            }

            final String path = JcrUtils.getNodePathQuietly(node);
            if (path == null) {
                return null;
            }

            if (!valid(path)) {
                return null;
            }

            final List<IModel<Node>> items = new LinkedList<>();
            items.add(0, model);

            if (!rootPaths.contains(path)) {
                JcrNodeModel parentModel = getParentJcrNodeModel(model);
                while (parentModel != null) {
                    items.add(0, parentModel);
                    if (rootPaths.contains(parentModel.getItemModel().getPath())) {
                        parentModel = null;
                    } else {
                        parentModel = parentModel.getParentModel();
                    }
                }
            }
            return items;
        }

        void update(final IModel<Node> model) {
            this.model = model;
        }

        private JcrNodeModel getParentJcrNodeModel(final IModel<Node> nodeModel) {
            final JcrNodeModel jcrNodeModel = nodeModel instanceof JcrNodeModel ? (JcrNodeModel) nodeModel :
                    new JcrNodeModel(nodeModel.getObject());
            return jcrNodeModel.getParentModel();
        }

        private boolean valid(final String path) {
            boolean valid = false;
            for (final String root : rootPaths) {
                if (path.startsWith(root)) {
                    valid = true;
                    break;
                }
            }
            return valid;
        }
    }
}
