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
import org.hippoecm.hst.plugins.frontend.editor.domain.SitemenuItem;
import org.hippoecm.hst.plugins.frontend.editor.validators.NodeUniqueValidator;

/**
 * The Class AddSitemenuItemDialog represents a Wicket dialog for adding sitemenu items.
 */
public class AddSitemenuItemDialog extends AddNodeDialog<SitemenuItem> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * Instantiates a new dialog for adding a sitemenu item.
     * 
     * @param dao the dao
     * @param plugin the plugin
     * @param parent the parent
     */
    public AddSitemenuItemDialog(EditorDAO<SitemenuItem> dao, RenderPlugin plugin, JcrNodeModel parent) {
        super(dao, plugin, parent);

        FormComponent textField = new RequiredTextField("name");
        textField.setOutputMarkupId(true);
        textField.add(new NodeUniqueValidator<SitemenuItem>(new BeanProvider<SitemenuItem>() {
            private static final long serialVersionUID = 1L;

            public SitemenuItem getBean() {
                return bean;
            }

        }));
        add(textField);
        setFocus(textField);

    }

}
