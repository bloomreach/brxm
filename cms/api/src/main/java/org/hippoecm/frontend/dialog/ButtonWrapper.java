/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Optional;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.ajax.PreventDoubleClickListener;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.buttons.ButtonStyle;
import org.hippoecm.frontend.buttons.ButtonType;
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
    private ButtonStyle style = ButtonStyle.DEFAULT;
    private ButtonType buttonType = ButtonType.BUTTON;

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
        final Button button;

        if (ajax) {
            button = new AjaxButton(DialogConstants.BUTTON) {

                @Override
                protected void onComponentTag(final ComponentTag tag) {
                    super.onComponentTag(tag);

                    tag.put("type", buttonType.getType());
                }

                @Override
                protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                    ButtonWrapper.this.onSubmit();
                }

                @Override
                protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                    super.updateAjaxAttributes(attributes);

                    attributes.getAjaxCallListeners().add(new PreventDoubleClickListener());

                    ButtonWrapper.this.onUpdateAjaxAttributes(attributes);
                }

            };
            button.setOutputMarkupId(true);
        } else {
            button = new Button(DialogConstants.BUTTON) {

                @Override
                protected void onComponentTag(final ComponentTag tag) {
                    super.onComponentTag(tag);

                    tag.put("type", buttonType.getType());
                }

                @Override
                public void onSubmit() {
                    ButtonWrapper.this.onSubmit();
                }
            };
        }

        button.setVisible(visible);
        button.setEnabled(enabled);
        button.setModel(label);

        if (style != null) {
            button.add(ClassAttribute.append(style.getCssClass()));
        }

        return button;
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

    public void setEnabled(final boolean isEnabled) {
        enabled = isEnabled;
        if (button != null && button.isEnabled() != isEnabled && WebApplicationHelper.isPartOfPage(button)) {
            button.setEnabled(isEnabled);
            if (ajax) {
                final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                if (target != null) {
                    target.add(button);
                }
            }
        }
    }

    public void setVisible(final boolean isVisible) {
        visible = isVisible;
        if (button != null && button.isVisible() != isVisible) {
            button.setVisible(isVisible);
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

    public void setStyle(final ButtonStyle style) {
        this.style = style;
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

    protected void setType(final ButtonType buttonType) {
        this.buttonType = buttonType;
    }
}
