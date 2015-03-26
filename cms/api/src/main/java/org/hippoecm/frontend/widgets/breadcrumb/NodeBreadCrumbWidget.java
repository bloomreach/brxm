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
package org.hippoecm.frontend.widgets.breadcrumb;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;

public abstract class NodeBreadCrumbWidget extends BreadCrumbWidget<Node> {

    public NodeBreadCrumbWidget(final String id, IModel<Node> model, final String... roots) {
        super(id, new NodeBreadCrumbModel(model, roots));
    }

    public void update(IModel<Node> newModel) {
        getModel().update(newModel);
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            target.add(this);
        }
    }

    private NodeBreadCrumbModel getModel() {
        return (NodeBreadCrumbModel) getDefaultModel();
    }

    private static class NodeBreadCrumbModel extends BreadCrumbModel<Node> {

        private final Set<String> rootPaths = new HashSet<>();

        public NodeBreadCrumbModel(IModel<Node> model, final String... roots) {
            if (roots == null || roots.length == 0) {
                rootPaths.add("/");
            } else {
                Collections.addAll(rootPaths, roots);
            }
            update(model);
        }

        public void update(IModel<Node> nodeModel) {
            if (nodeModel == null) {
                return;
            }

            final Node node = nodeModel.getObject();
            if (node == null) {
                return;
            }

            final String path = JcrUtils.getNodePathQuietly(node);
            if (path == null) {
                return;
            }

            if (valid(path)) {
                List<BreadCrumb<Node>> list = getObject();
                list.clear();

                list.add(0, new NodeBreadCrumb(nodeModel, false));

                if (!rootPaths.contains(path)) {
                    JcrNodeModel parentModel = getParentJcrNodeModel(nodeModel);
                    while (parentModel != null) {
                        list.add(0, new NodeBreadCrumb(parentModel, true));
                        if (rootPaths.contains(parentModel.getItemModel().getPath())) {
                            parentModel = null;
                        } else {
                            parentModel = parentModel.getParentModel();
                        }
                    }
                }
            }
        }

        private JcrNodeModel getParentJcrNodeModel(final IModel<Node> nodeModel) {
            JcrNodeModel jcrNodeModel  = nodeModel instanceof JcrNodeModel ? (JcrNodeModel) nodeModel :
                    new JcrNodeModel(nodeModel.getObject());
            return jcrNodeModel.getParentModel();
        }

        private boolean valid(final String path) {
            boolean valid = false;
            for (String root : rootPaths) {
                if (path.startsWith(root)) {
                    valid = true;
                    break;
                }
            }
            return valid;
        }
    }

    private static class NodeBreadCrumb extends BreadCrumb<Node> {

        public NodeBreadCrumb(final IModel<Node> model, final boolean enabled) {
            super(model, enabled);
        }

        @Override
        public String getName() {
            final IModel<Node> nodeModel = getModel();
            final String name = new NodeTranslator(nodeModel).getNodeName().getObject();
            if (StringUtils.isEmpty(name)) {
                String path = JcrUtils.getNodePathQuietly(nodeModel.getObject());
                if ("/".equals(path)) {
                    return "/";
                }
                return null;
            }
            return NodeNameCodec.decode(name);
        }
    }
}
