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

import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.plugins.frontend.editor.EditorPlugin;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.dao.SitemenuDAO;
import org.hippoecm.hst.plugins.frontend.editor.dao.SitemenuItemDAO;
import org.hippoecm.hst.plugins.frontend.editor.domain.Sitemenu;
import org.hippoecm.hst.plugins.frontend.editor.domain.SitemenuItem;

public class SitemenusEditorPlugin extends EditorPlugin<Sitemenu> {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public SitemenusEditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    //FIXME: This is a nasty hack
    @Override
    protected String getAddDialogTitle() {
        return new StringResourceModel("dialog.title", this, null).getString();
    }

    @Override
    protected EditorDAO<Sitemenu> newDAO() {
        return new SitemenuDAO(getPluginContext(), getPluginConfig());
    }

    @Override
    protected Dialog newAddDialog() {
        return new AddSitemenuDialog(dao, this, (JcrNodeModel) getModel());
    }

}
