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
package org.hippoecm.frontend.service.render;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.RequestContext;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WicketURLDecoder;
import org.apache.wicket.util.string.PrependingStringBuffer;
import org.hippoecm.frontend.IStringResourceProvider;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.IModelListener;
import org.hippoecm.frontend.model.IModelService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBehaviorService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ITranslateService;
import org.hippoecm.frontend.service.PluginRequestTarget;
import org.hippoecm.frontend.service.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRenderService extends Panel implements IModelListener, IRenderService,
        IStringResourceProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(AbstractRenderService.class);

    public static final String WICKET_ID = "wicket.id";
    public static final String MODEL_ID = "wicket.model";
    public static final String VARIANT_ID = "wicket.variant";
    public static final String SKIN_ID = "wicket.skin";
    public static final String CSS_ID = "wicket.css";
    public static final String EXTENSIONS_ID = "wicket.extensions";
    public static final String FEEDBACK = "wicket.feedback";
    public static final String BEHAVIOR = "wicket.behavior";

    private boolean redraw;
    private String wicketServiceId;
    private String wicketId;
    private String modelId;
    private String cssClasses;

    private final IPluginContext context;
    private final IPluginConfig config;
    protected LinkedHashMap<String, ExtensionPoint> children;
    private IRenderService parent;

    public static final HeaderContributor forCss(final String location) {
        return new HeaderContributor(new IHeaderContributor() {
            private static final long serialVersionUID = 1L;

            public void renderHead(IHeaderResponse response) {
                response.renderCSSReference(returnFixedRelativePath(location));
            }
        });
    }

    public static final HeaderContributor forJavaScript(final String location) {
        return new HeaderContributor(new IHeaderContributor() {
            private static final long serialVersionUID = 1L;

            public void renderHead(IHeaderResponse response) {
                response.renderJavascriptReference(returnFixedRelativePath(location));
            }
        });
    }

    // Adds ../ links to make the location relative to the root of the webapp,
    // provided it's not a fully-qualified URL.
    public static final String returnFixedRelativePath(String location) {
        // WICKET-59 allow external URLs, WICKET-612 allow absolute URLs.
        if (location.startsWith("http://") || location.startsWith("https://") || location.startsWith("/")) {
            return location;
        } else {
            return getFixedRelativePathPrefixToContextRoot() + location;
        }
    }

    public static final String getFixedRelativePathPrefixToContextRoot() {
        WebRequest request = (WebRequest) RequestCycle.get().getRequest();

        if (RequestContext.get().isPortletRequest()) {
            return request.getHttpServletRequest().getContextPath() + "/";
        }

        // Prepend to get back to the wicket handler.
        String tmp = RequestCycle.get().getRequest().getRelativePathPrefixToWicketHandler();
        PrependingStringBuffer prepender = new PrependingStringBuffer(tmp);

        String path = WicketURLDecoder.PATH_INSTANCE.decode(request.getPath());
        if (path == null || path.length() == 0) {
            path = "";
        }

        // Now prepend to get back from the wicket handler to the root context.

        // Find the absolute path for the wicket filter/servlet
        String wicketPath = "";

        // We're running as a filter.
        // Note: do not call RequestUtils.decode() on getServletPath ... it is
        //       already url-decoded (JIRA WICKET-1624)
        String servletPath = request.getServletPath();

        // We need to substitute the %3A (or the other way around) to be able to
        // get a good match, as parts of the path may have been escaped while
        // others arent

        // Add check if path is empty
        if (!"".equals(path) && servletPath.endsWith(path)) {
            int len = servletPath.length() - path.length() - 1;
            if (len < 0) {
                len = 0;
            }
            wicketPath = servletPath.substring(0, len);
        }
        // We're running as a servlet
        else {
            wicketPath = servletPath;
        }

        int start = 0;
        // add skip for starting slash
        if (wicketPath.startsWith("/")) {
            start = 1;
        }
        for (int i = start; i < wicketPath.length(); i++) {
            if (wicketPath.charAt(i) == '/') {
                prepender.prepend("../");
            }
        }
        return prepender.toString();
    }

    public AbstractRenderService(IPluginContext context, IPluginConfig properties) {
        super("id", getPluginModel(context, properties));
        this.context = context;
        this.config = properties;

        setOutputMarkupId(true);
        redraw = false;

        wicketId = "service.render";

        this.children = new LinkedHashMap<String, ExtensionPoint>();

        if (properties.getString(WICKET_ID) != null) {
            this.wicketServiceId = properties.getString(WICKET_ID);
        } else {
            log.warn("No service id ({}) defined", WICKET_ID);
        }

        if (properties.getString(MODEL_ID) != null) {
            modelId = properties.getString(MODEL_ID);
            if (modelId != null) {
                context.registerService(this, modelId);
            }
        }

        String[] extensions = config.getStringArray(EXTENSIONS_ID);
        if (extensions != null) {
            for (String extension : extensions) {
                addExtensionPoint(extension);
            }
        }

        StringBuffer sb;

        cssClasses = null;
        String[] classes = config.getStringArray(CSS_ID);
        if (classes != null) {
            sb = null;
            for (String cssClass : classes) {
                if (sb == null) {
                    sb = new StringBuffer(cssClass);
                } else {
                    sb.append(" ").append(cssClass);
                }
            }
            if (sb != null) {
                cssClasses = sb.toString();
            }
        }

        String[] skins = config.getStringArray(SKIN_ID);
        if (skins != null) {
            for (String skin : skins) {
                HeaderContributor cssContributor = forCss(skin);
                add(cssContributor);
                context.registerService(cssContributor, IDialogService.class.getName());
            }
        }

        if (config.getString(FEEDBACK) != null) {
            context.registerService(new ContainerFeedbackMessageFilter(this), config.getString(FEEDBACK));
        } else {
            log.debug("No feedback id {} defined to register message filter", FEEDBACK);
        }

        if (config.getStringArray(BEHAVIOR) != null) {
            ServiceTracker<IBehaviorService> tracker = new ServiceTracker<IBehaviorService>(IBehaviorService.class) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onServiceAdded(IBehaviorService behavior, String name) {
                    String path = behavior.getComponentPath();
                    if (path != null) {
                        Component component = get(path);
                        if (component != null) {
                            component.add(behavior);
                        } else {
                            log.warn("No component found under {}", path);
                        }
                    } else {
                        add(behavior);
                    }
                }

                @Override
                public void onRemoveService(IBehaviorService behavior, String name) {
                    String path = behavior.getComponentPath();
                    if (path != null) {
                        Component component = get(behavior.getComponentPath());
                        if (component != null) {
                            component.remove(behavior);
                        }
                    } else {
                        remove(behavior);
                    }
                }
            };

            for (String name : config.getStringArray(BEHAVIOR)) {
                context.registerTracker(tracker, name);
            }
        }

        context.registerService(this, wicketServiceId);
    }

    // override model change methods

    @Override
    public Component setModel(IModel model) {
        IModelService service = context.getService(modelId, IModelService.class);
        if (service != null) {
            service.setModel(model);
        } else {
            updateModel(model);
        }
        return this;
    }

    public final void updateModel(IModel model) {
        super.setModel(model);
    }

    // override methods with configuration data

    @Override
    public String getVariation() {
        if (config.getString(VARIANT_ID) != null) {
            return config.getString(VARIANT_ID);
        }
        // don't inherit variation from Wicket ancestor
        return null;
    }

    // utility routines for subclasses

    protected IPluginContext getPluginContext() {
        return context;
    }

    protected IPluginConfig getPluginConfig() {
        return config;
    }

    protected void redraw() {
        redraw = true;
    }

    protected void addExtensionPoint(final String extension) {
        ExtensionPoint extPt = createExtensionPoint(extension);
        children.put(extension, extPt);
        context.registerTracker(extPt, config.getString(extension));
    }

    protected abstract ExtensionPoint createExtensionPoint(String extension);

    protected void removeExtensionPoint(String name) {
        context.unregisterTracker(children.get(name), config.getString(name));
        children.remove(name);
    }

    protected IDialogService getDialogService() {
        return context.getService(IDialogService.class.getName(), IDialogService.class);
    }

    // allow styling

    @Override
    public void onComponentTag(final ComponentTag tag) {
        super.onComponentTag(tag);

        if (cssClasses != null) {
            tag.put("class", cssClasses);
        }
    }

    // implement IRenderService

    public Component getComponent() {
        return this;
    }

    public void render(PluginRequestTarget target) {
        if (redraw) {
            target.addComponent(this);
            redraw = false;
        }
        for (Map.Entry<String, ExtensionPoint> entry : children.entrySet()) {
            for (IRenderService service : entry.getValue().getChildren()) {
                service.render(target);
            }
        }
    }

    public void focus(IRenderService child) {
        IRenderService parent = getParentService();
        if (parent != null) {
            parent.focus(this);
        }
    }

    @Override
    public String getId() {
        return wicketId;
    }

    public void bind(IRenderService parent, String wicketId) {
        this.parent = parent;
        this.wicketId = wicketId;
    }

    public void unbind() {
        this.parent = null;
        wicketId = "service.render.unbound";
    }

    public IRenderService getParentService() {
        return parent;
    }

    // implement IStringResourceProvider
    @SuppressWarnings("unchecked")
    public String getString(Map<String, String> criteria) {
        String[] translators = config.getStringArray(ITranslateService.TRANSLATOR_ID);
        if (translators != null) {
            for (String translatorId : translators) {
                ITranslateService translator = (ITranslateService) context.getService(translatorId,
                        ITranslateService.class);
                if (translator != null) {
                    String translation = translator.translate(criteria);
                    if (translation != null) {
                        return translation;
                    }
                }
            }
        }
        return null;
    }

    // detach

    @Override
    protected void onDetach() {
        redraw = false;
        config.detach();
        super.onDetach();
    }

    private static IModel getPluginModel(IPluginContext context, IPluginConfig properties) {
        String modelId = properties.getString(MODEL_ID);
        if (modelId != null) {
            IModelService service = context.getService(modelId, IModelService.class);
            if (service != null) {
                return service.getModel();
            }
        }
        return null;
    }

    protected abstract class ExtensionPoint extends ServiceTracker<IRenderService> {
        private static final long serialVersionUID = 1L;

        private final List<IRenderService> list;
        protected String extension;

        ExtensionPoint(String extension) {
            super(IRenderService.class);
            this.extension = extension;
            this.list = new LinkedList<IRenderService>();
        }

        public List<IRenderService> getChildren() {
            return list;
        }

        @Override
        public void onServiceAdded(IRenderService service, String name) {
            list.add(service);
        }

        @Override
        public void onServiceChanged(IRenderService service, String name) {
        }

        @Override
        public void onRemoveService(IRenderService service, String name) {
            list.remove(service);
        }
    }
}
