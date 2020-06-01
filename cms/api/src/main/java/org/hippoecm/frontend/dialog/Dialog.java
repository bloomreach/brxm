/*
 *  Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.dialog;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;

public class Dialog<ModelType> extends AbstractDialog<ModelType> {

    public static final String BOTTOM_LEFT_ID = "bottom-left";

    private IValueMap size = DialogConstants.LARGE_AUTO;
    private IValueMap properties;

    private String cssClass;
    private String titleKey = "dialog.title";
    private IModel<?> titleModel;
    private IModel<String> title;

    private ScriptAction<ModelType> cancelAction;
    private ScriptAction<ModelType> closeAction;

    public Dialog() {
        this(null);
    }

    public Dialog(final IModel<ModelType> model) {
        super(model);

        setButtonCssClass("btn btn-default");

        add(new EmptyPanel(BOTTOM_LEFT_ID).setVisible(false));
    }

    @Override
    public IModel<String> getTitle() {
        if (title == null) {
            title = Model.of(getString(titleKey, titleModel, StringUtils.EMPTY));
        }
        return title;
    }

    public void setTitle(final IModel<String> title) {
        this.title = title;
    }

    @Override
    public IValueMap getProperties() {
        if (properties == null) {
            if (StringUtils.isNotEmpty(cssClass)) {
                final Map<String, Object> map = new LinkedHashMap<>(size);
                map.put("css-class-name", cssClass);
                properties = new ValueMap(map).makeImmutable();
            } else {
                properties = size;
            }
        }
        return properties;
    }

    @Override
    protected boolean addAjaxIndicator() {
        return false;
    }

    protected void setTitleKey(final String titleKey) {
        this.titleKey = titleKey;
    }

    protected void setTitleModel(final IModel<?> titleModel) {
        this.titleModel = titleModel;
    }

    protected void setSize(final IValueMap size) {
        this.size = size;
    }

    protected void setCssClass(final String cssClass) {
        this.cssClass = cssClass;
    }

    protected void setCancelAction(final ScriptAction<ModelType> cancelAction) {
        this.cancelAction = cancelAction;
    }

    protected void setCloseAction(final ScriptAction<ModelType> closeAction) {
        this.closeAction = closeAction;
    }

    @Override
    public void onClose() {
        final ScriptAction<ModelType> action = cancelled ? cancelAction : closeAction;
        if (action != null) {
            final String script = action.getJavaScript(getModelObject());
            if (StringUtils.isNotBlank(script)) {
                final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                if (target != null) {
                    target.getHeaderResponse().render(OnDomReadyHeaderItem.forScript(script));
                }
            }
        }
        super.onClose();
    }

    @Override
    protected void onDetach() {
        if (title != null) {
            title.detach();
        }
        if (titleModel != null) {
            titleModel.detach();
        }
        super.onDetach();
    }
}
