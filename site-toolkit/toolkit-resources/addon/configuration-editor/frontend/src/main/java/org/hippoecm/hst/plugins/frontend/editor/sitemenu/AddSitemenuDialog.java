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

package org.hippoecm.hst.plugins.frontend.editor.sitemenu;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.dialogs.AddNodeDialog;
import org.hippoecm.hst.plugins.frontend.editor.domain.BeanProvider;
import org.hippoecm.hst.plugins.frontend.editor.domain.Sitemenu;
import org.hippoecm.hst.plugins.frontend.editor.validators.NodeUniqueValidator;

/**
 * The Class AddSitemenuDialog.
 */
public class AddSitemenuDialog extends AddNodeDialog<Sitemenu> {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new dialog for adds the sitemenus.
     * 
     * @param dao the dao
     * @param plugin the plugin
     * @param parent the parent
     */
    public AddSitemenuDialog(EditorDAO<Sitemenu> dao,
			RenderPlugin plugin, JcrNodeModel parent) {
		super(dao, plugin, parent);
		
        FormComponent textField = new RequiredTextField("name");
        textField.setOutputMarkupId(true);
        textField.add(new NodeUniqueValidator<Sitemenu>(new BeanProvider<Sitemenu>() {
            private static final long serialVersionUID = 1L;

            public Sitemenu getBean() {
                return bean;
            }

        }));
        add(textField);
        setFocus(textField);

	}

}
