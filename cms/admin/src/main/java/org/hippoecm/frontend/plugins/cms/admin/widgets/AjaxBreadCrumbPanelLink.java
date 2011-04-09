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
package org.hippoecm.frontend.plugins.cms.admin.widgets;

import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;

public class AjaxBreadCrumbPanelLink extends AjaxBreadCrumbLink {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    /** The bread crumb model. */
    private final IBreadCrumbModel breadCrumbModel;

    /** factory for creating bread crumbs panels. */
    private final AjaxBreadCrumbPanelFactory breadCrumbPanelFactory;

    /** the plugin context */
    private final IModel model;

    public AjaxBreadCrumbPanelLink(final String id, final IPluginContext context, final BreadCrumbPanel caller,
            final Class panelClass) {
        this(id, caller.getBreadCrumbModel(), new AjaxBreadCrumbPanelFactory(context, panelClass));
    }

    public AjaxBreadCrumbPanelLink(final String id, final IPluginContext context, final BreadCrumbPanel caller,
            final Class panelClass, IModel model) {
        this(id, caller.getBreadCrumbModel(), new AjaxBreadCrumbPanelFactory(context, panelClass), model);
    }

    /**
     * Construct.
     * 
     * @param id
     *            The component id
     * @param breadCrumbModel
     *            The bread crumb model
     * @param breadCrumbPanelFactory
     *            The factory to create bread crumb panels
     */
    public AjaxBreadCrumbPanelLink(final String id, final IBreadCrumbModel breadCrumbModel,
            final AjaxBreadCrumbPanelFactory breadCrumbPanelFactory) {
        super(id, breadCrumbModel);

        if (breadCrumbModel == null) {
            throw new IllegalArgumentException("argument breadCrumbModel must be not null");
        }
        if (breadCrumbPanelFactory == null) {
            throw new IllegalArgumentException("argument breadCrumbPanelFactory must be not null");
        }

        this.model = null;
        this.breadCrumbModel = breadCrumbModel;
        this.breadCrumbPanelFactory = breadCrumbPanelFactory;
    }

    public AjaxBreadCrumbPanelLink(final String id, final IBreadCrumbModel breadCrumbModel,
            final AjaxBreadCrumbPanelFactory breadCrumbPanelFactory, IModel model) {
        super(id, breadCrumbModel);

        if (breadCrumbModel == null) {
            throw new IllegalArgumentException("argument breadCrumbModel must be not null");
        }
        if (model == null) {
            throw new IllegalArgumentException("argument model must be not null");
        }
        if (breadCrumbPanelFactory == null) {
            throw new IllegalArgumentException("argument breadCrumbPanelFactory must be not null");
        }
        this.model = model;
        this.breadCrumbModel = breadCrumbModel;
        this.breadCrumbPanelFactory = breadCrumbPanelFactory;
    }

    /**
     * Uses the set factory for creating a new instance of {@link IBreadCrumbParticipant}.
     * 
     * @see org.apache.wicket.extensions.breadcrumb.BreadCrumbLink#getParticipant(java.lang.String)
     */
    protected final IBreadCrumbParticipant getParticipant(String componentId) {
        if (model != null) {
            return breadCrumbPanelFactory.create(componentId, breadCrumbModel, model);
        }
        return breadCrumbPanelFactory.create(componentId, breadCrumbModel);
    }

}
