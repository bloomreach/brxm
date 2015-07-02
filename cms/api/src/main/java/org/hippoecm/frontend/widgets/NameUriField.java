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
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.time.Duration;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.repository.api.StringCodec;

public class NameUriField extends WebMarkupContainer {

    private final IModel<String> nameModel;
    private final IModel<String> urlModel;

    private final IModel<StringCodec> codecModel;

    private final FormComponent<String> nameComponent;
    private final FormComponent<String> urlComponent;

    private boolean urlIsEditable;
    private boolean urlIsRequired;

    public NameUriField(final String id, final IModel<StringCodec> codecModel) {
        this(id, codecModel, null, null);
    }

    public NameUriField(String id, IModel<StringCodec> codecModel, final String url, final String name) {
        this(id, codecModel, url, name, null);
    }

    public NameUriField(final String id, final IModel<StringCodec> codecModel, final String name, final String url,
                        final Boolean urlFieldEnabled) {
        super(id);
        this.codecModel = codecModel;

        // If urlFieldEnabled is null, we check if the encoded version of param name equals param url. If true we
        // choose to keep the name field in control of the value of url, otherwise control over the url field is
        // handed over to the user.
        urlIsEditable = urlIsRequired = urlFieldEnabled != null ? urlFieldEnabled :
                StringUtils.isNotEmpty(name) && !StringUtils.equals(encode(name), url);

        nameModel = Model.of(name);
        add(nameComponent = createNameComponent());

        urlModel = new Model<String>(url) {
            @Override
            public String getObject() {
                return encode(urlIsEditable ? super.getObject() : getName());
            }
        };
        add(urlComponent = createUrlComponent());

        add(createUrlAction());
    }

    private FormComponent<String> createNameComponent() {
        FormComponent<String> nameComponent = new TextField<>("name", nameModel);
        nameComponent.setRequired(true);
        nameComponent.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (!urlIsEditable) {
                    // the value of the url field is controlled by the name value, redraw when name changes
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

    private FormComponent<String> createUrlComponent() {
        FormComponent<String> urlComponent = new TextField<String>("url", urlModel) {
            @Override
            public boolean isEnabled() {
                return urlIsEditable;
            }

            @Override
            public boolean isRequired() {
                return urlIsRequired;
            }
        };
        urlComponent.add(CssClass.append(new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return urlIsEditable ? "grayedin" : "grayedout";
            }
        }));
        urlComponent.setOutputMarkupId(true);
        return urlComponent;
    }

    private Component createUrlAction() {
        AjaxLink<Boolean> uriAction = new AjaxLink<Boolean>("uriAction") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                urlIsRequired = !urlIsEditable;

                if (!urlComponent.isValid() || urlComponent.getForm().hasErrorMessage()) {
                    urlComponent.getForm().getFeedbackMessages().clear();
                    // reset the urlComponent with the value in nameModel and restart validation
                    urlModel.setObject("");
                    urlComponent.setConvertedInput(getName());
                    urlComponent.updateModel();
                    urlComponent.validate();
                }
                urlModel.setObject(getName());
                urlIsEditable = !urlIsEditable;

                target.add(this);
                target.add(urlComponent);
                target.focusComponent(urlIsEditable ? urlComponent : nameComponent);
            }
        };

        uriAction.add(new Label("uriActionLabel", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return urlIsEditable ? getString("url-reset") : getString("url-edit");
            }
        } ));
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

    private String encode(final String text) {
        if (text == null) {
            return null;
        }
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

    // If the codec model has been detached and the url field is not editable, we should redraw the url field to ensure
    // the intended codec is applied (mostly the codec model is detached so that upon the next usage it will load a
    // different StringCodec).
    public void onCodecModelDetached() {
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (!urlIsEditable) {
            target.add(urlComponent);
        }
    }
}
