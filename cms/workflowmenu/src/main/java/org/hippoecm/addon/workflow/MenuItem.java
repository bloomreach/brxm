/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;

class MenuItem extends Panel {

    private static final long serialVersionUID = 1L;

    public MenuItem(String id, final ActionDescription wf, final Form form) {
        super(id);

        MenuLink link = new MenuLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return wf.isEnabled();
            }

            @Override
            protected Form getForm() {
                if (wf.isFormSubmitted()) {
                    return form;
                } else {
                    return  null;
                }
            }

            @Override
            public void onClick() {
                wf.run();
            }
        };
        add(link);
        if (!wf.isEnabled()) {
            link.add(CssClass.append("disabled"));
        }
        
        Component fragment = wf.getFragment("text");
        if (fragment instanceof ActionDescription.ActionDisplay) {
            ((ActionDescription.ActionDisplay)fragment).substantiate();
            link.add(fragment);
        } else if (fragment instanceof Fragment) {
            link.add(fragment);
        } else {
            link.add(new Label("text").setVisible(false));
            // wf.setVisible(true);
        }

        fragment = wf.getFragment("icon");
        if (fragment instanceof ActionDescription.ActionDisplay) {
            ((ActionDescription.ActionDisplay)fragment).substantiate();
            link.add(fragment);
        } else if (fragment instanceof Fragment) {
            link.add(fragment);
        }
    }

}
