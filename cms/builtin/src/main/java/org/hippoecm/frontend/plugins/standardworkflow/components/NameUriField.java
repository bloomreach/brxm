/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.repository.api.StringCodec;

public class NameUriField extends WebMarkupContainer {
    private static final long serialVersionUID = 1L;

    private final Component urlComponent;

    private boolean urlModified = false;

    private PropertyModel<String> urlModel;
    private PropertyModel<String> nameModel;

    private String name;
    private String url;
    
    private Component nameComponent;

    private final IModel<StringCodec> codecModel;

    public NameUriField(String id, IModel<StringCodec> codecModel) {
        super(id);
        this.codecModel = codecModel;

        nameModel = new PropertyModel<String>(this, "name");
        urlModel = new PropertyModel<String>(this, "url");

        nameComponent = createNameComponent(nameModel);
        add(nameComponent);

        add(urlComponent = createUriComponent(nameModel, urlModel));
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

    private StringCodec getNodeNameCodec() {
        return codecModel.getObject();
    }
    
    private Component createNameComponent(final PropertyModel<String> nameModel) {
        final FormComponent nameComponent = new TextField<String>("name", new IModel<String>() {
            private static final long serialVersionUID = 1L;

            public String getObject() {
                return nameModel.getObject();
            }

            public void setObject(String object) {
                nameModel.setObject(object);
                if (!urlModified) {
                    urlModel.setObject(getNodeNameCodec().encode(nameModel.getObject()));
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

    private Component createUriComponent(final PropertyModel<String> nameModel, final PropertyModel<String> urlModel) {
        FormComponent urlComponent = new TextField<String>("url", urlModel) {
            @Override
            public boolean isEnabled() {
                return urlModified;
            }
        };

        urlComponent.add(new CssClassAppender(new AbstractReadOnlyModel<String>() {
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
                    urlModel.setObject(Strings.isEmpty(nameModel.getObject()) ? "" : getNodeNameCodec().encode(
                            nameModel.getObject()));
                } else {
                    target.focusComponent(urlComponent);
                }
                target.add(urlComponent);
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
}
