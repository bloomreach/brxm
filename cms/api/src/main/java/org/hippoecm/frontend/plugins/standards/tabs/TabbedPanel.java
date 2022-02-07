/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.list.LoopItem;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.AbstractRepeater;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.visit.IVisitor;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.ajax.NoDoubleClickBehavior;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.attributes.StyleAttribute;
import org.hippoecm.frontend.attributes.TitleAttribute;
import org.hippoecm.frontend.behaviors.IContextMenu;
import org.hippoecm.frontend.behaviors.IContextMenuManager;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.yui.layout.IWireframe;
import org.hippoecm.frontend.plugins.yui.rightclick.RightClickBehavior;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.render.ICardView;
import org.hippoecm.frontend.skin.Icon;

public class TabbedPanel extends WebMarkupContainer {

    abstract static class CloseLink<T> extends AbstractLink {

        public CloseLink(final String id, final IModel<T> model, final Form form) {
            super(id, model);

            if (form != null) {
                add(new AjaxFormSubmitBehavior(form, "click") {

                    @Override
                    protected void onSubmit(final AjaxRequestTarget target) {
                        collapseMenu(target);
                        onClick(target);
                    }

                    @Override
                    protected void onError(final AjaxRequestTarget target) {
                        collapseMenu(target);
                        onClick(target);
                    }
                });
            } else {
                add(new NoDoubleClickBehavior() {
                    @Override
                    protected void onEvent(final AjaxRequestTarget target) {
                        collapseMenu(target);
                        onClick(target);
                    }
                });
            }
        }

        private void collapseMenu(final AjaxRequestTarget target) {
            final IContextMenu parent = findParent(IContextMenu.class);
            if (parent != null) {
                parent.collapse(target);
            } else {
                final IContextMenuManager manager = findParent(IContextMenuManager.class);
                if (manager != null) {
                    manager.collapseAllContextMenus();
                }
            }
        }

        protected abstract void onClick(AjaxRequestTarget target);

    }

    public static final String TAB_PANEL_ID = "panel";

    private final TabsPlugin plugin;
    private final List<TabsPlugin.Tab> tabs;
    private final MarkupContainer panelContainer;
    private final MarkupContainer tabsContainer;
    private final CardView cardView;

    private IconSize iconType = IconSize.M;
    private int maxTabLength = 12;

    private transient boolean redraw = false;

    public TabbedPanel(final String id, final TabsPlugin plugin, final List<TabsPlugin.Tab> tabs, final MarkupContainer tabsContainer) {
        super(id, new Model<>(-1));

        if (tabs == null) {
            throw new IllegalArgumentException("argument [tabs] cannot be null");
        }

        this.plugin = plugin;
        this.tabs = tabs;
        this.tabsContainer = tabsContainer;

        setOutputMarkupId(true);

        final IModel<Integer> tabCount = ReadOnlyModel.of(TabbedPanel.this.tabs::size);

        // add the loop used to generate tab names
        tabsContainer.add(new Loop("tabs", tabCount) {

            @Override
            protected void populateItem(final LoopItem item) {
                final int index = item.getIndex();

                final WebMarkupContainer titleMarkupContainer = getTitleMarkupContainer(index);
                item.add(titleMarkupContainer);
                item.add(newBehavior(index));
                final TabsPlugin.Tab tab = getTabs().get(index);
                if (tab.isEditorTab()) {
                    final WebMarkupContainer menu = createContextMenu("contextMenu", index);

                    item.add(menu);
                    item.add(new RightClickBehavior(menu, item) {

                        @Override
                        protected void respond(final AjaxRequestTarget target) {
                            getContextmenu().setVisible(true);
                            target.add(getComponentToUpdate());
                            final IContextMenuManager menuManager = findParent(IContextMenuManager.class);
                            if (menuManager != null) {
                                menuManager.showContextMenu(this);

                                final Request request = RequestCycle.get().getRequest();
                                final IRequestParameters queryParameters = request.getQueryParameters();
                                final StringValue x = queryParameters.getParameterValue(MOUSE_X_PARAM);
                                final StringValue y = queryParameters.getParameterValue(MOUSE_Y_PARAM);
                                final String renderContextMenu = String.format(
                                        "Hippo.ContextMenu.renderAtPosition('%s', %d, %d);",
                                        menu.getMarkupId(), x.toInt(), y.toInt());

                                target.appendJavaScript(renderContextMenu);
                            }
                        }
                    });
                }
                item.setOutputMarkupId(true);
            }

            @Override
            protected LoopItem newItem(final int iteration) {
                return newTabContainer(iteration);
            }

        });

        panelContainer = newPanelContainer("panel-container");
        cardView = new CardView(tabs);
        panelContainer.add(cardView);
        add(panelContainer);
    }

    protected WebMarkupContainer newPanelContainer(final String id) {
        final WebMarkupContainer container = new WebMarkupContainer(id);
        container.setOutputMarkupId(true);
        return container;
    }

    private WebMarkupContainer createContextMenu(final String contextMenu, final int index) {
        final TabsPlugin.Tab tab = getTabs().get(index);
        final WebMarkupContainer menuContainer = new WebMarkupContainer(contextMenu);
        menuContainer.setOutputMarkupId(true);
        menuContainer.setVisible(false);

        menuContainer.add(new CloseLink<TabsPlugin.Tab>("editor-close", Model.of(tab), tab.getForm()) {

            @Override
            protected void onClick(final AjaxRequestTarget target) {
                plugin.onClose(tab, target);
            }

        });

        menuContainer.add(new CloseLink<Void>("editor-close-others", null, getPanelContainerForm()) {

            @Override
            public void onClick(final AjaxRequestTarget target) {
                //Create a copy so we won't run into ConcurrentModificationException
                plugin.closeAll(tab, target);
            }
        });

        menuContainer.add(new CloseLink<Void>("editor-close-all", null, getPanelContainerForm()) {

            @Override
            public void onClick(final AjaxRequestTarget target) {
                plugin.closeAll(null, target);
            }
        });

        menuContainer.add(new CloseLink<Void>("editor-close-unmodified", null, getPanelContainerForm()) {

            @Override
            public void onClick(final AjaxRequestTarget target) {
                //Create a copy so we won't run into ConcurrentModificationException
                final List<TabsPlugin.Tab> tabsCopy = new ArrayList<>(tabs);
                for (final TabsPlugin.Tab currentTab : tabsCopy) {
                    plugin.onCloseUnmodified(currentTab, target);
                }
            }
        });

        return menuContainer;
    }


    protected LoopItem newTabContainer(final int tabIndex) {
        return new LoopItem(tabIndex) {
            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                String cssClass = tag.getAttribute("class");
                if (cssClass == null) {
                    cssClass = " ";
                }
                cssClass += " tab" + getIndex();

                if (getIndex() == getSelectedTab()) {
                    cssClass += " selected";
                }
                if (getIndex() == getTabs().size() - 1) {
                    cssClass += " last";
                }
                tag.put("class", cssClass.trim());
            }
        };
    }

    // used by superclass to add title to the container
    protected WebMarkupContainer getTitleMarkupContainer(final int index) {
        final WebMarkupContainer container = new WebMarkupContainer("container", new Model<>(index));
        final TabsPlugin.Tab tab = getTabs().get(index);
        final IModel<TabsPlugin.Tab> tabModel = new Model<>(tab);

        if (tab.isEditorTab()) {
            final CloseLink closeLink = new CloseLink<TabsPlugin.Tab>("close", tabModel, tab.getForm()) {

                @Override
                protected void onClick(final AjaxRequestTarget target) {
                    plugin.onClose(tab, target);
                }

            };
            container.add(closeLink);

            final HippoIcon closeIcon = HippoIcon.fromSprite("close-icon", Icon.TIMES);
            closeLink.add(closeIcon);
        } else {
            final EmptyPanel hidden = new EmptyPanel("close");
            hidden.setVisible(false);
            hidden.add(HippoIcon.fromSprite("close-icon", Icon.EMPTY));
            container.add(hidden);
        }

        final WebMarkupContainer link = new AjaxLink<TabsPlugin.Tab>("link", tabModel) {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                plugin.onSelect(getModelObject(), target);
            }
        };

        Component icon = tab.getIcon("icon", iconType);
        if (icon == null) {
            icon = new EmptyPanel("icon").setRenderBodyOnly(true);
        }
        link.add(icon);

        link.add(new Label("title", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                final IModel<String> titleModel = tabModel.getObject().getTitle();
                if (titleModel != null) {
                    return titleModel.getObject();
                }
                return "title";
            }
        }));

        link.add(TitleAttribute.append(() -> {
            final IModel<String> titleModel = tabModel.getObject().getTitle();
            return titleModel != null
                    ? titleModel.getObject()
                    : StringUtils.EMPTY;
        }));

        final String titleCssClass = tabModel.getObject().getTitleCssClass();
        if (titleCssClass != null) {
            link.add(ClassAttribute.append(titleCssClass));
        }
        container.add(link);

        return container;
    }

    protected Behavior newBehavior(final int tabIndex) {
        return new NoDoubleClickBehavior() {
            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                plugin.onSelect(tabs.get(tabIndex), target);
            }
        };
    }

    protected Form getPanelContainerForm() {
        return null;
    }

    public void setMaxTitleLength(final int maxTitleLength) {
        this.maxTabLength = maxTitleLength;
    }

    public void redraw() {
        redraw = true;
    }

    void removed(final TabsPlugin.Tab tabbie) {
        cardView.removed(tabbie);
        redraw();
    }

    void addLast() {
        cardView.addLast();
        redraw();
    }

    void addFirst() {
        cardView.addFirst();
        redraw();
    }

    public void render(final PluginRequestTarget target) {
        cardView.onPopulate();
        if (redraw) {
            if (target != null) {
                target.add(tabsContainer);
                cardView.updateCards(target);
            }
            redraw = false;
        }
    }

    // @see org.apache.wicket.Component#onAttach()
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        if (!hasBeenRendered() && getSelectedTab() == -1 && !tabs.isEmpty()) {
            // select the first tab by default
            setSelectedTab(0);

            final TabsPlugin.Tab firstTab = tabs.get(0);
            plugin.onTabActivated(firstTab);
        }
    }

    public final List<TabsPlugin.Tab> getTabs() {
        return tabs;
    }

    public void setSelectedTab(final int index) {
        if (index >= tabs.size()) {
//            panelContainer.replace(plugin.getEmptyPanel());
            return;
        }

        setDefaultModelObject(index);

        if (index < 0) {
            cardView.select(null);
            return;
        }

        final TabsPlugin.Tab tab = tabs.get(index);

        final WebMarkupContainer panel = tab.getPanel(TAB_PANEL_ID);
        if (panel == null) {
            throw new WicketRuntimeException(
                    "ITab.getPanel() returned null. TabbedPanel [" + getPath() + "] ITab index [" + index + "]");
        }

        if (!panel.getId().equals(TAB_PANEL_ID)) {
            throw new WicketRuntimeException(
                    "ITab.getPanel() returned a panel with invalid id [" + panel.getId() + "]. You must always return a panel with id equal to the provided panelId parameter. TabbedPanel [" + getPath() + "] ITab index [" + index + "]");
        }

        cardView.select(tab);
//        panelContainer.replace(panel);
    }

    public final int getSelectedTab() {
        return (Integer) getDefaultModelObject();
    }

    public void setIconType(final IconSize iconType) {
        this.iconType = iconType;
    }

    public IconSize getIconType() {
        return iconType;
    }

    private static class CardView extends AbstractRepeater implements ICardView {

        private final List<TabsPlugin.Tab> tabs;
        private final Set<TabsPlugin.Tab> added = new HashSet<>();
        private final Set<TabsPlugin.Tab> removed = new HashSet<>();

        private TabsPlugin.Tab selected;
        private int counter = 0;
        private boolean populated = false;

        CardView(final List<TabsPlugin.Tab> tabs) {
            super("cards");
            this.tabs = tabs;
            setRenderBodyOnly(true);
        }

        @Override
        public boolean isActive(final Component component) {
            Component container = component;
            if (selected != null) {
                final MarkupContainer selectedPanel = selected.getPanel(TAB_PANEL_ID);
                while (container != null) {
                    if (container == selectedPanel) {
                        return true;
                    }
                    container = container.getParent();
                }
            }
            return false;
        }

        void select(final TabsPlugin.Tab tabbie) {
            this.selected = tabbie;
        }

        void removed(final TabsPlugin.Tab tabbie) {
            if (added.contains(tabbie)) {
                added.remove(tabbie);
            } else {
                removed.add(tabbie);
            }
        }

        void addLast() {
            final TabsPlugin.Tab tabbie = tabs.get(tabs.size() - 1);
            if (removed.contains(tabbie)) {
                removed.remove(tabbie);
            } else {
                added.add(tabbie);
            }
        }

        void addFirst() {
            final TabsPlugin.Tab tabbie = tabs.get(0);
            if (removed.contains(tabbie)) {
                removed.remove(tabbie);
            } else {
                added.add(tabbie);
            }
        }

        protected ListItem<TabsPlugin.Tab> newItem(final TabsPlugin.Tab tabbie) {
            return new ListItem<TabsPlugin.Tab>(counter++, new Model<>(tabbie)) {
                {
                    add(StyleAttribute.append(() -> getModelObject() == selected
                            ? "display: block"
                            : "display: none"));

                    add(getModelObject().getPanel(TAB_PANEL_ID));
                    setOutputMarkupId(true);
                }
            };
        }

        @Override
        protected void onPopulate() {
            if (!populated) {
                populated = true;
                for (final TabsPlugin.Tab tabbie : tabs) {
                    add(newItem(tabbie));
                }
                added.clear();
                removed.clear();
            }
        }

        @Override
        protected Iterator<? extends Component> renderIterator() {
            final Iterator<TabsPlugin.Tab> upstream = tabs.iterator();
            return new Iterator<Component>() {

                @Override
                public boolean hasNext() {
                    return upstream.hasNext();
                }

                @Override
                public Component next() {
                    final TabsPlugin.Tab tabbie = upstream.next();

                    final Iterator<? extends Component> iterator = iterator();
                    while (iterator.hasNext()) {
                        final Component component = iterator.next();
                        if (component.getDefaultModelObject() == tabbie) {
                            return component;
                        }
                    }

                    throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public void updateCards(final AjaxRequestTarget target) {
            for (final TabsPlugin.Tab tabbie : added) {
                final ListItem<TabsPlugin.Tab> item = newItem(tabbie);
                add(item);

                final String addScript = String.format(
                        "var element = document.createElement('div');" +
                                "element.setAttribute('id', '%s');" +
                                "Wicket.$('%s').appendChild(element);",
                        item.getMarkupId(), getParent().getMarkupId());

                target.prependJavaScript(addScript);
                target.add(item);
            }

            Iterator<Component> children = iterator();
            while (children.hasNext()) {
                final Component item = children.next();
                if (removed.contains(item.getDefaultModelObject())) {

                    final String cleanupAndRemoveScript = String.format(
                            "var element = Wicket.$('%s');" +
                                    "HippoAjax.cleanupElement(element);" +
                                    "element.parentNode.removeChild(element);",
                            item.getMarkupId());

                    target.appendJavaScript(cleanupAndRemoveScript);
                    children.remove();
                }
            }

            children = iterator();
            while (children.hasNext()) {
                final Component item = children.next();
                final String display = item.getDefaultModelObject() == selected
                        ? "block"
                        : "none";

                final String javascript = String.format(
                        "var element = Wicket.$('%s');" +
                                "element.setAttribute('style', 'display: %s;');",
                        item.getMarkupId(), display);

                target.appendJavaScript(javascript);

                if (item.getDefaultModelObject() == selected) {
                    renderWireframes((MarkupContainer) item, target);
                }
            }

            added.clear();
            removed.clear();
        }

        void renderWireframes(final MarkupContainer cont, final AjaxRequestTarget target) {
            // Visit child components in order to find components that contain a {@link WireframeBehavior}.
            cont.visitChildren(Component.class, (IVisitor<Component, Void>) (component, visit) -> {
                for (final Object behavior : component.getBehaviors()) {
                    if (behavior instanceof IWireframe) {
                        final IWireframe wireframe = (IWireframe) behavior;
                        wireframe.resize(target);
                        visit.dontGoDeeper();
                        return;
                    }
                }
            });
        }

    }
}
