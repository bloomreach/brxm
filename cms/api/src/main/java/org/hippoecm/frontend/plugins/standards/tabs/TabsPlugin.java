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
package org.hippoecm.frontend.plugins.standards.tabs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin that manages a number of {@link IRenderService}s using a tabbing interface.
 * <p/>
 * Configuration:
 * <ul>
 * <li><b>title.maxlength</b><br>
 * The maximum length (in characters) of the title.  When exceeded, the title
 * will be shown truncated with ellipses.  The title attribute will contain the
 * full title.
 * <li><b>icon.size</b><br>
 * The size of the icon in the tab.  Can be one of the {@link IconSize}
 * sizes.  By default, 'tiny' will be used.
 * </ul>
 */
public class TabsPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TabsPlugin.class);

    public static final String TAB_ID = "tabs";
    public static final String MAX_TAB_TITLE_LENGTH = "title.maxlength";
    public static final String TAB_ICON_SIZE = "icon.size";

    private final TabbedPanel tabbedPanel;
    private RenderService emptyPanel;
    private final List<Tab> tabs;
    private int selectCount;
    private boolean openleft = false;

    private int previousSelectedTabIndex = -1;

    public TabsPlugin(IPluginContext context, IPluginConfig properties) {
        super(context, properties);

        setOutputMarkupId(true);

        IPluginConfig panelConfig = new JavaPluginConfig(properties.getName() + "-empty-panel");
        panelConfig.put("wicket.id", properties.getString(TAB_ID));
        panelConfig.put("wicket.behavior", properties.getString("tabbedpanel.behavior"));

        if (properties.containsKey("tabbedpanel.openleft")) {
            openleft = properties.getBoolean("tabbedpanel.openleft");
        }

        emptyPanel = new RenderService(context, panelConfig);
        context.registerService(emptyPanel, properties.getString(TAB_ID));

        MarkupContainer tabsContainer = new TabsContainer();
        tabsContainer.setOutputMarkupId(true);

        tabs = new ArrayList<Tab>();
        add(tabbedPanel = newTabbedPanel("tabs", tabs, tabsContainer));
        tabbedPanel.setMaxTitleLength(properties.getInt(MAX_TAB_TITLE_LENGTH, 12));
        tabbedPanel.setIconType(IconSize.getIconSize(properties.getString(TAB_ICON_SIZE, "tiny")));

        if (properties.containsKey("tabs.container.id")) {
            JavaPluginConfig containerConfig = new JavaPluginConfig(properties.getName() + "-tabs-container");
            containerConfig.put("wicket.id", properties.getString("tabs.container.id"));
            RenderService containerService = new TabsContainerService(context, containerConfig);
            containerService.add(tabsContainer);
        } else {
            tabbedPanel.add(tabsContainer);
        }

        selectCount = 0;
        ServiceTracker<IRenderService> tabsTracker = new ServiceTracker<IRenderService>(IRenderService.class) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onServiceAdded(IRenderService service, String name) {
                // add the plugin
                service.bind(TabsPlugin.this, TabbedPanel.TAB_PANEL_ID);
                if (service != emptyPanel) {
                    Tab tabbie = new Tab(service);
                    if (openleft) {
                        tabs.add(0, tabbie);
                        tabbedPanel.setSelectedTab(0);
                        tabbedPanel.addFirst();
                    } else {
                        tabs.add(tabbie);
                        if (tabs.size() == 1) {
                            tabbedPanel.setSelectedTab(0);
                        }
                        tabbedPanel.addLast();
                    }
                }
            }

            @Override
            public void onRemoveService(IRenderService service, String name) {
                Tab tabbie = findTabbie(service);
                if (tabbie != null) {
                    tabs.remove(tabbie);
                    tabbie.destroy();
                    if (tabs.size() == 0) {
                        tabbedPanel.setSelectedTab(-1);
                    }
                    service.unbind();
                    tabbedPanel.removed(tabbie);
                }
            }
        };
        context.registerTracker(tabsTracker, properties.getString(TAB_ID));

    }

    protected TabbedPanel newTabbedPanel(String id, List<Tab> tabs, MarkupContainer tabsContainer) {
        return new TabbedPanel(id, TabsPlugin.this, tabs, tabsContainer);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        JavaScriptResourceReference tabsPluginJs = new JavaScriptResourceReference(TabsPlugin.class, "TabsPlugin.js");
        response.render(JavaScriptReferenceHeaderItem.forReference(tabsPluginJs));
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        tabbedPanel.render(target);
        for (Tab tabbie : tabs) {
            tabbie.renderer.render(target);
        }
    }

    @Override
    public void focus(IRenderService child) {
        Tab tabbie = findTabbie(child);
        if (tabbie != null) {
            tabbie.select();
            onSelectTab(tabs.indexOf(tabbie));
        }
        super.focus(child);
    }

    @Override
    public void onDetach() {
        for (Tab tab : tabs) {
            tab.detach();
        }
        super.onDetach();
    }

    @Override
    public String getVariation() {
        String variation = super.getVariation();
        if (Strings.isEmpty(variation)) {
            if (getPluginConfig().containsKey("tabs.container.id")) {
                return "split";
            }
        }
        return variation;
    }

    public boolean hasOpenTabs() {
        return this.tabs.size() > 0;
    }

    Panel getEmptyPanel() {
        return emptyPanel;
    }

    void onSelect(Tab tabbie, AjaxRequestTarget target) {
        tabbie.renderer.focus(null);
        onSelectTab(tabs.indexOf(tabbie));
        fireTabSelectionEvent(tabbie, target);
    }

    private void fireTabSelectionEvent(final Tab tab, final AjaxRequestTarget target) {
        final String tabId = tab.getDecoratorId();
        if (tabId != null) {
            final String fireEventJavascript = String.format("window.Hippo.fireTabSelectionEvent('%s');", tabId);
            target.appendJavaScript(fireEventJavascript);
        }
    }

    /**
     * Get the list of tabs that are modified.
     *
     * @param ignoreTab - Ignore this tab while getting the list of tabs. Can be null, if all the changed tabs
     * need to be retrieved.
     * @return List<Tab> of changed tabs, empty list if there are none.
     */
    private List<Tab> getChangedTabs(Tab ignoreTab) {
        List<Tab> changedTabs = new ArrayList<Tab>();
        for (Tab tab : tabs) {
            if (ignoreTab != null && tab.equals(ignoreTab)) {
                continue;
            }
            IServiceReference<IRenderService> reference = getPluginContext().getReference(tab.renderer);
            if (reference == null) {
                continue;
            }
            IEditor editor = getPluginContext().getService(reference.getServiceId(), IEditor.class);
            try {
                if (editor != null && editor.isModified()) {
                    changedTabs.add(tab);
                }
            } catch (EditorException e) {
                log.warn("Failed to find out if the editor is modified: " + e.getMessage());
            }
        }

        return changedTabs;
    }

    /**
     * Template method for subclasses.  Called when a tab is selected, either
     * explicitly (user clicks tab) or implicitly (tabbie requests focus).
     *
     * @param index Index of the tab
     */
    protected void onSelectTab(int index) {
    }

    /**
     * Closes all tabs except the ignoredTab tab
     *
     * @param ignoredTab    the tab to exclude from closing, pass null to close all the tabs.
     * @param target AjaxRequestTarget
     */
    void closeAll(Tab ignoredTab, final AjaxRequestTarget target) {
        List<Tab> changedTabs = getChangedTabs(ignoredTab);
        if (changedTabs.size() > 0) {
            IDialogService dialogService = getPluginContext().getService(IDialogService.class.getName(), IDialogService.class);
            dialogService.show(new CloseAllDialog(changedTabs, ignoredTab));

        } else {
            List<TabsPlugin.Tab> tabsCopy = new ArrayList<TabsPlugin.Tab>(tabs);
            for (TabsPlugin.Tab currentTab : tabsCopy) {
                if(ignoredTab != null && ignoredTab.equals(currentTab)){
                    continue;
                }
                onClose(currentTab, target);
            }
        }
    }

    /**
     * Closes the tab if it is unmodified and the editor is not in Edit mode.
     *
     * @param tab    The tab to close
     * @param target AjaxRequestTarget
     */
    void onCloseUnmodified(Tab tab, AjaxRequestTarget target) {
        IServiceReference<IRenderService> reference = getPluginContext().getReference(tab.renderer);
        if (reference == null) {
            log.error("Could not find render service for a tab");
            return;
        }
        final IEditor editor = getPluginContext().getService(reference.getServiceId(), IEditor.class);

        try {
            if (!editor.getMode().equals(IEditor.Mode.EDIT)) {
                editor.close();
            }

        } catch (EditorException e) {
            log.warn("Unable to save the document in the editor", e);
            throw new RuntimeException("Unable to save the document in the editor", e);
        }

    }

    /**
     * Closes the tab if there are no changes. In case the tab contains an editor that has modified document a dialog is
     * shown with option to Discard or Save the document.
     *
     * @param tab    the tab to close
     * @param target AjaxRequestTarget (unused)
     */
    void onClose(Tab tab, AjaxRequestTarget target) {
        IServiceReference<IRenderService> reference = getPluginContext().getReference(tab.renderer);
        if (reference == null) {
            log.error("Could not find render service for a tab");
            return;
        }
        final IEditor editor = getPluginContext().getService(reference.getServiceId(), IEditor.class);
        try {
            if (editor.isModified() || !editor.isValid()) {

                OnCloseDialog onCloseDialog = new OnCloseDialog(new OnCloseDialog.Actions() {
                    public void revert() {
                        try {
                            editor.discard();
                        } catch (EditorException e) {
                            log.warn("Unable to discard the document in the editor", e);
                            throw new RuntimeException("Unable to discard the document in the editor", e);
                        }
                    }

                    public void save() {
                        try {
                            editor.done();
                        } catch (EditorException e) {
                            log.warn("Unable to save the document in the editor", e);
                            throw new RuntimeException("Unable to save the document in the editor", e);
                        }
                    }

                    public void close() {

                        try {
                            editor.close();
                        } catch (EditorException ex) {
                            log.error(ex.getMessage());
                        }
                    }
                }, editor.isValid(), (JcrNodeModel) editor.getModel());

                IDialogService dialogService = getPluginContext().getService(IDialogService.class.getName(),
                        IDialogService.class);
                dialogService.show(onCloseDialog);

            } else {
                if (editor.getMode() == IEditor.Mode.EDIT) {
                    editor.discard();
                }
                editor.close();
            }
        } catch (EditorException e) {
            log.warn("Unable to save the document in the editor", e);
            throw new RuntimeException("Unable to save the document in the editor", e);
        }

    }

    private Tab findTabbie(IRenderService service) {
        for (Tab tab : tabs) {
            if (tab.renderer == service) {
                return tab;
            }
        }
        return null;
    }

    public void hide() {
        previousSelectedTabIndex = tabbedPanel.getSelectedTab();
        tabbedPanel.setSelectedTab(-1);
        tabbedPanel.redraw();
    }

    public void show() {
        if (previousSelectedTabIndex > -1) {
            tabbedPanel.setSelectedTab(previousSelectedTabIndex);
            tabbedPanel.redraw();
            previousSelectedTabIndex = -1;
        }
    }

    protected final TabbedPanel getTabbedPanel() {
        return tabbedPanel;
    }

    protected class Tab implements ITab, IObserver<IObservable> {
        private static final long serialVersionUID = 1L;

        ServiceTracker<ITitleDecorator> decoratorTracker;
        ITitleDecorator decorator;
        IModel<String> titleModel;
        IRenderService renderer;
        int lastSelected;

        Tab(IRenderService renderer) {
            this.renderer = renderer;

            IPluginContext context = getPluginContext();
            String serviceId = context.getReference(renderer).getServiceId();
            decoratorTracker = new ServiceTracker<ITitleDecorator>(ITitleDecorator.class) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onServiceAdded(ITitleDecorator service, String name) {
                    decorator = service;
                    if (titleModel instanceof IObservable) {
                        getPluginContext().unregisterService(Tab.this, IObserver.class.getName());
                    }
                    titleModel = null;
                    getTabbedPanel().redraw();
                }

                @Override
                protected void onRemoveService(ITitleDecorator service, String name) {
                    if (decorator == service) {
                        if (titleModel instanceof IObservable) {
                            getPluginContext().unregisterService(Tab.this, IObserver.class.getName());
                        }
                        titleModel = null;
                        decorator = null;
                        getTabbedPanel().redraw();
                    }
                }
            };
            context.registerTracker(decoratorTracker, serviceId);
        }

        void destroy() {
            IPluginContext context = getPluginContext();
            String serviceId = context.getReference(renderer).getServiceId();
            context.unregisterTracker(decoratorTracker, serviceId);

            if (tabs.size() > 0) {
                // look for previously selected tab
                int lastCount = 0;
                Tab lastTab = tabs.get(0);
                for (Tab tab : getTabbedPanel().getTabs()) {
                    if (tab.lastSelected > lastCount) {
                        lastCount = tab.lastSelected;
                        lastTab = tab;
                    }
                }
                getTabbedPanel().setSelectedTab(tabs.indexOf(lastTab));
                lastTab.lastSelected = ++TabsPlugin.this.selectCount;
                lastTab.renderer.focus(null);
                getTabbedPanel().redraw();
            } else {
                getTabbedPanel().setSelectedTab(-1);
            }
        }

        public IObservable getObservable() {
            return (IObservable) titleModel;
        }


        public IModel<Node> getModel() {
            IServiceReference<IRenderService> reference = getPluginContext().getReference(renderer);
            IEditor editor = getPluginContext().getService(reference.getServiceId(), IEditor.class);
            return editor.getModel();
        }

        public void onEvent(Iterator<? extends IEvent<IObservable>> events) {
            getTabbedPanel().redraw();
        }

        // implement ITab interface

        public IModel<String> getTitle() {
            if (titleModel == null && decorator != null) {
                titleModel = decorator.getTitle();
                if (titleModel instanceof IObservable) {
                    IPluginContext context = getPluginContext();
                    context.registerService(this, IObserver.class.getName());
                }
            }
            return titleModel;
        }

        public String getDecoratorId() {
            if (decorator instanceof Perspective) {
                return ((Perspective)decorator).getMarkupId(true);
            }
            return null;
        }

        public ResourceReference getIcon(IconSize type) {
            if (decorator != null) {
                return decorator.getIcon(type);
            }
            return null;
        }

        public Panel getPanel(String panelId) {
            assert (panelId.equals(TabbedPanel.TAB_PANEL_ID));

            return (Panel) renderer;
        }

        // package internals

        boolean isEditorTab() {
            IServiceReference<IRenderService> reference = getPluginContext().getReference(renderer);
            IEditor editor = getPluginContext().getService(reference.getServiceId(), IEditor.class);
            return (editor != null);
        }

        public Form getForm() {
            IServiceReference<IRenderService> reference = getPluginContext().getReference(renderer);
            IEditor editor = getPluginContext().getService(reference.getServiceId(), IEditor.class);
            return editor.getForm();
        }

        void select() {
            if (tabs.indexOf(this) != getTabbedPanel().getSelectedTab()) {
                getTabbedPanel().setSelectedTab(tabs.indexOf(this));
                lastSelected = ++TabsPlugin.this.selectCount;
                getTabbedPanel().redraw();
            }
        }

        void detach() {
            ((Panel) renderer).detach();
            if (titleModel != null) {
                titleModel.detach();
            }
        }

        public boolean isVisible() {
            return true;
        }

    }

    private class CloseAllDialog extends AbstractDialog {

        public CloseAllDialog(final List<Tab> changedTabs, final Tab ignoredTab) {
            super();
            setOkVisible(false);

            AjaxButton button = new AjaxButton(DialogConstants.BUTTON) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    List<TabsPlugin.Tab> tabsCopy = new ArrayList<TabsPlugin.Tab>(tabs);
                    for (TabsPlugin.Tab currentTab : tabsCopy) {
                        IServiceReference<IRenderService> reference = getPluginContext().getReference(currentTab.renderer);
                        if (reference == null) {
                            log.error("Could not find render service for a tab");
                            return;
                        }
                        IEditor editor = getPluginContext().getService(reference.getServiceId(), IEditor.class);
                        try {

                            if (editor.isModified()) {
                                editor.discard(); ///discard the document and switch to VIEW mode
                            }
                            editor.close();
                        } catch (EditorException e) {
                            log.error("Unable to discard/close the document {}", e.getMessage());
                        }
                    }
                    closeDialog();
                    TabsPlugin.this.closeAll(ignoredTab, target);
                }
            };

            button.setModel(new ResourceModel("discard-all"));

            addButton(button);

            button = new AjaxButton(DialogConstants.BUTTON) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    for (TabsPlugin.Tab currentTab : changedTabs) {
                        IServiceReference<IRenderService> reference = getPluginContext().getReference(currentTab.renderer);
                        if (reference == null) {
                            log.error("Could not find render service for a tab");
                            return;
                        }
                        IEditor editor = getPluginContext().getService(reference.getServiceId(), IEditor.class);
                        try {

                            if (editor.isModified()) {
                                editor.done(); //save the document and switch to VIEW mode
                            }
                            editor.close();
                        } catch (EditorException e) {
                            log.error("Unable to save the document {}", e.getMessage());
                        }
                    }
                    closeDialog();
                    TabsPlugin.this.closeAll(ignoredTab, target);
                }
            };

            button.setModel(new ResourceModel("save-all"));

            addButton(button);



            ModifiedDocumentsProvider provider = new ModifiedDocumentsProvider(getTabModelList(changedTabs));
            add(new ModifiedDocumentsView("modified-docs-view", provider));
        }

        private List<JcrNodeModel> getTabModelList(List<Tab> changedTabs) {
            List<JcrNodeModel> tabModels = new ArrayList<JcrNodeModel>();
            for (Tab tab : changedTabs) {
                IServiceReference<IRenderService> reference = getPluginContext().getReference(tab.renderer);
                if (reference == null) {
                    continue;
                }
                IEditor editor = getPluginContext().getService(reference.getServiceId(), IEditor.class);
                try {
                    if (editor != null && editor.isModified()) {
                        tabModels.add(new JcrNodeModel((Node) editor.getModel().getObject()));
                    }
                } catch (EditorException e) {
                    log.warn("Failed to find out if the editor is modified: " + e.getMessage());
                }
            }
            return tabModels;
        }

        public IModel getTitle() {
            return new StringResourceModel("title", this, null);
        }

        @Override
        public IValueMap getProperties() {
            return DialogConstants.MEDIUM;
        }
    }

    private static class OnCloseDialog extends AbstractDialog {
        private static final long serialVersionUID = 1L;

        public interface Actions extends IClusterable {

            void save();

            void revert();

            void close();
        }

        public OnCloseDialog(final Actions actions, final boolean isValid, JcrNodeModel model) {
            super(model);

            setOkVisible(false);

            if (isValid) {
                add(new Label("label", new ResourceModel("message")));
            } else {
                add(new Label("label", new ResourceModel("invalid")));
            }

            final Label exceptionLabel = new Label("exception", "");
            exceptionLabel.setOutputMarkupId(true);
            add(exceptionLabel);

            AjaxButton button = new AjaxButton(DialogConstants.BUTTON) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    try {
                        actions.revert();
                        actions.close();
                        closeDialog();
                    } catch (Exception ex) {
                        exceptionLabel.setDefaultModel(new Model<String>(ex.getMessage()));
                        target.add(exceptionLabel);
                    }
                }
            };

            if (isValid) {
                button.setModel(new ResourceModel("discard", "Discard"));
            } else {
                button.setModel(new ResourceModel("discard-invalid"));
            }
            addButton(button);

            button = new AjaxButton(DialogConstants.BUTTON) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return isValid;
                }

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    try {
                        actions.save();
                        actions.close();
                        closeDialog();
                    } catch (Exception ex) {
                        exceptionLabel.setDefaultModel(new Model<String>(ex.getMessage()));
                        target.add(exceptionLabel);
                    }
                }
            };
            button.setModel(new ResourceModel("save", "Save"));
            addButton(button);
        }

        public IModel<String> getTitle() {
            return new StringResourceModel("close-document", this, null, "Close {0}",
                        new PropertyModel(getModel(), "name"));
        }

        @Override
        public IValueMap getProperties() {
            return DialogConstants.SMALL;
        }

    }
}
