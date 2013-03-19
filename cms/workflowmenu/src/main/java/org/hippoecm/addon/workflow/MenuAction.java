/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.behaviors.IContextMenu;

class MenuAction extends Panel implements IContextMenu {

    private static final long serialVersionUID = 1L;

    private MenuLink link;
    
    public MenuAction(String id, final ActionDescription wf, final Form form) {
        super(id);

        add(link = new MenuLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                if (wf instanceof StdWorkflow) {
                    ((StdWorkflow)wf).invoke();
                }
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
            public boolean isEnabled() {
                return wf.isEnabled();
            }

            @Override
            public boolean isVisible() {
                return wf.isVisible();
            }
        });

        link.add(new AttributeAppender("class", new IModel() {
            private static final long serialVersionUID = 1L;

            public Object getObject() {
                if (!wf.isEnabled()) {
                    return "disabled";
                }
                return "";
            }

            public void setObject(Object object) {
            }

            public void detach() {
            }
            
        }, " ") );

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

    /**
     * {@inheritDoc}
     * This visibility of this menu action is determined by checking the
     * {@code link} visibility status witch in turn determines the visibility
     * by checking the visibility of the {@link StdWorkflow} e.g. the workflow (action)
     * itself.
     */
    @Override
    public boolean isVisible() {
        if (link == null) {
            return true;
        }
        return link.isVisible();
    }
}
