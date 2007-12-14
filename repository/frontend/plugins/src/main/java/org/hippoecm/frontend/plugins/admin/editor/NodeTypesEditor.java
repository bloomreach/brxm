/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.admin.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListItemModel;
import org.apache.wicket.markup.html.list.ListView;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NodeTypesEditor extends CheckGroup {
    private static final long serialVersionUID = 1L;
    
    static final Logger log = LoggerFactory.getLogger(NodeTypesEditor.class);

    private ArrayList<String> current;
    
    public NodeTypesEditor(String id, JcrNodeTypesProvider provider) {
        super(id, new ArrayList<String>());

        current = new ArrayList<String>();

        setProvider(provider);

        add(new ListView("type", getAllNodeTypes()) {
            private static final long serialVersionUID = 1L;

            protected void populateItem(ListItem item) {
                ListItemModel model = (ListItemModel) item.getModel();

                String type = (String) model.getObject();

                Check check = new Check("check", model); 
                item.add(check);

                check.add(new Label("name", type));
            }
        });
    }
    
    protected abstract void onAddNodeType(String name);

    protected abstract void onRemoveNodeType(String name);

    @Override
    protected void onSelectionChanged(Collection newSelection) {
        Iterator<String> iterator = newSelection.iterator();
        Set<String> removed = new HashSet<String>(current);
        while (iterator.hasNext()) {
            String type = iterator.next();
            if (removed.contains(type)) {
                removed.remove(type);
            }
            else {
                onAddNodeType(type);
                current.add(type);
            }
        }

        iterator = removed.iterator();
        while (iterator.hasNext()) {
            String type = iterator.next();
            onRemoveNodeType(type);
            current.remove(type);
        }
    }

    @Override
    protected boolean wantOnSelectionChangedNotifications() {
        return true;
    }

    public void setProvider(JcrNodeTypesProvider provider) {
        current.clear();
        Iterator<NodeType> types = provider.iterator(0, provider.size());
        while(types.hasNext()) {
            NodeType type = types.next();
            current.add(type.getName());
        }
        ArrayList<String> selected = (ArrayList<String>) getModelObject();
        selected.clear();
        selected.addAll(current);
    }

    private List<String> getAllNodeTypes() {
        List<String> list = new ArrayList<String>();
        try {
            UserSession session = (UserSession) getSession();
            NodeTypeManager ntmgr = session.getJcrSession().getWorkspace().getNodeTypeManager();
            NodeTypeIterator iterator = ntmgr.getMixinNodeTypes();
            while(iterator.hasNext()) {
                list.add(iterator.nextNodeType().getName());
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return list;
    }
}
