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

import java.lang.reflect.Constructor;

import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;

public class AjaxBreadCrumbPanelFactory implements IBreadCrumbPanelFactory {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    /** Class to construct. */
    private Class panelClass;

    public BreadCrumbPanel create(String componentId, IBreadCrumbModel breadCrumbModel) {
        return create(componentId, null, breadCrumbModel);
    }

    /**
     * Construct.
     * 
     * @param panelClass
     *            The class to use for creating instances. Must be of type {@link BreadCrumbPanel},
     *            and must have constructor
     *            {@link BreadCrumbPanel#BreadCrumbPanel(String, IBreadCrumbModel)}
     */
    public AjaxBreadCrumbPanelFactory(final Class panelClass) {
        if (panelClass == null) {
            throw new IllegalArgumentException("argument panelClass must be not null");
        }

        if (!BreadCrumbPanel.class.isAssignableFrom(panelClass)) {
            throw new IllegalArgumentException("argument panelClass (" + panelClass + ") must extend class "
                    + BreadCrumbPanel.class.getName());
        }

        this.panelClass = panelClass;

        // check whether it has the proper constructor
        getConstructor();
    }
    /**
     * Construct.
     * 
     * @param panelClass
     *            The class to use for creating instances. Must be of type {@link BreadCrumbPanel},
     *            and must have constructor
     *            {@link BreadCrumbPanel#BreadCrumbPanel(String, IBreadCrumbModel)}
     */
    public AjaxBreadCrumbPanelFactory(final Class panelClass, IModel model) {
        if (panelClass == null) {
            throw new IllegalArgumentException("argument panelClass must be not null");
        }

        if (!BreadCrumbPanel.class.isAssignableFrom(panelClass)) {
            throw new IllegalArgumentException("argument panelClass (" + panelClass + ") must extend class "
                    + BreadCrumbPanel.class.getName());
        }

        this.panelClass = panelClass;

        // check whether it has the proper constructor
        getModelConstructor();
    }

    /**
     * @see org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory#create(java.lang.String,
     *      org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel)
     */
    public BreadCrumbPanel create(String componentId, IPluginContext context, IBreadCrumbModel breadCrumbModel) {
        Constructor ctor = getConstructor();
        try {
            return (BreadCrumbPanel) ctor.newInstance(new Object[] { componentId, context, breadCrumbModel });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory#create(java.lang.String,
     *      org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel)
     */
    public BreadCrumbPanel create(String componentId, IPluginContext context, IBreadCrumbModel breadCrumbModel, IModel model) {
        Constructor ctor = getModelConstructor();
        try {
            return (BreadCrumbPanel) ctor.newInstance(new Object[] { componentId, context, breadCrumbModel, model });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the proper constructor of the panel class.
     * 
     * @return The constructor.
     */
    private final Constructor getConstructor() {
        try {
            Constructor ctor = panelClass.getConstructor(new Class[] { String.class, IPluginContext.class,
                    IBreadCrumbModel.class });
            return ctor;
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the proper constructor of the panel class.
     * 
     * @return The constructor.
     */
    private final Constructor getModelConstructor() {
        try {
            Constructor ctor = panelClass.getConstructor(new Class[] { String.class, IPluginContext.class,
                    IBreadCrumbModel.class, IModel.class });
            return ctor;
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
