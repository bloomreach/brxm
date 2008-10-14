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

import java.util.regex.Pattern;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoNode;

class Reference extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id:  $";
    private static final long serialVersionUID = 1L;
    
    private static Pattern pattern = Pattern.compile("^\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}$");
    
    public Reference(String id, JcrPropertyModel propertyModel, JcrPropertyValueModel valueModel){
        super(id);
        String asString = "";
        try {
            boolean isProtected = propertyModel.getProperty().getDefinition().isProtected();
            Session session = ((UserSession) getSession()).getJcrSession();
            asString  = valueModel.getValue().getString();
            Node targetNode = session.getNodeByUUID(asString);
            if (targetNode instanceof HippoNode) {
                final JcrNodeModel targetModel = new JcrNodeModel(targetNode);
                AjaxLink link = new AjaxLink("reference-link") {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void onClick(AjaxRequestTarget requestTarget) {
                        EditorPlugin plugin = (EditorPlugin) findParent(EditorPlugin.class);
                        plugin.setModel(targetModel);
                    }
                };
                add(link);
                link.add(new Label("reference-link-text", new Model (targetNode.getPath())));

                if (isProtected) {
                    add(new Label("reference-edit", asString));
                } else {
                    TextFieldWidget editor = new TextFieldWidget("reference-edit", new Model(asString));
                    editor.setSize("40");
                    add(editor);            
                }
            } else {
                add(new Label("reference-edit", asString));
                AjaxLink link = nopLink();
                add(link);
                link.setEnabled(false);
                Label label = new Label("reference-link-text", "(" + targetNode.getClass().getName() + ")");
                link.add(label);
            }
        } catch (ItemNotFoundException e) {
            AjaxLink link = nopLink();
            add(link);
            link.setEnabled(false);
            Label label = new Label("reference-link-text", "(Broken reference)");
            add(new AttributeAppender("style", new Model("color:red"), " "));
            link.add(label);
            TextFieldWidget editor = new TextFieldWidget("reference-edit", new Model(asString));
            editor.setSize("40");
            add(editor);          
        } catch (RepositoryException e) {
            add(new Label("reference-edit", e.getClass().getName() + ":" + e.getMessage()));
            AjaxLink link = nopLink();
            add(link);
            link.add(new Label("reference-link-text", ""));
            link.setVisible(false);
        }
    }

    static boolean isUid(JcrPropertyValueModel valueModel) throws RepositoryException {
        String asString = valueModel.getValue().getString();
        return pattern.matcher(asString).matches();
    }
    
    private AjaxLink nopLink() {
        return new AjaxLink("reference-link") {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
            }                    
        };
    }


}
