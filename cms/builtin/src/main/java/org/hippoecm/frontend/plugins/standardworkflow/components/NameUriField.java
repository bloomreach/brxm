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
package org.hippoecm.frontend.plugins.standardworkflow.components;

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
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.time.Duration;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.repository.api.StringCodec;

public class NameUriField extends WebMarkupContainer {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private String url;
    @SuppressWarnings("unused")
    private String name;

    private final FormComponent urlComponent;
    private final FormComponent nameComponent;

    private final PropertyModel<String> urlModel;
    private final PropertyModel<String> nameModel;
    private final IModel<StringCodec> codecModel;

    private boolean urlModified = false;

    public NameUriField(String id, IModel<StringCodec> codecModel, final String url, final String name, final boolean urlModified) {
        this(id, codecModel);
        this.url = url;
        this.name = name;
        this.urlModified = urlModified;
    }

    public NameUriField(String id, IModel<StringCodec> codecModel) {
        super(id);
        this.codecModel = codecModel;

        urlModel = PropertyModel.of(this, "url");
        nameModel = PropertyModel.of(this, "name");

        add(urlComponent = createUriComponent(urlModel));
        add(nameComponent = createNameComponent(nameModel));

        add(createUrlAction());
    }

    @Override
    protected void onBeforeRender() {
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            target.focusComponent(nameComponent);
        }
        super.onBeforeRender();
    }

    private String encode(final IModel<String> text) {
        return codecModel.getObject().encode(text.getObject());
    }

    private FormComponent createNameComponent(final PropertyModel<String> nameModel) {
        final FormComponent nameComponent = new TextField<>("name", new IModel<String>() {
            private static final long serialVersionUID = 1L;

            public String getObject() {
                return nameModel.getObject();
            }

            public void setObject(String object) {
                nameModel.setObject(object);
                if (!urlModified) {
                    urlModel.setObject(encode(nameModel));
                }
            }

            public void detach() {
                nameModel.detach();
            }

        });
        nameComponent.setRequired(true);
        nameComponent.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (!urlModified) {
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

    private FormComponent createUriComponent(final PropertyModel<String> urlModel) {
        FormComponent urlComponent = new TextField<String>("url", urlModel) {
            @Override
            public boolean isEnabled() {
                return urlModified;
            }
        };

        urlComponent.add(CssClass.append(new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return urlModified ? "grayedin" : "grayedout";
            }
        }));
        urlComponent.setOutputMarkupId(true);
        return urlComponent;
    }

    private Component createUrlAction() {
        AjaxLink<Boolean> uriAction = new AjaxLink<Boolean>("uriAction") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                urlModified = !urlModified;
                if (!urlModified) {
                    urlModel.setObject(Strings.isEmpty(nameModel.getObject()) ? "" : encode(nameModel));
                    urlComponent.modelChanged();
                } else {
                    target.focusComponent(urlComponent);
                }
                target.add(urlComponent);
                target.add(this);
            }
        };
        uriAction.add(new Label("uriActionLabel", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return urlModified ? getString("url-reset") : getString("url-edit");
            }
        }));
        return uriAction;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public void encodeUri() {
        if (!urlModified) {
            final String name = Strings.isEmpty(nameModel.getObject()) ? "" : encode(nameModel);
            urlModel.setObject(name);
            AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target != null) {
                target.add(urlComponent);
            }
        }
    }

    public FormComponent[] getComponents() {
        return new FormComponent[]{this.urlComponent, this.nameComponent};
    }

    public FormComponent getUrlComponent() {
        return urlComponent;
    }

    public FormComponent getNameComponent(){
        return nameComponent;
    }
}
