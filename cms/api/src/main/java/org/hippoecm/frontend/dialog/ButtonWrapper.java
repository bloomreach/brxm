/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.util.WebApplicationHelper;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

public class ButtonWrapper implements IClusterable {

    private Button button;

    private boolean ajax;
    private IModel<String> label;
    private boolean visible;
    private boolean enabled;
    private KeyType keyType;
    private boolean hasChanges = false;

    public ButtonWrapper(final Button button) {
        this.button = button;
        visible = button.isVisible();
        enabled = button.isEnabled();
        label = button.getModel();

        if (button instanceof AjaxButton) {
            ajax = true;
        }
    }

    public ButtonWrapper(final IModel<String> label) {
        this(label, true);
    }

    public ButtonWrapper(final IModel<String> label, final boolean ajax) {
        this.ajax = ajax;
        this.label = label;
        this.visible = true;
        this.enabled = true;
    }

    private Button createButton() {
        if (ajax) {
            final AjaxButton button = new AjaxButton(DialogConstants.BUTTON) {

                @Override
                protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                    ButtonWrapper.this.onSubmit();
                }

                @Override
                public boolean isVisible() {
                    return visible;
                }

                @Override
                public boolean isEnabled() {
                    return enabled;
                }

                @Override
                protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                    super.updateAjaxAttributes(attributes);
                    attributes.getAjaxCallListeners().add(new AjaxCallListener(){
                        @Override
                        public CharSequence getBeforeHandler(final Component component) {
                            return String.format("$('#%s').prop('disabled', true);", getMarkupId());
                        }

                        @Override
                        public CharSequence getCompleteHandler(final Component component) {
                            return String.format("$('#%s').prop('disabled', false);", getMarkupId());
                        }
                    });
                    ButtonWrapper.this.onUpdateAjaxAttributes(attributes);
                }
            };
            button.setModel(label);
            return button;
        } else {
            final Button button = new Button(DialogConstants.BUTTON) {

                @Override
                public void onSubmit() {
                    ButtonWrapper.this.onSubmit();
                }

                @Override
                public boolean isVisible() {
                    return visible;
                }

                @Override
                public boolean isEnabled() {
                    return enabled;
                }
            };
            button.setModel(label);
            return button;
        }
    }

    protected void onUpdateAjaxAttributes(final AjaxRequestAttributes attributes) {
    }

    public Button getButton() {
        if (button == null) {
            button = decorate(createButton());
        }
        return button;
    }

    protected Button decorate(final Button button) {
        button.setEnabled(enabled);
        button.setVisible(visible);
        if (getKeyType() != null) {
            button.add(new InputBehavior(new KeyType[]{getKeyType()}, EventType.click) {

                @Override
                protected Boolean getDisable_in_input() {
                    return !getKeyType().equals(KeyType.Escape);
                }

                @Override
                public void onRemove(final Component component) {
                    super.onRemove(component);
                    final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                    if (target != null) {
                        target.appendJavaScript(String.format("if (window['shortcut']) { shortcut.remove('%s'); }",
                                getKeyType().getKeyCode()));
                    }
                }
            });
        }
        return button;
    }

    public void setEnabled(final boolean isset) {
        enabled = isset;
        if (button != null && WebApplicationHelper.isPartOfPage(button)) {
            button.setEnabled(isset);
            if (ajax) {
                final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                if (target != null) {
                    if (!isset) {
                        renderAttribute(target, "disabled", "disabled");
                    } else {
                        target.appendJavaScript(
                                String.format("Wicket.$('%s').removeAttribute('disabled')", button.getMarkupId()));
                        for (final Behavior behavior : button.getBehaviors()) {
                            final ComponentTag tag = new ComponentTag("button", XmlTag.TagType.OPEN_CLOSE);
                            behavior.onComponentTag(button, tag);
                            behavior.renderHead(button, target.getHeaderResponse());

                            for (final Map.Entry<String, Object> entry : tag.getAttributes().entrySet()) {
                                renderAttribute(target, entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Reverse the encoding that was done by {@link AjaxEventBehavior#onComponentTag} method.
     * That is needed for rendering to html, but is not usable in an ajax response.
     *
     * @param target    The ajax request target
     * @param key       The attribute key
     * @param value     The attribute value
     */
    private void renderAttribute(final AjaxRequestTarget target, final String key, Object value) {
        if (value != null) {
            value = Strings.replaceAll(value.toString(), "&nbsp;", " ");
            value = Strings.replaceAll(value.toString(), "&amp;", "&");
            value = Strings.replaceAll(value.toString(), "&gt;", ">");
            value = Strings.replaceAll(value.toString(), "&lt;", "<");
            value = Strings.replaceAll(value.toString(), "&quot;", "\\\"");
        }

        target.appendJavaScript(
                String.format("Wicket.$('%s').setAttribute('%s', \"%s\")", button.getMarkupId(), key, value));
    }

    public void setVisible(final boolean isset) {
        visible = isset;
        if (button != null && button.isVisible() != isset) {
            button.setVisible(isset);
            if (ajax) {
                final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                if (target != null) {
                    target.add(button);
                }
            }
        }
    }

    public void setAjax(final boolean c) {
        ajax = c;
    }

    public void setLabel(final IModel<String> label) {
        this.label = label;
        if (button != null) {
            button.setModel(label);
        }
        hasChanges = true;
    }

    public void setKeyType(final KeyType keyType) {
        this.keyType = keyType;
    }

    protected void onSubmit() {
    }

    public boolean hasChanges() {
        if (!ajax) {
            return false;
        }

        if (button == null) {
            return true;
        }

        if (visible != button.isVisible()) {
            return true;
        }

        if (enabled != button.isEnabled()) {
            return true;
        }

        return hasChanges;
    }

    public void rendered() {
        hasChanges = false;
    }

    protected KeyType getKeyType() {
        return keyType;
    }

}
