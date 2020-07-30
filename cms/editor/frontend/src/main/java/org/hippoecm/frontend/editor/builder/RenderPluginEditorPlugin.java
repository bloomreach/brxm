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
package org.hippoecm.frontend.editor.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.ajax.NoDoubleClickAjaxLink;
import org.hippoecm.frontend.ajax.NoDoubleClickBehavior;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.attributes.TitleAttribute;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
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
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderPluginEditorPlugin extends RenderPlugin implements ILayoutAware {

    private static final Logger log = LoggerFactory.getLogger(RenderPluginEditorPlugin.class);

    // control of own location in parent
    private BuilderContext builderContext;
    private ILayoutContext layoutContext;

    // descriptor of own layout, for rearranging children
    private RenderContext renderContext;

    protected IClusterControl previewControl;
    private IObserver configObserver;
    private Map<String, ChildTracker> trackers;

    public RenderPluginEditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        renderContext = new RenderContext(context, config);
        builderContext = new BuilderContext(context, config);
        final boolean editable = (builderContext.getMode() == Mode.EDIT);

        final WebMarkupContainer container = new WebMarkupContainer("head");
        container.setOutputMarkupId(true);
        add(container);

        add(ClassAttribute.append(() -> builderContext.hasFocus()
                ? "active"
                : StringUtils.EMPTY));

        // add transitions from parent container
        container.add(new RefreshingView<ILayoutTransition>("transitions") {

            @Override
            protected Iterator<IModel<ILayoutTransition>> getItemModels() {
                final Iterator<ILayoutTransition> transitionIter = getTransitionIterator();
                return new Iterator<IModel<ILayoutTransition>>() {

                    public boolean hasNext() {
                        return transitionIter.hasNext();
                    }

                    public IModel<ILayoutTransition> next() {
                        return new Model<>(transitionIter.next());
                    }

                    public void remove() {
                        transitionIter.remove();
                    }

                };
            }

            @Override
            protected void populateItem(final Item<ILayoutTransition> item) {
                final ILayoutTransition transition = item.getModelObject();
                final AjaxLink<Void> link = new NoDoubleClickAjaxLink<Void>("link") {

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        layoutContext.apply(transition);
                    }

                    @Override
                    protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                        super.updateAjaxAttributes(attributes);
                        attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.STOP);
                    }

                };
                link.setVisible(editable);

                final String name = transition.getName();
                final Icon icon = getTransitionIconByName(name);

                link.add(ClassAttribute.append(name));
                link.add(TitleAttribute.append(name));
                link.add(HippoIcon.fromSprite("icon", icon));
                item.add(link);
            }

        });

        final AjaxLink<Void> removeLink = new NoDoubleClickAjaxLink<Void>("remove") {

            @Override
            public void onClick(final AjaxRequestTarget target) {
                if (validateDelete()) {
                    builderContext.delete();
                }
            }

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.STOP);
            }

        };
        removeLink.setVisible(editable);
        removeLink.add(HippoIcon.fromSprite("icon", Icon.TIMES_CIRCLE));
        container.add(removeLink);


        if (editable) {
            add(new NoDoubleClickBehavior() {
                @Override
                protected void onEvent(final AjaxRequestTarget target) {
                    builderContext.focus();
                }

                @Override
                protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                    super.updateAjaxAttributes(attributes);
                    attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.STOP);
                }
            });

            builderContext.addBuilderListener(new IBuilderListener() {

                public void onBlur() {
                    final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                    if (target != null) {
                        target.add(RenderPluginEditorPlugin.this);
                    }
                }

                public void onFocus() {
                    final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                    if (target != null) {
                        target.add(RenderPluginEditorPlugin.this);
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

            public IPluginConfig getObservable() {
                return editedConfig;
            }

            public void onEvent(final Iterator<? extends IEvent<IPluginConfig>> events) {
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
    public void render(final PluginRequestTarget target) {
        super.render(target);

        if (previewControl != null) {
            final String serviceId = getPluginContext().getReference(this).getServiceId() + ".preview";
            final IRenderService previewService = getPluginContext().getService(serviceId, IRenderService.class);
            if (previewService != null) {
                previewService.render(target);
            }
        }
    }

    public void setLayoutContext(final ILayoutContext layoutContext) {
        this.layoutContext = layoutContext;
    }

    @Override
    public String getTemplateBuilderPluginId() {
        return builderContext.getPluginId();
    }

    @Override
    public ILayoutAware getDefaultChild() {
        if (trackers != null) {
            for (final Map.Entry<String, ChildTracker> entry : trackers.entrySet()) {
                final ChildTracker tracker = entry.getValue();
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
            final List<ILayoutAware> children = new LinkedList<>();
            trackers.forEach((id, childTracker) -> children.add(childTracker.getService()));
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
            final List<ILayoutTransition> list = Collections.emptyList();
            return list.iterator();
        }
    }

    protected void updatePreview() {
        if (previewControl != null) {
            previewControl.stop();
            previewControl = null;
        }

        unregisterChildTrackers();

        final IPluginContext pluginContext = getPluginContext();
        final JavaClusterConfig childClusterConfig = new JavaClusterConfig();
        final IPluginConfig childPluginConfig = new JavaPluginConfig(getEffectivePluginConfig());

        final String serviceId = getPluginContext().getReference(this).getServiceId() + ".preview";
        childPluginConfig.put(RenderService.WICKET_ID, serviceId);
        childClusterConfig.addPlugin(childPluginConfig);

        previewControl = pluginContext.newCluster(childClusterConfig, null);
        previewControl.start();

        final IRenderService renderService = pluginContext.getService(serviceId, IRenderService.class);
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
        return checkWhetherSubtypesHaveEditorTemplates();
    }

    protected void registerChildTrackers() {
        trackers = new LinkedHashMap<>();

        final IPluginConfig bare = builderContext.getEditablePluginConfig();
        final IPluginConfig effective = getEffectivePluginConfig();
        final Map<String, ILayoutPad> pads = renderContext.getLayoutDescriptor().getLayoutPads();
        for (final Map.Entry<String, ILayoutPad> entry : pads.entrySet()) {
            final String extension = "extension." + entry.getKey();
            if (effective.getString(extension) != null) {
                final ChildTracker tracker = newChildTracker(entry.getValue(), bare.getString(extension));

                final String effectiveWicketId = effective.getString(extension);
                getPluginContext().registerTracker(tracker, effectiveWicketId);
                trackers.put(effectiveWicketId, tracker);
            }
        }
    }

    protected void unregisterChildTrackers() {
        if (trackers != null) {
            for (final Map.Entry<String, ChildTracker> entry : trackers.entrySet()) {
                getPluginContext().unregisterTracker(entry.getValue(), entry.getKey());
            }
            trackers = null;
        }
    }

    protected ChildTracker newChildTracker(final ILayoutPad pad, final String wicketId) {
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

        private ILayoutAware service;
        private ILayoutPad pad;
        private String wicketId;
        private ILayoutContext control;

        public ChildTracker(final ILayoutPad pad, final String wicketId) {
            super(ILayoutAware.class);
            this.pad = pad;
            this.wicketId = wicketId;
        }

        @Override
        protected void onServiceAdded(final ILayoutAware service, final String name) {
            if (control != null) {
                throw new RuntimeException("A ILayoutAware service has already registered at " + name);
            }
            this.service = service;
            control = newLayoutControl(service);
            service.setLayoutContext(control);
        }

        @Override
        protected void onRemoveService(final ILayoutAware service, final String name) {
            service.setLayoutContext(null);
            control = null;
        }

        public ILayoutAware getService() {
            return service;
        }

        protected ILayoutContext newLayoutControl(final ILayoutAware service) {
            return new LayoutContext(builderContext, service, pad, wicketId);
        }

        public ILayoutPad getPad() {
            return pad;
        }
    }

    // A utility method to check for subtypes whether they have editor templates or not
    protected boolean checkWhetherSubtypesHaveEditorTemplates() {
        final IPluginContext context = getPluginContext();
        final IPluginConfig config = getPluginConfig();
        final BuilderContext builderContext = getBuilderContext();

        final String field = getBuilderContext().getEditablePluginConfig().getString("field");
        if (StringUtils.isEmpty(field)) {
            return true;
        }

        // Do not delete if there is a subtype that has an editor template for this field
        final List<ITypeDescriptor> subTypes = builderContext.getType().getSubTypes();
        final ITemplateEngine templateEngine = context.getService(config.getString(ITemplateEngine.ENGINE),
                ITemplateEngine.class);

        final List<String> subTypeNames = new ArrayList<>();
        for (final ITypeDescriptor subType : subTypes) {
            try {
                final IClusterConfig template = templateEngine.getTemplate(subType, IEditor.Mode.VIEW);
                for (final IPluginConfig plugin : template.getPlugins()) {
                    if (StringUtils.equals(field, plugin.getString("field"))) {
                        subTypeNames.add(subType.getName());
                    }
                }
            } catch (final TemplateEngineException e) {
                // This error does not prevent deletion.
                log.warn("Misconfiguration of type definition {} encountered.", subType.getName());
            }
        }
        if (!subTypeNames.isEmpty()) {
            error(buildErrorMessage(field, subTypeNames));
            return false;
        }
        return true;
    }

    /**
     * Builds error message with subTypeNames.
     *
     * @param subTypeNames names of subTypes where the field exists.
     * @return the formatted error message.
     */
    private String buildErrorMessage(final String field, final List<String> subTypeNames) {
        final StringResourceModel errorMessageModel = new StringResourceModel("field-is-inherited", this)
                .setParameters(field, Strings.join(", ", subTypeNames));

        return errorMessageModel.getString();
    }

    private Icon getTransitionIconByName(final String name) {
        if (name != null) {
            final String[] split = StringUtils.split(name, " ");
            switch (split[split.length - 1]) {
                case "up": {
                    return Icon.CHEVRON_UP_CIRCLE;
                }
                case "down": {
                    return Icon.CHEVRON_DOWN_CIRCLE;
                }
                case "left": {
                    return Icon.CHEVRON_LEFT_CIRCLE;
                }
                case "right": {
                    return Icon.CHEVRON_RIGHT_CIRCLE;
                }
            }
        }
        log.warn("Undetermined transition icon by name '{}'; the name should be, or have as last word: 'up', 'down', 'left' or 'right'. Returning default icon.", name);
        return Icon.EXCLAMATION_CIRCLE;
    }
}
