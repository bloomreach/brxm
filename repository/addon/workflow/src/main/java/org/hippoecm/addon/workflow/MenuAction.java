/*
 *  Copyright 2009 Hippo.
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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.behaviors.IContextMenu;

class MenuAction extends Panel implements IContextMenu {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";

    private static final long serialVersionUID = 1L;

    public MenuAction(String id, final ActionDescription wf) {
        super(id);

        MenuLink link;
        add(link = new MenuLink("link") {
            @Override
            public void onClick() {
                if (wf instanceof StdWorkflow) {
                    ((StdWorkflow)wf).invoke();
                }
            }
        });

        Component fragment = wf.getFragment("text");
        if (fragment instanceof ActionDescription.ActionDisplay) {
            ((ActionDescription.ActionDisplay)fragment).substantiate();
            link.add(fragment);
        } else if (fragment instanceof Fragment) {
            link.add(fragment);
        } else {
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
    
    public void collapse(AjaxRequestTarget target) {
    }
}
