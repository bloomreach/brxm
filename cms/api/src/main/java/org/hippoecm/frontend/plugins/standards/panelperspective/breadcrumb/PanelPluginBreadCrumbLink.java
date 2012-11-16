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
package org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.version.undo.Change;

public abstract class PanelPluginBreadCrumbLink extends AjaxLink {
    private static final long serialVersionUID = 1L;

    /** The bread crumb model. */
    private final IBreadCrumbModel breadCrumbModel;

    /**
     * Construct.
     * 
     * @param id
     *            The link id
     * @param breadCrumbModel
     *            The bread crumb model
     */
    public PanelPluginBreadCrumbLink(String id, IBreadCrumbModel breadCrumbModel) {
        super(id);
        this.breadCrumbModel = breadCrumbModel;
    }

    /**
     * Callback for the onClick event. If ajax failed and this event was generated via a normal link
     * the target argument will be null
     * 
     * @param target
     *            ajax target if this linked was invoked using ajax, null otherwise
     */
    @Override
    public void onClick(AjaxRequestTarget target) {

        // get the currently active particpant
        final IBreadCrumbParticipant active = breadCrumbModel.getActive();
        if (active == null) {
            throw new IllegalStateException("The model has no active bread crumb. Before using " + this
                    + ", you have to have at least one bread crumb in the model");
        }

        // get the participant to set as active
        final IBreadCrumbParticipant participant = getParticipant(active.getComponent().getId());

        // add back button support
        addStateChange(new Change() {
            private static final long serialVersionUID = 1L;

            public void undo() {
                breadCrumbModel.setActive(active);
            }
        });

        // set the next participant as the active one
        breadCrumbModel.setActive(participant);
    }

    /**
     * Gets the {@link IBreadCrumbParticipant bread crumb participant} to be set active when the
     * link is clicked.
     * 
     * @param componentId
     *            When the participant creates it's own view, it typically should use this component
     *            id for the component that is returned by
     *            {@link IBreadCrumbParticipant#getComponent()}.
     * @return The bread crumb participant
     */
    protected abstract IBreadCrumbParticipant getParticipant(String componentId);

}
