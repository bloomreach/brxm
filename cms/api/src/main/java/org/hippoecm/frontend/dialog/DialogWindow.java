/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.behaviors.EventStoppingBehavior;

public class DialogWindow extends ModalWindow implements IDialogService {

    private static final long serialVersionUID = 1L;

    private static final ResourceReference MODAL_JS = new JavaScriptResourceReference(DialogWindow.class, "res/hippo-modal.js");
    private static final ResourceReference MODAL_STYLESHEET = new CssResourceReference(DialogWindow.class, "res/hippo-modal.css");

    private class Callback implements ModalWindow.WindowClosedCallback {
        private static final long serialVersionUID = 1L;

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

        pending = new LinkedList<Dialog>();

        add(new EventStoppingBehavior("onclick"));
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

        response.render(CssHeaderItem.forReference(MODAL_STYLESHEET));
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
     * @param dialog
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
        setTitle(new Model<String>("title"));
        remove(getContent());
        dialog = null;
    }

    @Override
    public void renderHead(final HtmlHeaderContainer container) {
        super.renderHead(container);

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
        setInitialHeight(properties.getInt("height", 455));
        setInitialWidth(properties.getInt("width", 850));
        setResizable(properties.getAsBoolean("resizable", false));
        String defaultCssClassName = isResizable() ? "w_grey_resize" : "w_grey";
        String cssClassName = properties.getString("css-class-name", defaultCssClassName);
        setCssClassName(cssClassName);

        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            show(target);
        }
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
