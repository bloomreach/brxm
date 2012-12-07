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
package org.hippoecm.frontend.dialog;

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.behaviors.EventStoppingBehavior;

public class DialogWindow extends ModalWindow implements IDialogService {

    private static final String LINE_BREAKS_REGEX = "(\\r|\\n)";
    private static final long serialVersionUID = 1L;

    private class Callback implements ModalWindow.WindowClosedCallback {
        private static final long serialVersionUID = 1L;

        Dialog dialog;

        Callback(Dialog dialog) {
            this.dialog = dialog;
        }

        public void onClose(AjaxRequestTarget target) {
            dialog.onClose();
            if (pending.size() > 0) {
                Dialog dialog = pending.remove(0);
                internalShow(dialog);
            } else {
                cleanup();
            }
        }
    }

    private Dialog shown;
    private List<Dialog> pending;

    public DialogWindow(String id) {
        super(id);

        pending = new LinkedList<Dialog>();

        add(JavascriptPackageResource.getHeaderContribution(
                new JavascriptResourceReference(DialogWindow.class, "res/hippo-modal.js")));
        add(CSSPackageResource.getHeaderContribution(DialogWindow.class, "res/hippo-modal.css"));
        add(new EventStoppingBehavior("onclick"));
    }

    public void show(Dialog dialog) {
        if (isShown()) {
            pending.add(dialog);
        } else {
            internalShow(dialog);
        }
    }

    /**
     * Hides the dialog, if it is currently shown, or removes it from the list of
     * to-be-shown dialogs.  The onClose() method is not invoked on the dialog.
     * @param dialog
     */
    public void hide(Dialog dialog) {
        if (dialog == shown) {
            close();
            cleanup();
        }
        if (pending.contains(dialog)) {
            pending.remove(dialog);
        }
        if (pending.size() > 0) {
            show(pending.remove(0));
        }
    }

    public void close() {
        AjaxRequestTarget target = AjaxRequestTarget.get();
        if (target != null) {
            close(target);
        } else {
            respondOnWindowClosed(null);
        }
    }

    public void render(PluginRequestTarget target) {
        if (shown != null) {
            shown.render(target);
        }
    }

    @Override
    public boolean isShown() {
        return (shown != null);
    }

    private void cleanup() {
        shown = null;
        setContent(new EmptyPanel(getContentId()));
        setWindowClosedCallback(null);
        setTitle(new Model<String>("title"));
    }

    private void internalShow(Dialog dialog) {
        shown = dialog;
        dialog.setDialogService(this);
        setContent(dialog.getComponent());
        setTitle(removeLineBreaks(dialog.getTitle()));
        setWindowClosedCallback(new Callback(dialog));

        IValueMap properties = dialog.getProperties();
        setInitialHeight(properties.getInt("height", 455));
        setInitialWidth(properties.getInt("width", 850));
        setResizable(properties.getAsBoolean("resizable", false));
        String defaultCssClassName = isResizable() ? "w_grey_resize" : "w_grey";
        String cssClassName = properties.getString("css-class-name", defaultCssClassName);
        setCssClassName(cssClassName);

        IRequestTarget target = RequestCycle.get().getRequestTarget();
        if (AjaxRequestTarget.class.isAssignableFrom(target.getClass())) {
            show((AjaxRequestTarget) target);
        }
    }

    private static IModel<String> removeLineBreaks(IModel<String> stringModel) {
        final String s = stringModel.getObject();
        final String withoutLineBreaks = s.replaceAll(LINE_BREAKS_REGEX, "");
        if (s.equals(withoutLineBreaks)) {
            return stringModel;
        }
        return new Model<String>(withoutLineBreaks);
    }

    /**
     * Shows the modal window.
     *
     * @param target
     *            Request target associated with current ajax request.
     */
    public void show(final AjaxRequestTarget target)
    {
        if (!super.isShown())
        {
            getContent().setVisible(true);
            target.addComponent(this);
        }
    }

    /**
     * @see org.apache.wicket.markup.html.panel.Panel#renderHead(org.apache.wicket.markup.html.internal.HtmlHeaderContainer)
     */
    public void renderHead(HtmlHeaderContainer container)
    {
        super.renderHead(container);
        if (super.isShown())
        {
            container.getHeaderResponse().renderOnDomReadyJavascript(getWindowOpenJavascript());
        }
    }

    @Override
    protected boolean makeContentVisible() {
        return shown != null;
    }
}
