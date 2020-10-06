/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang3.Validate;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.hippoecm.frontend.behaviors.ContextMenuBehavior;
import org.hippoecm.frontend.behaviors.IContextMenu;
import org.hippoecm.frontend.behaviors.OpenInContentPerspectiveBehavior;
import org.hippoecm.frontend.dialog.DialogServiceFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.event.IRefreshable;
import org.hippoecm.frontend.model.event.ObservableRegistry;
import org.hippoecm.frontend.observation.JcrObservationManager;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceTracker;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugin.config.impl.IApplicationFactory;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.PluginConfigFactory;
import org.hippoecm.frontend.plugin.impl.PluginContext;
import org.hippoecm.frontend.plugin.impl.PluginManager;
import org.hippoecm.frontend.service.IController;
import org.hippoecm.frontend.service.INavAppSettingsService;
import org.hippoecm.frontend.service.INestedBrowserContextService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.NavAppSettings;
import org.hippoecm.frontend.service.NestedBrowserContextService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;

public class PluginPage extends Home implements IServiceTracker<IRenderService> {

    private static final long serialVersionUID = 1L;
    private static final String RELATIVE_FAVICON_PATH = "navapp-assets/favicon.ico";

    private final int pageId;

    private final PluginManager mgr;
    private final PluginContext context;
    private IRenderService root;
    private final DialogServiceFactory dialogService;
    private final ObservableRegistry obRegistry;
    private final IPluginConfigService pluginConfigService;
    private final ContextMenuBehavior menuBehavior;

    public PluginPage() {
        this(PluginUserSession.get().getApplicationFactory());
    }

    public PluginPage(IApplicationFactory appFactory) {
        Task pageInitTask = null;

        try {
            if (HDC.isStarted()) {
                pageInitTask = HDC.getCurrentTask().startSubtask(PluginPage.class.getSimpleName() + ".init");
            }

            pageId = ((PluginUserSession) UserSession.get()).getPageId();

            add(new EmptyPanel("root"));

            mgr = new PluginManager(this);
            context = new PluginContext(mgr, new JavaPluginConfig("home"));
            context.connect(null);

            context.registerTracker(this, "service.root");

            if (Main.isCmsApplication() && Main.hasNoIFrameParameter()) {
                pluginConfigService = new PluginConfigFactory(PluginApplication.PLUGIN_APPLICATION_VALUE_NAVAPP, appFactory);
            } else {
                pluginConfigService = new PluginConfigFactory(UserSession.get(), appFactory);
            }
            context.registerService(pluginConfigService, IPluginConfigService.class.getName());

            final String configurationParameter = PluginApplication.get()
                    .getConfigurationParameter(Main.PLUGIN_APPLICATION_HIDE_PERSPECTIVE_MENU_PARAMETER
                            , Boolean.FALSE.toString());
            final boolean showPerspectiveMenu = Boolean.parseBoolean(configurationParameter);
            context.registerService(new NestedBrowserContextService(!showPerspectiveMenu)
                    , INestedBrowserContextService.class.getName());

            obRegistry = new ObservableRegistry(context, null);
            obRegistry.startObservation();

            dialogService = new DialogServiceFactory("dialog");
            dialogService.init(context, IDialogService.class.getName());
            add(dialogService.getComponent());

            context.registerService(this, Home.class.getName());
            registerGlobalBehaviorTracker();
            add(new OpenInContentPerspectiveBehavior(context));

            add(menuBehavior = new ContextMenuBehavior());

            IClusterConfig pluginCluster = pluginConfigService.getDefaultCluster();
            IClusterControl clusterControl = context.newCluster(pluginCluster, null);
            clusterControl.start();

            IController controller = context.getService(IController.class.getName(), IController.class);

            if (controller != null) {
                WebRequest request = (WebRequest) RequestCycle.get().getRequest();
                controller.process(request.getRequestParameters());
            }

            add(new Label("pageTitle", getString("page.title", null, "Bloomreach Experience")));
            add(new ExternalLink("faviconLink", RELATIVE_FAVICON_PATH));
        } finally {
            if (pageInitTask != null) {
                pageInitTask.stop();
            }
        }
    }

    private void registerGlobalBehaviorTracker() {
        final String[] serviceIds = {
                context.getReference(this).getServiceId(),
                Behavior.class.getName()
        };
        for (String serviceId : serviceIds) {
            ServiceTracker<Behavior> tracker = new ServiceTracker<Behavior>(Behavior.class) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onServiceAdded(Behavior behavior, String name) {
                    add(behavior);
                }

                @Override
                public void onRemoveService(Behavior behavior, String name) {
                    remove(behavior);
                }
            };
            context.registerTracker(tracker, serviceId);
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        Task pageRenderHeadTask = null;

        try {
            if (HDC.isStarted()) {
                pageRenderHeadTask = HDC.getCurrentTask().startSubtask(PluginPage.class.getSimpleName() + ".renderHead");
            }

            super.renderHead(response);
            CoreLibrariesContributor.contribute(Application.get(), response);

            showPerspectiveMenu(response);

            if (WebApplicationHelper.isDevelopmentMode()) {
                addDevelopmentModeCssClassToHtml(response);
            }
        } finally {
            if (pageRenderHeadTask != null) {
                pageRenderHeadTask.stop();
            }
        }
    }

    private void showPerspectiveMenu(final IHeaderResponse response) {
        final INestedBrowserContextService nestedBrowserContextService =
                context.getService(INestedBrowserContextService.class.getName(), INestedBrowserContextService.class);
        final String message = String.format("%s should not be null, make sure it's registered on the %s"
                , INestedBrowserContextService.class.getName(), IPluginContext.class.getName());
        Validate.notNull(nestedBrowserContextService, message);
        if (!nestedBrowserContextService.hidePerspectiveMenu()) {
            final String script = String.format("$(\"div#ft\").addClass(\"%s\")", "show-perspective-menu");
            response.render(JavaScriptHeaderItem.forScript(script, "show-perspective-menu"));
        }
    }

    private static void addDevelopmentModeCssClassToHtml(final IHeaderResponse response) {
        final String script = "$('html').addClass('wicket-development-mode');";
        response.render(JavaScriptHeaderItem.forScript(script, "html-wicket-development-mode"));
    }

    public Component getComponent() {
        return this;
    }

    /**
     * Refresh the JCR session, i.e. invalidate (cached) subtrees for which an event has been received.
     */
    public void refresh() {
        Task refreshTask = null;

        try {
            if (HDC.isStarted()) {
                refreshTask = HDC.getCurrentTask().startSubtask(PluginPage.class.getSimpleName() + ".refresh");
            }

            // objects may be invalid after refresh, so reacquire them when needed
            detach();

            // refresh session
            JcrObservationManager.getInstance().refreshSession();
        } finally {
            if (refreshTask != null) {
                refreshTask.stop();
            }
        }
    }

    /**
     * Notify refreshables and listeners in the page for which events have been received.
     */
    public void processEvents() {
        Task pageProcessEventsTask = null;

        try {
            if (HDC.isStarted()) {
                pageProcessEventsTask = HDC.getCurrentTask().startSubtask(PluginPage.class.getSimpleName() + ".processEvents");
            }

            refresh();

            // re-evaluate models
            context.getServices(IRefreshable.class.getName(), IRefreshable.class).forEach(org.hippoecm.frontend.model.event.IRefreshable::refresh);

            // process JCR events
            JcrObservationManager.getInstance().processEvents();
        } finally {
            if (pageProcessEventsTask != null) {
                pageProcessEventsTask.stop();
            }

            setFlag(FLAG_RESERVED1, false);
        }
    }

    @Override
    protected void onInitialize() {
        Task initTask = null;

        try {
            if (HDC.isStarted()) {
                initTask = HDC.getCurrentTask().startSubtask(PluginPage.class.getSimpleName() + ".onInitialize");
            }

            super.onInitialize();
        } finally {
            if (initTask != null) {
                initTask.stop();
            }
        }
    }

    @Override
    protected void onBeforeRender() {
        Task beforeRenderTask = null;

        try {
            if (HDC.isStarted()) {
                beforeRenderTask = HDC.getCurrentTask().startSubtask(PluginPage.class.getSimpleName() + ".onBeforeRender");
            }

            super.onBeforeRender();
        } finally {
            if (beforeRenderTask != null) {
                beforeRenderTask.stop();
            }
        }
    }

    @Override
    protected void onAfterRender() {
        Task afterRenderTask = null;

        try {
            if (HDC.isStarted()) {
                afterRenderTask = HDC.getCurrentTask().startSubtask(PluginPage.class.getSimpleName() + ".onAfterRender");
            }

            super.onAfterRender();
        } finally {
            if (afterRenderTask != null) {
                afterRenderTask.stop();
            }
        }
    }

    public void render(PluginRequestTarget target) {
        Task pageRenderTask = null;

        try {
            if (HDC.isStarted()) {
                pageRenderTask = HDC.getCurrentTask().startSubtask(PluginPage.class.getSimpleName() + ".render");
            }

            if (root != null) {
                root.render(target);
            }

            dialogService.render(target);
            menuBehavior.checkMenus(target);
        } finally {
            if (pageRenderTask != null) {
                pageRenderTask.stop();
            }
        }
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

    public void addService(IRenderService service, String name) {
        final INestedBrowserContextService nestedBrowserContextService =
                context.getService(INestedBrowserContextService.class.getName(), INestedBrowserContextService.class);
        final String message = String.format("%s should not be null, make sure it's registered on the %s"
                , INestedBrowserContextService.class.getName(), IPluginContext.class.getName());
        Validate.notNull(nestedBrowserContextService, message);

        if (nestedBrowserContextService.showNavigationApplication()) {
            final INavAppSettingsService navAppSettingsService = context.getService(INavAppSettingsService.SERVICE_ID, INavAppSettingsService.class);
            final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(RequestCycle.get().getRequest());
            final NavAppPanel navAppPanel = new NavAppPanel("root", navAppSettings);
            navAppPanel.setRenderBodyOnly(true);
            replace(navAppPanel);
            replace(new EmptyPanel("dialog"));
        } else {
            root = service;
            root.bind(this, "root");
            replace(root.getComponent());
        }
    }

    public void removeService(IRenderService service, String name) {
        replace(new EmptyPanel("root"));
        root.unbind();
        root = null;
    }

    @Override
    public int getPageId() {
        return pageId;
    }

    @Override
    public void onDetach() {
        Task detachTask = null;

        try {
            if (HDC.isStarted()) {
                detachTask = HDC.getCurrentTask().startSubtask(PluginPage.class.getSimpleName() + ".onDetach");
            }

            context.detach();
            super.onDetach();
        } finally {
            if (detachTask != null) {
                detachTask.stop();
            }
        }
    }

    public void showContextMenu(IContextMenu active) {
        menuBehavior.activate(active);
    }

    @Override
    public void collapseAllContextMenus() {
        menuBehavior.collapseAll();
    }

    @Override
    public void renderPage() {
        collapseAllContextMenus();
        super.renderPage();
    }
}
