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
package org.hippocms.repository.frontend;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.WebPage;
import org.hippocms.repository.frontend.dialog.DialogWindow;
import org.hippocms.repository.frontend.dialog.DynamicDialogCreator;
import org.hippocms.repository.frontend.editor.EditorPanel;
import org.hippocms.repository.frontend.menu.Menu;
import org.hippocms.repository.frontend.model.JcrNodeModel;
import org.hippocms.repository.frontend.tree.TreePanel;
import org.hippocms.repository.frontend.update.UpdateManager;

public class Browser extends WebPage {
    private static final long serialVersionUID = 1L;
    
    private EditorPanel editorPanel;
    private TreePanel treePanel;
    private Menu menu;

    public Browser() throws RepositoryException {
        BrowserSession session = (BrowserSession)getSession();
        UpdateManager updateManager = session.getUpdateManager();
 
        Node root = session.getJcrSession().getRootNode();
        JcrNodeModel model = new JcrNodeModel(root);

        treePanel = new TreePanel("treePanel", model);
        updateManager.addUpdatable(treePanel);
        add(treePanel);
        
        editorPanel = new EditorPanel("editorPanel", model);
        updateManager.addUpdatable(editorPanel);
        add(editorPanel);
        
        menu = new Menu("menu", model); 
        updateManager.addUpdatable(menu);
        add(menu);
        
        
        DialogWindow dynamicDialog = new DialogWindow("dynamic-dialog", model);
        dynamicDialog.setPageCreator(new DynamicDialogCreator(dynamicDialog, model));
        add(dynamicDialog);
        add(dynamicDialog.dialogLink("dynamic-dialog-link"));
    }

}
