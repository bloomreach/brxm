/*
 *  Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.addon.workflow;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.behaviors.IContextMenu;
import org.hippoecm.frontend.buttons.ButtonStyle;

class MenuAction extends Panel implements IContextMenu {

    private final MenuLink link;

    public MenuAction(final String id, final ActionDescription wf, final Form form) {
        super(id);

        link = new MenuLink("link") {

            @Override
            public void onClick() {
                if (wf instanceof StdWorkflow) {
                    final StdWorkflow stdWorkflow = (StdWorkflow) wf;
                    if (form != null && form.hasError() && !stdWorkflow.invokeOnFormError()) {
                        return;
                    }
                    wf.invoke();
                }
            }

            @Override
            protected Form getForm() {
                if (wf.isFormSubmitted()) {
                    return form;
                } else {
                    return null;
                }
            }

            @Override
            public boolean isEnabled() {
                return wf.isEnabled();
            }

            @Override
            public boolean isVisible() {
                return wf.isVisible();
            }
        };
        add(link);

        link.add(ClassAttribute.append(() -> wf.getCssClass() != null
                ? wf.getCssClass()
                : ButtonStyle.DEFAULT.getCssClass()));

        link.add(ClassAttribute.append(() -> !wf.isEnabled()
                ? "disabled"
                : StringUtils.EMPTY));

        final Component fragment = wf.getFragment("text");
        if (fragment instanceof ActionDescription.ActionDisplay) {
            ((ActionDescription.ActionDisplay) fragment).substantiate();
            link.add(fragment);
        } else if (fragment instanceof Fragment) {
            link.add(fragment);
        }
    }

    public void collapse(final AjaxRequestTarget target) {
    }

    /**
     * {@inheritDoc} This visibility of this menu action is determined by checking the {@code link} visibility status
     * witch in turn determines the visibility by checking the visibility of the {@link StdWorkflow} e.g. the workflow
     * (action) itself.
     */
    @Override
    public boolean isVisible() {
        return link == null || link.isVisible();
    }
}
