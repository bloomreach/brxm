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
package org.hippoecm.frontend.editor.builder;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.behaviors.EventStoppingDecorator;
import org.hippoecm.frontend.editor.builder.EditorContext.Mode;
import org.hippoecm.frontend.editor.layout.ILayoutContext;
import org.hippoecm.frontend.editor.layout.ILayoutPad;
import org.hippoecm.frontend.editor.layout.ILayoutTransition;
import org.hippoecm.frontend.editor.layout.LayoutContext;
import org.hippoecm.frontend.editor.layout.RenderContext;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderPluginEditorPlugin extends RenderPlugin implements ILayoutAware {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RenderPluginEditorPlugin.class);

    // control of own location in parent
    private BuilderContext builderContext;
    private ILayoutContext layoutContext;

    // descriptor of own layout, for rearranging children
    private RenderContext renderContext;

    protected IClusterControl previewControl;
    private IObserver configObserver;
    private Map<String, ChildTracker> trackers;
    private WebMarkupContainer container;

    public RenderPluginEditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        renderContext = new RenderContext(context, config);
        builderContext = new BuilderContext(context, config);
        final boolean editable = (builderContext.getMode() == Mode.EDIT);

        container = new WebMarkupContainer("head");
        container.setOutputMarkupId(true);

        // add transitions from parent container
        container.add(new RefreshingView<ILayoutTransition>("transitions") {
            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<IModel<ILayoutTransition>> getItemModels() {
                final Iterator<ILayoutTransition> transitionIter = getTransitionIterator();
                return new Iterator<IModel<ILayoutTransition>>() {

                    public boolean hasNext() {
                        return transitionIter.hasNext();
                    }

                    public IModel<ILayoutTransition> next() {
                        return new Model<ILayoutTransition>(transitionIter.next());
                    }

                    public void remove() {
                        transitionIter.remove();
                    }

                };
            }

            @Override
            protected void populateItem(Item<ILayoutTransition> item) {
                final ILayoutTransition transition = item.getModelObject();
                AjaxLink<Void> link = new AjaxLink<Void>("link") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        layoutContext.apply(transition);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new EventStoppingDecorator(super.getAjaxCallDecorator());
                    }

                };
                link.setVisible(editable);
                link.add(new AttributeAppender("class", new Model<String>(transition.getName()), " "));
                item.add(link);
            }

        });

        container.add(new AjaxLink("remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (validateDelete()) {
                    builderContext.delete();
                }

            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new EventStoppingDecorator(super.getAjaxCallDecorator());
            }

        }.setVisible(editable));

        container.add(new AttributeAppender("class", new IModel() {
            private static final long serialVersionUID = 1L;

            public Object getObject() {
                if (builderContext.hasFocus()) {
                    return "active";
                }
                return "";
            }

            public void setObject(Object object) {
            }

            public void detach() {
            }
        }, " "));
        add(container);

        if (editable) {
            add(new AjaxEventBehavior("onclick") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    builderContext.focus();
                }

                @Override
                protected IAjaxCallDecorator getAjaxCallDecorator() {
                    return new EventStoppingDecorator(super.getAjaxCallDecorator());
                }

            });

            builderContext.addBuilderListener(new IBuilderListener() {
                private static final long serialVersionUID = 1L;

                public void onBlur() {
                    AjaxRequestTarget target = AjaxRequestTarget.get();
                    if (target != null) {
                        target.addComponent(container);
                    }
                }

                public void onFocus() {
                    AjaxRequestTarget target = AjaxRequestTarget.get();
                    if (target != null) {
                        target.addComponent(container);
                    }
                }

            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        updatePreview();

        final IPluginConfig editedConfig = builderContext.getEditablePluginConfig();
        getPluginContext().registerService(configObserver = new IObserver<IPluginConfig>() {
            private static final long serialVersionUID = 1L;

            public IPluginConfig getObservable() {
                return editedConfig;
            }

            public void onEvent(Iterator<? extends IEvent<IPluginConfig>> events) {
                updatePreview();
            }

        }, IObserver.class.getName());
    }

    @Override
    protected void onStop() {
        getPluginContext().unregisterService(configObserver, IObserver.class.getName());
        super.onStop();
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);

        if (previewControl != null) {
            String serviceId = getPluginContext().getReference(this).getServiceId() + ".preview";
            IRenderService previewService = getPluginContext().getService(serviceId, IRenderService.class);
            if (previewService != null) {
                previewService.render(target);
            }
        }
    }

    public void setLayoutContext(ILayoutContext layoutContext) {
        this.layoutContext = layoutContext;
    }

    @Override
    public String getTemplateBuilderPluginId() {
        return builderContext.getPluginId();
    }

    @Override
    public ILayoutAware getDefaultChild() {
        if (trackers != null) {
            for (Map.Entry<String, ChildTracker> entry : trackers.entrySet()) {
                ChildTracker tracker = entry.getValue();
                final ILayoutAware service = tracker.getService();
                if (service != null) {
                    return service;
                }
            }
        }
        return null;
    }

    @Override
    public List<ILayoutAware> getChildren() {
        if (trackers != null) {
            List<ILayoutAware> children = new LinkedList<ILayoutAware>();
            for (Map.Entry<String, ChildTracker> entry : trackers.entrySet()) {
                children.add(entry.getValue().getService());
            }
            return children;
        }
        return Collections.emptyList();
    }

    @Override
    public String getTemplateBuilderExtensionPoint() {
        return null;
    }

    protected ILayoutContext getLayoutContext() {
        return layoutContext;
    }

    protected Iterator<ILayoutTransition> getTransitionIterator() {
        if (layoutContext != null) {
            final ILayoutPad pad = layoutContext.getLayoutPad();
            final Iterator<String> transitionIter = pad.getTransitions().iterator();
            return new Iterator<ILayoutTransition>() {

                public boolean hasNext() {
                    return transitionIter.hasNext();
                }

                public ILayoutTransition next() {
                    return pad.getTransition(transitionIter.next());
                }

                public void remove() {
                    transitionIter.remove();
                }

            };
        } else {
            List<ILayoutTransition> list = Collections.emptyList();
            return list.iterator();
        }
    }

    protected void updatePreview() {
        if (previewControl != null) {
            previewControl.stop();
            previewControl = null;
        }

        unregisterChildTrackers();

        IPluginContext pluginContext = getPluginContext();
        JavaClusterConfig childClusterConfig = new JavaClusterConfig();
        IPluginConfig childPluginConfig = new JavaPluginConfig(getEffectivePluginConfig());

        String serviceId = getPluginContext().getReference(this).getServiceId() + ".preview";
        childPluginConfig.put(RenderService.WICKET_ID, serviceId);
        childClusterConfig.addPlugin(childPluginConfig);

        previewControl = pluginContext.newCluster(childClusterConfig, null);
        previewControl.start();

        IRenderService renderService = pluginContext.getService(serviceId, IRenderService.class);
        if (renderService != null) {
            renderService.bind(this, "preview");
            addOrReplace(renderService.getComponent());
        } else {
            addOrReplace(new EmptyPanel("preview"));
            log.warn("No render service found in plugin preview");
        }

        registerChildTrackers();

        redraw();
    }

    protected boolean validateDelete() {
        return false;
    }

    protected void registerChildTrackers() {
        trackers = new LinkedHashMap<String, ChildTracker>();

        IPluginConfig bare = builderContext.getEditablePluginConfig();
        IPluginConfig effective = getEffectivePluginConfig();
        Map<String, ILayoutPad> pads = renderContext.getLayoutDescriptor().getLayoutPads();
        for (Map.Entry<String, ILayoutPad> entry : pads.entrySet()) {
            String extension = "extension." + entry.getKey();
            if (effective.getString(extension) != null) {
                ChildTracker tracker = newChildTracker(entry.getValue(), bare.getString(extension));

                String effectiveWicketId = effective.getString(extension);
                getPluginContext().registerTracker(tracker, effectiveWicketId);
                trackers.put(effectiveWicketId, tracker);
            }
        }
    }

    protected void unregisterChildTrackers() {
        if (trackers != null) {
            for (Map.Entry<String, ChildTracker> entry : trackers.entrySet()) {
                getPluginContext().unregisterTracker(entry.getValue(), entry.getKey());
            }
            trackers = null;
        }
    }

    protected ChildTracker newChildTracker(ILayoutPad pad, String wicketId) {
        return new ChildTracker(pad, wicketId);
    }

    protected BuilderContext getBuilderContext() {
        return builderContext;
    }

    protected IPluginConfig getEffectivePluginConfig() {
        return getPluginConfig().getPluginConfig("model.effective");
    }

    /**
     * Service tracker that hands out ILayoutControl instances via the ILayoutAware interface.  This allows child render
     * services to reposition themselves.
     */
    protected class ChildTracker extends ServiceTracker<ILayoutAware> {
        private static final long serialVersionUID = 1L;

        private ILayoutAware service;
        private ILayoutPad pad;
        private String wicketId;
        private ILayoutContext control;

        public ChildTracker(ILayoutPad pad, String wicketId) {
            super(ILayoutAware.class);
            this.pad = pad;
            this.wicketId = wicketId;
        }

        @Override
        protected void onServiceAdded(ILayoutAware service, String name) {
            if (control != null) {
                throw new RuntimeException("A ILayoutAware service has already registered at " + name);
            }
            this.service = service;
            control = newLayoutControl(service);
            service.setLayoutContext(control);
        }

        @Override
        protected void onRemoveService(ILayoutAware service, String name) {
            service.setLayoutContext(null);
            control = null;
        }

        public ILayoutAware getService() {
            return service;
        }

        protected ILayoutContext newLayoutControl(ILayoutAware service) {
            return new LayoutContext(builderContext, service, pad, wicketId);
        }

        public ILayoutPad getPad() {
            return pad;
        }
    }

}
