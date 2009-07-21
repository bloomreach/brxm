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

package org.hippoecm.hst.plugins.frontend.editor;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.plugin.IActivator;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.hst.plugins.frontend.editor.context.HstContext;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.domain.EditorBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EditorPlugin<K extends EditorBean> extends RenderPlugin implements IActivator {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(EditorPlugin.class);

    protected final EditorDAO<K> dao;
    protected final HstContext hstContext;

    private FormComponent focus;
    protected final AjaxLink addLink;

    public EditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        hstContext = context.getService(HstContext.class.getName(), HstContext.class);

        dao = newDAO();

        add(new Label("title", new StringResourceModel("title", this, null)));

        add(addLink = new AjaxLink("add") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IDialogService dialogService = getDialogService();
                dialogService.show(newAddDialog());
            }
        });
    }

    protected void setFocus(FormComponent fc) {
        focus = fc;
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        if (focus != null) {
            target.focusComponent(focus);
            focus = null;
        }
    }

    protected String getAddDialogTitle() {
        return "";
    }

    @Override
    protected void onModelChanged() {
        redraw();
    }

    @Override
    protected void detachModel() {
        hstContext.detach();
        super.detachModel();
    }

    public void start() {
    }

    public void stop() {
    }

    abstract protected EditorDAO<K> newDAO();

    abstract protected Dialog newAddDialog();

}