/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.WicketAjaxReference;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.WicketEventReference;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.hippoecm.frontend.behaviors.ContextMenuBehavior;
import org.hippoecm.frontend.behaviors.IContextMenu;
import org.hippoecm.frontend.dialog.DialogServiceFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.event.IRefreshable;
import org.hippoecm.frontend.model.event.ObservableRegistry;
import org.hippoecm.frontend.observation.JcrObservationManager;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IServiceTracker;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugin.config.impl.IApplicationFactory;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.PluginConfigFactory;
import org.hippoecm.frontend.plugin.impl.PluginContext;
import org.hippoecm.frontend.plugin.impl.PluginManager;
import org.hippoecm.frontend.service.IController;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.session.UserSession;

public class PluginPage extends Home implements IServiceTracker<IRenderService> {

    private static final long serialVersionUID = 1L;

    private PluginManager mgr;
    private PluginContext context;
    private IRenderService root;
    private DialogServiceFactory dialogService;
    private ObservableRegistry obRegistry;
    private IPluginConfigService pluginConfigService;
    private ContextMenuBehavior menuBehavior;

    public PluginPage() {
        this(PluginUserSession.get().getApplicationFactory());
    }

    public PluginPage(IApplicationFactory appFactory) {
        add(new EmptyPanel("root"));

        mgr = new PluginManager(this);
        context = new PluginContext(mgr, new JavaPluginConfig("home"));
        context.connect(null);

        context.registerTracker(this, "service.root");

        pluginConfigService = new PluginConfigFactory(UserSession.get(), appFactory);
        context.registerService(pluginConfigService, IPluginConfigService.class.getName());

        obRegistry = new ObservableRegistry(context, null);
        obRegistry.startObservation();

        dialogService = new DialogServiceFactory("dialog");
        dialogService.init(context, IDialogService.class.getName());
        add(dialogService.getComponent());

        context.registerService(this, Home.class.getName());
        registerGlobalBehaviorTracker();

        add(JavascriptPackageResource.getHeaderContribution(WicketEventReference.INSTANCE));
        add(JavascriptPackageResource.getHeaderContribution(WicketAjaxReference.INSTANCE));
        
        add(menuBehavior = new ContextMenuBehavior());

        IClusterConfig pluginCluster = pluginConfigService.getDefaultCluster();
        IClusterControl clusterControl = context.newCluster(pluginCluster, null);
        clusterControl.start();

        IController controller = context.getService(IController.class.getName(), IController.class);
        if (controller != null) {
            WebRequest request = (WebRequest) RequestCycle.get().getRequest();
            controller.process(request.getParameterMap());
        }
    }

    private void registerGlobalBehaviorTracker() {
        final String[] serviceIds = {
                context.getReference(this).getServiceId(),
                IBehavior.class.getName()
        };
        for (String serviceId : serviceIds) {
            ServiceTracker<IBehavior> tracker = new ServiceTracker<IBehavior>(IBehavior.class) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onServiceAdded(IBehavior behavior, String name) {
                    add(behavior);
                }

                @Override
                public void onRemoveService(IBehavior behavior, String name) {
                    remove(behavior);
                }
            };
            context.registerTracker(tracker, serviceId);
        }
    }

    public Component getComponent() {
        return this;
    }

    /**
     * Refresh the JCR session, i.e. invalidate (cached) subtrees for which an event has been received.
     */
    public void refresh() {
        // objects may be invalid after refresh, so reacquire them when needed
        detach();

        // refresh session
        JcrObservationManager.getInstance().refreshSession();
    }

    /**
     * Notify refreshables and listeners in the page for which events have been received.
     */
    public void processEvents() {
        refresh();
        try {
            // re-evaluate models
            for (IRefreshable refreshable : context.getServices(IRefreshable.class.getName(), IRefreshable.class)) {
                refreshable.refresh();
            }

            // process JCR events
            JcrObservationManager.getInstance().processEvents();
        } finally {
            setFlag(FLAG_RESERVED1, false);
        }
    }

    public void render(PluginRequestTarget target) {
        if (root != null) {
            root.render(target);
        }
        dialogService.render(target);
        menuBehavior.checkMenus(target);
    }

    public void focus(IRenderService child) {
    }

    public void bind(IRenderService parent, String wicketId) {
    }

    public void unbind() {
    }

    public IRenderService getParentService() {
        return null;
    }

    public String getServiceId() {
        return null;
    }

    // DO NOT CALL THIS METHOD
    // Use the IPluginContext to access the plugin manager
    public final PluginManager getPluginManager() {
        return mgr;
    }

    @Override
    protected void setHeaders(WebResponse response) {
        response.setHeader("Pragma", "no-cache");
        // FF3 bug: no-store shouldn't be necessary
        response.setHeader("Cache-Control", "no-store, no-cache, max-age=0, must-revalidate"); // no-store
        response.setHeader("X-Frame-Options", "sameorigin");
    }

    @Override
    public final void onNewBrowserWindow() {
        setResponsePage(getClass());
    }

    public void addService(IRenderService service, String name) {
        root = service;
        root.bind(this, "root");
        replace(root.getComponent());
    }

    public void removeService(IRenderService service, String name) {
        replace(new EmptyPanel("root"));
        root.unbind();
        root = null;
    }

    public void updateService(IRenderService service, String name) {
    }

    @Override
    public void onDetach() {
        context.detach();
        super.onDetach();
    }

    public void showContextMenu(IContextMenu active) {
        menuBehavior.activate(active);
    }

    @Override
    public void collapseAllContextMenus() {
        menuBehavior.collapseAll();
    }
}
