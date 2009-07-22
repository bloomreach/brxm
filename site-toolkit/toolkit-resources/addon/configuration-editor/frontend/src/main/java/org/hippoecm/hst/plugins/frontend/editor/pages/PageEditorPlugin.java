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

package org.hippoecm.hst.plugins.frontend.editor.pages;

import java.util.List;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.plugins.frontend.editor.components.ComponentEditorPlugin;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.dao.PageDAO;
import org.hippoecm.hst.plugins.frontend.editor.domain.Component;

public class PageEditorPlugin extends ComponentEditorPlugin {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public PageEditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        
        //Pages shouldn't be able to add nested pages
        addLink.setVisible(false);
    }

    @Override
    protected EditorDAO<Component> newDAO() {
        return new PageDAO(getPluginContext(), hstContext.page.getNamespace());
    }

    //    @Override
    //    protected Dialog newAddDialog() {
    //        return new AddPageDialog((PageDAO) dao, this, (JcrNodeModel) getModel());
    //    }

    @Override
    public List<String> getReferenceableComponents() {
        List<String> refs = hstContext.page.getReferenceables();
        refs.addAll(super.getReferenceableComponents());
        return refs;
    }
}
