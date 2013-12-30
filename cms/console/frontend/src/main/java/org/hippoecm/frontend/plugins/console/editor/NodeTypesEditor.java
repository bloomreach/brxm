/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NodeTypesEditor extends WebMarkupContainer {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(NodeTypesEditor.class);

    NodeTypesEditor(String id, IModel<Node> nodeModel) {
        super(id, nodeModel);
        setOutputMarkupId(true);

        add(new ListView<String>("type", getAllNodeTypes()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                IModel<String> model = item.getModel();
                String type = model.getObject();

                final IModel<Node> defaultModel = NodeTypesEditor.this.getModel();
                AjaxCheckBox check = new AjaxCheckBox("check", new MixinModel(defaultModel, type)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        target.add(NodeTypesEditor.this);
                    }

                    @Override
                    public boolean isEnabled() {
                        MixinModel mixinModel = (MixinModel) getModel();
                        return !mixinModel.isInherited();
                    }
                };
                item.add(check);

                check.add(new Label("name", type).add(new AttributeAppender("for", check.getMarkupId())));
            }
        });
    }

    public IModel<Node> getModel() {
        return (IModel<Node>) getDefaultModel();
    }

    public void setModel(final IModel<Node> model) {
        setDefaultModel(model);
    }

    @SuppressWarnings("unused")
    public String getMixinTypes() {
        Node node = (Node) getDefaultModelObject();
        try {
            final Collection<String> declaredMixinTypes = getDeclaredMixinTypes(node);
            final Collection<String> inheritedMixinTypes = getInheritedMixinTypes(node);
            final Collection<String> allMixinTypes = new ArrayList<>(declaredMixinTypes);
            allMixinTypes.addAll(inheritedMixinTypes);

            return StringUtils.join(allMixinTypes, ", ");
        } catch (RepositoryException re) {
            log.error(re.getMessage());
        }
        return "";
    }

    private Collection<String> getDeclaredMixinTypes(final Node node) throws RepositoryException {
        final List<String> result = new ArrayList<String>();
        for (NodeType nodeType : node.getMixinNodeTypes()) {
            result.add(nodeType.getName());
        }
        return result;
    }

    private Collection<String> getInheritedMixinTypes(final Node node) throws RepositoryException {
        final List<String> result = new ArrayList<String>();
        for (NodeType nodeType : node.getMixinNodeTypes()) {
            for (NodeType superType : nodeType.getSupertypes()) {
                if (superType.isMixin()) {
                    result.add(superType.getName());
                }
            }
        }
        for (NodeType nodeType : node.getPrimaryNodeType().getSupertypes()) {
            if (nodeType.isMixin()) {
                result.add(nodeType.getName());
            }
        }
        return result;
    }

    private List<String> getAllNodeTypes() {
        final List<String> result = new ArrayList<>();
        try {
            final Session session = UserSession.get().getJcrSession();
            final NodeTypeManager ntmgr = session.getWorkspace().getNodeTypeManager();
            final NodeTypeIterator iterator = ntmgr.getMixinNodeTypes();
            while (iterator.hasNext()) {
                result.add(iterator.nextNodeType().getName());
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        Collections.sort(result);
        return result;
    }

    private static class MixinModel extends NodeModelWrapper<Boolean> {

        private static final long serialVersionUID = 1L;

        private String type;

        private MixinModel(IModel<Node> nodeModel, String mixin) {
            super(nodeModel);
            this.type = mixin;
        }

        public Boolean getObject() {
            try {
                return isNodeType();
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            return false;
        }

        public void setObject(Boolean value) {
            try {
                Node node = getNode();
                if (node == null) {
                    throw new UnsupportedOperationException();
                }
                if (value) {
                    if (!isNodeType()) {
                        node.addMixin(type);
                    }
                } else {
                    if (isNodeType() && hasMixin()) {
                        node.removeMixin(type);
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }

        private boolean isNodeType() throws RepositoryException {
            Node node = getNode();
            if (node == null) {
                return false;
            }
            return node.isNodeType(type);
        }

        private boolean hasMixin() throws RepositoryException {
            Node node = getNode();
            if (node == null) {
                return false;
            }
            for (NodeType nodeType : node.getMixinNodeTypes()) {
                if (nodeType.getName().equals(type)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isInherited() {
            try {
                return isNodeType() && !hasMixin();
            } catch (RepositoryException re) {
                log.error(re.getMessage());
            }
            return false;
        }
    }

}
