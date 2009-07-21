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

package org.hippoecm.hst.plugins.frontend.editor.dialogs;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.domain.BeanProvider;
import org.hippoecm.hst.plugins.frontend.editor.domain.EditorBean;

public abstract class EditorDialog<K extends EditorBean> extends AbstractDialog implements BeanProvider<K> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private FormComponent focus;
    protected K bean;
    protected EditorDAO<K> dao;

    public EditorDialog(EditorDAO<K> dao, K bean) {
        super(new CompoundPropertyModel(bean));
        this.dao = dao;
        this.bean = bean;
    }

    //FIXME: This is a nasty hack
    public IModel getTitle() {
        return new StringResourceModel("dialog.title", this, null, new Object[] { getDialogTitle() });
    }

    //FIXME: This is a nasty hack
    protected String getDialogTitle() {
        return "";
    }

    @Override
    protected void onOk() {
        if (dao.save(bean)) {
            update(bean);
        }
    }

    @Override
    protected void onCancel() {
        JcrNodeModel parent = new JcrNodeModel(bean.getModel().getParentModel().getItemModel().getPath());
        if (dao.delete(bean)) {
            update(dao.load(parent));
        }
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        if (focus != null) {
            target.focusComponent(focus);
            focus = null;
        }
    }

    protected void setFocus(FormComponent fc) {
        focus = fc;
    }

    public K getBean() {
        return bean;
    }

    abstract public void update(K bean);

}