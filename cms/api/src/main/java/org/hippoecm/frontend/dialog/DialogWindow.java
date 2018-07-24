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

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.behaviors.EventStoppingBehavior;

public class DialogWindow extends ModalWindow implements IDialogService {

    private static final ResourceReference MODAL_JS = new JavaScriptResourceReference(DialogWindow.class, "hippo-modal.js");

    private class Callback implements ModalWindow.WindowClosedCallback {

        Dialog dialog;

        Callback(Dialog dialog) {
            this.dialog = dialog;
        }

        public void onClose(AjaxRequestTarget target) {
            closeDialog(dialog);
        }
    }

    private Dialog dialog;
    private List<Dialog> pending;

    public DialogWindow(String id) {
        super(id);

        pending = new LinkedList<>();

        add(new EventStoppingBehavior("click"));

        // Simply refresh if the user wants to
        showUnloadConfirmation(false);
    }

    private void closeDialog(Dialog dialog) {
        dialog.onClose();
        if (pending.size() > 0) {
            Dialog removedDialog = pending.remove(0);
            internalShow(removedDialog);
        } else {
            clear();
            setWindowClosedCallback(null);
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(JavaScriptHeaderItem.forReference(MODAL_JS));
    }

    /**
     * Revert ModalWindow change committed in https://issues.apache.org/jira/browse/WICKET-5101. Dialogs should be created
     * synchronously, otherwise other initialization code inside the dialogs (e.g. initializing YUI accordions) fails.
     */
    @Override
    protected CharSequence getShowJavaScript() {
        return "Wicket.Window.create(settings).show();\n";
    }

    /**
     * Adds the full dialog title for use in a tooltip.
     * This value shouldn't be HTML-escaped, but requires JavaScript quote-escaping.
     *
     * @param settings buffer containing a JS snippet
     * @return modified buffer
     */
    @Override
    protected AppendingStringBuffer postProcessSettings(final AppendingStringBuffer settings) {
        String title = new StringWithoutLineBreaksModel(dialog.getTitle()).getObject();
        String jsEscapedTitle = StringUtils.replace(title, "\"", "\\\"");

        settings.append("settings.titleTooltip = \"");
        settings.append(jsEscapedTitle);
        settings.append("\";\n");

        return settings;
    }

    public void show(Dialog dialog) {
        if (isShown()) {
            pending.add(dialog);
        } else {
            internalShow(dialog);
        }
    }

    /**
     * Hides the dialog, if it is currently shown, or removes it from the list of to-be-shown dialogs.  The onClose()
     * method is not invoked on the dialog.
     *
     * @param dialog The dialog to hide
     */
    public void hide(Dialog dialog) {
        if (pending.contains(dialog)) {
            pending.remove(dialog);
        }

        if (dialog == this.dialog) {
            close();
        }
    }

    public void showPending() {
        if (!pending.isEmpty()) {
            show(pending.remove(0));
        }
    }

    public void close() {
        if (isShown()) {
            AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target != null) {
                clear();
                String contentMarkupId = getContent().getMarkupId();
                target.appendJavaScript("var el = document.getElementById('" + contentMarkupId + "');"
                        + "if (el) { el.parentNode.removeChild(el); }");
                target.add(this);
                close(target);
            } else {
                closeDialog(dialog);
            }
        }
    }

    private void clear() {
        setTitle(Model.of("title"));
        remove(getContent());
        dialog = null;
    }

    @Override
    public void internalRenderHead(final HtmlHeaderContainer container) {
        super.internalRenderHead(container);

        if (!container.getWebRequest().isAjax() && isShown()) {
            container.getHeaderResponse().render(OnDomReadyHeaderItem.forScript(getWindowOpenJavaScript()));
        }
    }

    @Override
    public boolean isShowingDialog() {
        return isShown();
    }

    public void render(PluginRequestTarget target) {
        if (dialog != null) {
            dialog.render(target);
        }
    }

    @Override
    public boolean isShown() {
        return dialog != null && super.isShown();
    }

    private void internalShow(Dialog dialog) {
        this.dialog = dialog;
        dialog.setDialogService(this);
        setTitle(new StringWithoutLineBreaksModel(dialog.getTitle()));
        setContent(dialog.getComponent());
        setWindowClosedCallback(new Callback(dialog));

        IValueMap properties = dialog.getProperties();

        if (properties.containsKey("height") && properties.getString("height").equals("auto")) {
            setUseInitialHeight(false);
        } else {
            setUseInitialHeight(true);
            setInitialHeight(properties.getInt("height", 455));
        }

        setInitialWidth(properties.getInt("width", 850));
        setResizable(properties.getAsBoolean("resizable", false));

        String cssClasses = "hippo-dialog";
        if (isResizable()) {
            cssClasses += " hippo-dialog-resizable";
        }

        final String customCssClass = properties.getString("css-class-name", null);
        if (StringUtils.isNotEmpty(customCssClass)) {
            cssClasses += " " + customCssClass;
        }
        setCssClassName(cssClasses);

        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            show(target);
            focusContent(target);
        }
    }

    private void focusContent(final AjaxRequestTarget target) {
        final Component content = getContent();
        content.add(new AttributeModifier("tabindex", -1)); // make content focusable
        target.focusComponent(content);
    }

    /**
     * Shows the modal window.
     *
     * @param target Request target associated with current ajax request.
     */
    public void show(final AjaxRequestTarget target) {
        if (!super.isShown()) {
            getContent().setVisible(true);
            target.add(this);
            target.getHeaderResponse().render(OnDomReadyHeaderItem.forScript(getWindowOpenJavaScript()));
        }
    }

    @Override
    protected boolean makeContentVisible() {
        return dialog != null;
    }

}
