/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.function.Consumer;

import javax.jcr.Node;

import org.apache.wicket.Component;
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
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.Dialog;
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
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.skin.Icon;
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
 * sizes. By default, 'm' will be used.
 * </ul>
 */
public class TabsPlugin extends RenderPlugin {

    static final Logger log = LoggerFactory.getLogger(TabsPlugin.class);

    public static final String TAB_ID = "tabs";
    public static final String MAX_TAB_TITLE_LENGTH = "title.maxlength";
    public static final String TAB_ICON_SIZE = "icon.size";

    private final TabbedPanel tabbedPanel;
    private RenderService emptyPanel;
    private final List<Tab> tabs;
    private int selectionCount;
    private boolean openleft = false;

    private boolean isHidden = false;
    private boolean avoidTabRefocus = false;

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

        tabs = new ArrayList<>();
        add(tabbedPanel = newTabbedPanel("tabs", tabs, tabsContainer));
        tabbedPanel.setMaxTitleLength(properties.getInt(MAX_TAB_TITLE_LENGTH, 12));
        tabbedPanel.setIconType(IconSize.getIconSize(properties.getString(TAB_ICON_SIZE, IconSize.M.name())));

        if (properties.containsKey("tabs.container.id")) {
            JavaPluginConfig containerConfig = new JavaPluginConfig(properties.getName() + "-tabs-container");
            containerConfig.put("wicket.id", properties.getString("tabs.container.id"));
            RenderService containerService = new TabsContainerService(context, containerConfig);
            containerService.add(tabsContainer);
        } else {
            tabbedPanel.add(tabsContainer);
        }

        selectionCount = 0;
        ServiceTracker<IRenderService> tabsTracker = new ServiceTracker<IRenderService>(IRenderService.class) {

            @Override
            public void onServiceAdded(IRenderService service, String name) {
                // add the plugin
                service.bind(TabsPlugin.this, TabbedPanel.TAB_PANEL_ID);
                if (service != emptyPanel) {
                    final int selectedTabIndex = tabbedPanel.getSelectedTab();
                    if (selectedTabIndex >= 0) {
                        final Tab selectedTab = tabs.get(selectedTabIndex);
                        onTabDeactivated(selectedTab);
                    }

                    final Tab tab = new Tab(service);
                    if (openleft) {
                        tabs.add(0, tab);
                        tabbedPanel.setSelectedTab(0);
                        tabbedPanel.addFirst();
                    } else {
                        tabs.add(tab);
                        if (tabs.size() == 1) {
                            tabbedPanel.setSelectedTab(0);
                        }
                        tabbedPanel.addLast();
                    }

                    onTabActivated(tab);
                }
            }

            @Override
            public void onRemoveService(IRenderService service, String name) {
                final Tab tab = findTab(service);
                if (tab != null) {
                    onTabDeactivated(tab);

                    tabs.remove(tab);
                    tab.destroy();

                    if (tabs.isEmpty()) {
                        tabbedPanel.setSelectedTab(-1);
                    }
                    service.unbind();
                    tabbedPanel.removed(tab);
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
        for (Tab tab : tabs) {
            tab.renderer.render(target);
        }
    }

    @Override
    public void focus(IRenderService child) {
        Tab tab = findTab(child);
        if (tab != null) {
            tab.select();
            onSelectTab(tabs.indexOf(tab));
        }
        super.focus(child);
    }

    @Override
    public void onDetach() {
        tabs.forEach(TabsPlugin.Tab::detach);
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
        return !this.tabs.isEmpty();
    }

    void onSelect(Tab tab, AjaxRequestTarget target) {
        tab.renderer.focus(null);
        onSelectTab(tabs.indexOf(tab));
        fireTabSelectionEvent(tab, target);
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
        List<Tab> changedTabs = new ArrayList<>();
        for (Tab tab : tabs) {
            if (ignoreTab != null && tab.equals(ignoreTab)) {
                continue;
            }
            final IEditor editor = tab.getEditor();
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
     * explicitly (user clicks tab) or implicitly (tab requests focus).
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
    void closeAll(final Tab ignoredTab, final AjaxRequestTarget target) {
        final List<Tab> changedTabs = getChangedTabs(ignoredTab);
        if (!changedTabs.isEmpty()) {
            IDialogService dialogService = getPluginContext().getService(IDialogService.class.getName(), IDialogService.class);
            dialogService.show(new CloseAllDialog(changedTabs, ignoredTab));
        } else {
            List<TabsPlugin.Tab> tabsCopy = new ArrayList<>(tabs);
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
        final IEditor editor = tab.getEditor();
        try {
            if (editor != null && !editor.getMode().equals(IEditor.Mode.EDIT)) {
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
        final IEditor editor = tab.getEditor();
        if (editor == null) {
            return;
        }
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
                }, editor.isValid(), (JcrNodeModel) editor.getModel(), editor.getMode());

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

    private Tab findTab(IRenderService service) {
        for (Tab tab : tabs) {
            if (tab.renderer == service) {
                return tab;
            }
        }
        return null;
    }

    public void hide() {
        isHidden = true;
        blurTabs();
    }

    public void blurTabs() {
        final int tabIndex = tabbedPanel.getSelectedTab();
        if (tabIndex > -1) {
            tabbedPanel.setSelectedTab(-1);
            tabbedPanel.redraw();

            onTabDeactivated(tabs.get(tabIndex));
        }
    }

    public void disableTabRefocus() {
        this.avoidTabRefocus = true;
    }

    /**
     * @deprecated use {@link #focusRecentTab} or {@link #focusRecentTabUnlessHidden()} instead.
     */
    @Deprecated
    public void show() {
        focusRecentTab();
    }

    public void focusRecentTabUnlessHidden() {
        if (!isHidden) {
            focusRecentTab();
        }
    }

    public void focusRecentTab() {
        Tab tab = findMostRecentlySelectedTab();
        if (tab != null) {
            final int tabIndex = tabs.indexOf(tab);
            tabbedPanel.setSelectedTab(tabIndex);
            tabbedPanel.redraw();

            onTabActivated(tab);

            tab.renderer.focus(null);
        }
    }

    private Tab findMostRecentlySelectedTab() {
        int highestSelectionStamp = -1;
        Tab selectedTab = null;
        for (Tab tab : getTabbedPanel().getTabs()) {
            if (tab.selectionStamp > highestSelectionStamp) {
                highestSelectionStamp = tab.selectionStamp;
                selectedTab = tab;
            }
        }
        return selectedTab;
    }

    protected final TabbedPanel getTabbedPanel() {
        return tabbedPanel;
    }

    protected void onTabActivated(final Tab tab) {
        // hook method for sub-classes to execute logic when a tab is activated
        tab.selectionStamp = ++selectionCount;
        isHidden = false;
        avoidTabRefocus = false;
    }

    protected void onTabDeactivated(final Tab tab) {
        // hook method for sub-classes to execute logic when a tab is deactivated
    }

    protected class Tab implements ITab, IObserver<IObservable> {

        ServiceTracker<ITitleDecorator> decoratorTracker;
        ITitleDecorator decorator;
        IModel<String> titleModel;
        IRenderService renderer;
        int selectionStamp;

        Tab(IRenderService renderer) {
            this.renderer = renderer;

            IPluginContext context = getPluginContext();
            String serviceId = context.getReference(renderer).getServiceId();
            decoratorTracker = new ServiceTracker<ITitleDecorator>(ITitleDecorator.class) {

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

            getTabbedPanel().setSelectedTab(-1);
            if (!avoidTabRefocus) {
                focusRecentTabUnlessHidden();
            }
        }

        public IObservable getObservable() {
            return (IObservable) titleModel;
        }

        @SuppressWarnings("unchecked")
        public IModel<Node> getModel() {
            final IEditor editor = getEditor();
            return editor != null ? editor.getModel() : null;
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

        public String getTitleCssClass() {
            if (decorator instanceof Perspective) {
                return ((Perspective)decorator).getTitleCssClass();
            }
            return null;
        }

        @Deprecated
        // Use public Component getIcon(String id, IconSize size) instead
        public ResourceReference getIcon(IconSize size) {
            log.warn("This method is deprecated in favor of public Component getIcon(String id, IconSize size)");
            return decorator != null ? decorator.getIcon(size) : null;
        }

        public Component getIcon(String id, IconSize size) {
            if (decorator == null) {
                return null;
            }
            Component icon = decorator.getIcon(id, size);
            if (icon != null) {
                return icon;
            }
            ResourceReference reference = decorator.getIcon(size);
            return reference != null ? HippoIcon.fromResource(id, reference, size) : null;
        }

        public Panel getPanel(String panelId) {
            assert (panelId.equals(TabbedPanel.TAB_PANEL_ID));

            return (Panel) renderer;
        }

        public void discard() throws EditorException {
            final IEditor editor = getEditor();
            if (editor != null) {
                editor.discard();
            }
        }

        // package internals

        IEditor getEditor() {
            final IPluginContext context = getPluginContext();
            final IServiceReference<IRenderService> reference = context.getReference(renderer);

            if (reference == null) {
                log.error("Could not find render service for a tab");
                return null;
            }

            return context.getService(reference.getServiceId(), IEditor.class);
        }

        boolean isEditorTab() {
            return getEditor() != null;
        }

        public Form getForm() {
            final IEditor editor = getEditor();
            return editor != null ? editor.getForm() : null;
        }

        void select() {
            final TabbedPanel panel = getTabbedPanel();
            final int selectedTabIndex = panel.getSelectedTab();
            final int myIndex = tabs.indexOf(this);

            if (myIndex != selectedTabIndex) {
                if (selectedTabIndex >= 0) {
                    final Tab selectedTab = tabs.get(selectedTabIndex);
                    onTabDeactivated(selectedTab);
                }

                panel.setSelectedTab(myIndex);
                onTabActivated(this);

                panel.redraw();
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

    private class CloseAllDialog extends Dialog {

        public static final String MODIFIED_DOCS_VIEW_ID = "modified-docs-view";

        private final ModifiedDocumentsProvider provider;
        private final ModifiedDocumentsView modifiedDocumentsView;
        private final Tab ignoredTab;

        public CloseAllDialog(final List<Tab> changedTabs, final Tab ignoredTab) {
            super();
            this.ignoredTab = ignoredTab;

            setOkVisible(false);
            setSize(DialogConstants.LARGE_AUTO);
            setTitleKey("title");

            addButton(new AjaxButton(DialogConstants.BUTTON, new ResourceModel("discard-all")) {
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    final Consumer<Tab> discardAndClose = currentTab -> {
                        final IEditor editor = currentTab.getEditor();
                        if (editor != null) {
                            try {
                                if (editor.isModified() || editor.getMode() == IEditor.Mode.EDIT) {
                                    editor.discard(); ///discard the document and switch to VIEW mode
                                }
                                editor.close();
                            } catch (EditorException e) {
                                log.error("Unable to discard/close the document {}", e.getMessage());
                            }
                        }
                    };
                    processAllTabs(discardAndClose);
                    updateDialog(target);
                }
            });

            addButton(new AjaxButton(DialogConstants.BUTTON, new ResourceModel("save-all")) {
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    final Consumer<Tab> saveAndClose = currentTab -> {
                        final IEditor editor = currentTab.getEditor();
                        if (editor != null){
                            try {
                                if (editor.isModified()) {
                                    editor.done(); //save the document and switch to VIEW mode
                                } else if (editor.getMode() == IEditor.Mode.EDIT) {
                                    editor.discard();
                                }
                                editor.close();
                            } catch (EditorException e) {
                                log.error("Unable to save the document {}", e.getMessage());
                            }
                        }
                    };
                    processAllTabs(saveAndClose);
                    updateDialog(target);
                }
            });

            provider = new ModifiedDocumentsProvider(getNodeModelList(changedTabs));
            modifiedDocumentsView = new ModifiedDocumentsView(MODIFIED_DOCS_VIEW_ID, provider);
            add(modifiedDocumentsView);
        }

        private void processAllTabs(final Consumer<Tab> tabAction) {
            // need to clone the list because the tabAction may close a tab, then it is removed from the tabs at
            // ServiceTracker#onRemoveService(). This may cause ConcurrentModificationException
            final List<Tab> listTabsClone = new ArrayList<>(TabsPlugin.this.tabs);
            listTabsClone.stream()
                .filter(t -> t != null && !t.equals(ignoredTab))
                .forEach(tabAction);
        }

        private void updateDialog(final AjaxRequestTarget target) {
            // If there's any changed tab left, update the close-all dialog, otherwise close it.
            final List<Tab> updatedChangedTabs = getChangedTabs(ignoredTab);
            if (updatedChangedTabs.isEmpty()) {
                closeDialog();
            } else {
                final List<JcrNodeModel> nodeModelList = getNodeModelList(updatedChangedTabs);
                provider.setModifiedDocuments(nodeModelList);
                target.add(modifiedDocumentsView);
            }
        }

        private List<JcrNodeModel> getNodeModelList(List<Tab> changedTabs) {
            final List<JcrNodeModel> tabModels = new ArrayList<>();
            for (Tab tab : changedTabs) {
                final IEditor editor = tab.getEditor();
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
    }

    private static class OnCloseDialog extends Dialog<Node> {
        public interface Actions extends IClusterable {

            void save();

            void revert();

            void close();
        }

        public OnCloseDialog(final Actions actions, final boolean isValid, JcrNodeModel model, final IEditor.Mode mode) {
            super(model);

            setOkVisible(false);
            setSize(DialogConstants.SMALL_AUTO);

            add(new Label("label", new ResourceModel(isValid ? "message" : "invalid")));
            add(HippoIcon.fromSprite("icon", Icon.EXCLAMATION_TRIANGLE, IconSize.L));

            final Label exceptionLabel = new Label("exception", "");
            exceptionLabel.setOutputMarkupId(true);
            add(exceptionLabel);

            ResourceModel discardButtonLabel = isValid ? new ResourceModel("discard", "Discard") : new ResourceModel("discard-invalid");
            addButton(new AjaxButton(DialogConstants.BUTTON, discardButtonLabel) {
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    try {
                        actions.revert();
                        actions.close();
                        closeDialog();
                    } catch (Exception ex) {
                        exceptionLabel.setDefaultModel(Model.of(ex.getMessage()));
                        target.add(exceptionLabel);
                    }
                }
            });


            final AjaxButton saveButton = new AjaxButton(DialogConstants.BUTTON, new ResourceModel("save", "Save")) {
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
                        exceptionLabel.setDefaultModel(Model.of(ex.getMessage()));
                        target.add(exceptionLabel);
                    }
                }
            };
            saveButton.setEnabled(mode == IEditor.Mode.EDIT);
            addButton(saveButton);
        }

        @Override
        public IModel<String> getTitle() {
            return new StringResourceModel("close-document", this, null, "Close {0}",
                        new PropertyModel(getModel(), "displayName"));
        }
    }
}
