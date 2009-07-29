/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.tools.projectexport;

import java.util.Arrays;

import javax.swing.tree.TreeNode;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;

class NewContentItem extends Panel
{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ExportItem.java 18965 2009-07-23 07:16:15Z bvanhalderen $";

    String action = "include";
    String filename = "";

    private String[] choices = new String[] { "include", "exclude" };

    NewContentItem(MarkupContainer parent, String id, final ExportTreeModel tree, final TreeNode node, final Element.ContentElement element) {
        super(id);
        DropDownChoice folderChoice;
        Component filenameComponent;
        add(folderChoice = new DropDownChoice("action", new PropertyModel(this, "action"), Arrays.asList(choices)));
        add(new Label("path", ((Element.ContentElement) element).getPath()));
        add(filenameComponent = new TextField("filename", new PropertyModel(NewContentItem.this, "filename")));
        filenameComponent.add(new OnChangeAjaxBehavior() {
            public void onUpdate(AjaxRequestTarget target) {
                ((Element.ContentElement) element).file = filename;
            }
        });
        filenameComponent.setOutputMarkupId(true);
        folderChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            public void onUpdate(AjaxRequestTarget target) {
                if ("include".equals(action)) {
                    ((Element.ContentElement) tree.backingElement(node)).excluded = false;
                } else if ("exclude".equals(action)) {
                    ((Element.ContentElement) tree.backingElement(node)).excluded = true;
                }
            }
        });
        folderChoice.setNullValid(false);
        folderChoice.setRequired(true);
        folderChoice.setOutputMarkupId(true);
    }
}
