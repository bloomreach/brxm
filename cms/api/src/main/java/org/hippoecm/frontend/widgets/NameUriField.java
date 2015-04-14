/*
 * Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.widgets;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.time.Duration;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.repository.api.StringCodec;

public class NameUriField extends WebMarkupContainer {

    private final IModel<String> nameModel;
    private final IModel<String> urlModel;

    private final IModel<StringCodec> codecModel;

    private final FormComponent nameComponent;
    private final FormComponent urlComponent;

    private boolean editingUrl;
    private boolean modifiedUrl;

    public NameUriField(String id, IModel<StringCodec> codecModel, final String url, final String name) {
        super(id);
        this.codecModel = codecModel;

        nameModel = Model.of(name);
        urlModel = Model.of(url);
        modifiedUrl = !StringUtils.equals(encode(nameModel.getObject()), urlModel.getObject());

        add(nameComponent = createNameComponent(nameModel));
        add(urlComponent = createUriComponent(urlModel));

        add(createUrlAction());
    }

    public NameUriField(final String id, final IModel<StringCodec> codecModel) {
        this(id, codecModel, "", "");
    }

    private FormComponent createNameComponent(final IModel<String> nameModel) {
        FormComponent nameComponent = new TextField<>("name", nameModel);
        nameComponent.setRequired(true);
        nameComponent.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (!editingUrl && !modifiedUrl) {
                    urlModel.setObject(encode(nameModel.getObject()));
                    target.add(urlComponent);
                }
            }

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setThrottlingSettings(new ThrottlingSettings(NameUriField.this.getPath(), Duration.milliseconds(500)));
            }
        });
        nameComponent.setOutputMarkupId(true);
        return nameComponent;
    }

    private FormComponent createUriComponent(final IModel<String> urlModel) {
        FormComponent urlComponent = new TextField<String>("url", urlModel) {
            @Override
            public boolean isEnabled() {
                return editingUrl;
            }
        };
        urlComponent.setRequired(true);
        urlComponent.add(CssClass.append(new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return editingUrl ? "grayedin" : "grayedout";
            }
        }));
        urlComponent.setOutputMarkupId(true);
        return urlComponent;
    }

    private Component createUrlAction() {
        AjaxLink<Boolean> uriAction = new AjaxLink<Boolean>("uriAction") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (editingUrl) { // resetting
                    urlModel.setObject(encode(nameModel.getObject()));
                    modifiedUrl = false;
                } else { // starting edit
                    target.focusComponent(urlComponent);
                    modifiedUrl = true;
                }
                editingUrl = !editingUrl;
                target.add(this);
                target.add(urlComponent);
            }
        };
        uriAction.add(new Label("uriActionLabel", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return editingUrl ? getString("url-reset") : getString("url-edit");
            }
        }));
        return uriAction;
    }

    @Override
    protected void onBeforeRender() {
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            target.focusComponent(nameComponent);
        }
        super.onBeforeRender();
    }

    public void encodeUri() {
        urlModel.setObject(encode(nameModel.getObject()));
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            target.add(urlComponent);
        }
    }

    private String encode(final String text) {
        return codecModel.getObject().encode(text);
    }

    public FormComponent[] getComponents() {
        return new FormComponent[]{this.urlComponent, this.nameComponent};
    }

    public FormComponent getUrlComponent() {
        return urlComponent;
    }

    public FormComponent getNameComponent() {
        return nameComponent;
    }

    public String getName() {
        return nameModel.getObject();
    }

    public String getUrl() {
        return urlModel.getObject();
    }
}
