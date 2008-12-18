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

package org.hippoecm.frontend.plugins.xinha.dialog.links;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.dialog.browse.AbstractBrowserPlugin;
import org.hippoecm.frontend.widgets.BooleanFieldWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class ExternalLinkPlugin extends AbstractBrowserPlugin {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final Form form;

    public ExternalLinkPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(form = new Form("form1"));

        form.add(new TextFieldWidget("href", getBean().getPropertyModel(XinhaLink.HREF)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                update();
            }
        });

        form.add(new TextFieldWidget("title", getBean().getPropertyModel(XinhaLink.TITLE)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                update();
            }
        });

        form.add(new BooleanFieldWidget("popup", new PopupModel(getBean().getPropertyModel(XinhaLink.TARGET))) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                update();
            }
        });
    }

    private void update() {
        if (!ok.isEnabled() && (getBean().getHref() != null && getBean().getHref().length() > 0)) {
            enableOk(true);
        }
    }

    private XinhaLink getBean() {
        return (XinhaLink) getModelObject();
    }

    class PopupModel implements IModel {
        private static final long serialVersionUID = 1L;

        private static final String NEW_WINDOW = "_blank";

        private IModel wrapped;

        public PopupModel(IModel wrapped) {
            this.wrapped = wrapped;
        }

        public Object getObject() {
            String value = (String) wrapped.getObject();
            if (value != null && value.equals(NEW_WINDOW)) {
                return true;
            }
            return false;
        }

        public void setObject(Object object) {
            Boolean test = (Boolean) object;
            if (test) {
                wrapped.setObject(NEW_WINDOW);
            } else {
                wrapped.setObject("");
            }
        }

        public void detach() {
            wrapped.detach();
        }

    }
}
