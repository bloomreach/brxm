/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.menu.save;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugins.console.Shortcuts;
import org.hippoecm.frontend.session.UserSession;

public class SaveDialogLink extends DialogLink {

    private static final long serialVersionUID = 1L;

    public SaveDialogLink(String id, IModel<String> linkText, final IDialogFactory dialogFactory, final IDialogService dialogService) {
        super(id, linkText, dialogFactory, dialogService, Shortcuts.CTRL_S);

        Label label = new Label("dialog-link-text-extended", new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (hasSessionChanges()) {
                    return "*";
                }
                return "";
            }
        });
        label.setOutputMarkupId(true);
        link.add(label);
    }

    private boolean hasSessionChanges() {
        Session session = UserSession.get().getJcrSession();
        try {
            return session.hasPendingChanges();
        } catch (RepositoryException e) {
            return false;
        }
    }

    @Override
    protected void onComponentTag(final ComponentTag tag) {
        super.onComponentTag(tag);
        tag.put("class", getCssClass());
    }

    private String getCssClass() {
        return hasSessionChanges() ? "hippo-console-menu-actions-save session-changes" : "hippo-console-menu-actions-save";
    }


    // Since the click is executed on the link, redrawing it will confuse browsers.
    // Instead, use javascript to update the class attribute.
    public void update(final PluginRequestTarget target) {
        target.add(link.get("dialog-link-text-extended"));
        target.appendJavaScript("Wicket.$('" + getMarkupId() + "').setAttribute('class', '" + getCssClass() + "');");
    }

}
