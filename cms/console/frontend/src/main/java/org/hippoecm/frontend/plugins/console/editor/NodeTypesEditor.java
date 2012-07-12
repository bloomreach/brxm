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
package org.hippoecm.frontend.plugins.console.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NodeTypesEditor extends CheckGroup<String> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(NodeTypesEditor.class);

    private IModel<Node> nodeModel;
    private final Collection<String> inheritedMixinTypes = new HashSet<String>();

    NodeTypesEditor(String id, IModel<Node> nodeModel) {
        super(id, Collections.<String>emptyList());
        this.nodeModel = nodeModel;

        add(new ListView<String>("type", getAllNodeTypes()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<String> item) {
                IModel<String> model = item.getModel();

                Check<String> check = new Check<String>("check", model);
                item.add(check);

                String type = model.getObject();
                check.add(new Label("name", type));

                check.setEnabled(!inheritedMixinTypes.contains(type));
            }
        });
    }

    @Override
    protected boolean wantOnSelectionChangedNotifications() {
        return true;
    }

    @Override
    protected void onSelectionChanged(Collection<? extends String> selection) {
        if (getModelObject() instanceof List && nodeModel != null) {
            try {
                final Node node = nodeModel.getObject();

                final Set<String> actualTypes = new HashSet<String>();
                if (node.hasProperty("jcr:mixinTypes")) {
                    Value[] nodeTypes = node.getProperty("jcr:mixinTypes").getValues();
                    for (Value nodeType : nodeTypes) {
                        actualTypes.add(nodeType.getString());
                    }
                }

                final Set<String> toBeAdded = new HashSet<String>(selection);
                toBeAdded.removeAll(actualTypes);
                for (String add : toBeAdded) {
                    node.addMixin(add);
                }

                final Set<String> toBeRemoved = new HashSet<String>(actualTypes);
                toBeRemoved.removeAll(selection);
                for (String remove : toBeRemoved) {
                    node.removeMixin(remove);
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    // (package) privates

    private List<String> getAllNodeTypes() {
        List<String> list = new ArrayList<String>();
        try {
            UserSession session = UserSession.get();
            NodeTypeManager ntmgr = session.getJcrSession().getWorkspace().getNodeTypeManager();
            NodeTypeIterator iterator = ntmgr.getMixinNodeTypes();
            while (iterator.hasNext()) {
                list.add(iterator.nextNodeType().getName());
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        Collections.sort(list);
        return list;
    }

    void setNodeModel(IModel<Node> newModel) {
        nodeModel = newModel;
    }

    public void setInheritedMixinTypes(final Collection<String> inheritedMixinTypes) {
        this.inheritedMixinTypes.clear();
        this.inheritedMixinTypes.addAll(inheritedMixinTypes);
    }
}
