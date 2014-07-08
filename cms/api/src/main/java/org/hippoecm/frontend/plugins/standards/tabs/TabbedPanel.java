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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.list.LoopItem;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.AbstractRepeater;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.behaviors.IContextMenu;
import org.hippoecm.frontend.behaviors.IContextMenuManager;
import org.hippoecm.frontend.plugins.yui.layout.IWireframe;
import org.hippoecm.frontend.plugins.yui.rightclick.RightClickBehavior;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.render.ICardView;

public class TabbedPanel extends WebMarkupContainer {

    private static final long serialVersionUID = 1L;

    abstract static class CloseLink<T> extends AbstractLink {

        private static final long serialVersionUID = 1L;

        public CloseLink(final String id, final IModel<T> model, Form form) {
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
                add(new AjaxEventBehavior("click") {

                    @Override
                    protected void onEvent(final AjaxRequestTarget target) {
                        collapseMenu(target);
                        onClick(target);
                    }
                });
            }
        }

        private void collapseMenu(final AjaxRequestTarget target) {
            IContextMenu parent = findParent(IContextMenu.class);
            if (parent != null) {
                parent.collapse(target);
            } else {
                IContextMenuManager manager = findParent(IContextMenuManager.class);
                if (manager != null) {
                    manager.collapseAllContextMenus();
                }
            }
        }

        protected abstract void onClick(AjaxRequestTarget target);

    }

    public static final String TAB_PANEL_ID = "panel";

    private final TabsPlugin plugin;

    private int maxTabLength = 12;
    private final List<TabsPlugin.Tab> tabs;
    private MarkupContainer panelContainer;
    private MarkupContainer tabsContainer;
    private IconSize iconType = IconSize.TINY;
    private transient boolean redraw = false;
    private CardView cardView;

    public TabbedPanel(String id, TabsPlugin plugin, List<TabsPlugin.Tab> tabs, MarkupContainer tabsContainer) {
        super(id, new Model<Integer>(-1));

        if (tabs == null) {
            throw new IllegalArgumentException("argument [tabs] cannot be null");
        }

        this.plugin = plugin;
        this.tabs = tabs;
        this.tabsContainer = tabsContainer;

        setOutputMarkupId(true);

        final IModel<Integer> tabCount = new AbstractReadOnlyModel<Integer>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Integer getObject() {
                return TabbedPanel.this.tabs.size();
            }
        };

        // add the loop used to generate tab names
        tabsContainer.add(new Loop("tabs", tabCount) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(LoopItem item) {
                final int index = item.getIndex();

                final WebMarkupContainer titleMarkupContainer = getTitleMarkupContainer(index);
                item.add(titleMarkupContainer);
                item.add(newBehavior(index));
                TabsPlugin.Tab tab = getTabs().get(index);
                if (tab.isEditorTab()) {
                    final WebMarkupContainer menu = createContextMenu("contextMenu", index);

                    item.add(menu);
                    item.add(new RightClickBehavior(menu, item) {

                        @Override
                        protected void respond(AjaxRequestTarget target) {
                            getContextmenu().setVisible(true);
                            target.add(getComponentToUpdate());
                            IContextMenuManager menuManager = findParent(IContextMenuManager.class);
                            if (menuManager != null) {
                                menuManager.showContextMenu(this);
                                StringValue x = RequestCycle.get().getRequest().getQueryParameters().getParameterValue(MOUSE_X_PARAM);
                                StringValue y = RequestCycle.get().getRequest().getQueryParameters().getParameterValue(MOUSE_Y_PARAM);
                                target.appendJavaScript(
                                        "Hippo.ContextMenu.renderAtPosition('" + menu.getMarkupId() + "', " + x + ", " + y + ");");
                            }
                        }
                    });
                }
                item.setOutputMarkupId(true);
            }

            @Override
            protected LoopItem newItem(int iteration) {
                return newTabContainer(iteration);
            }

        });

        panelContainer = newPanelContainer("panel-container");
        cardView = new CardView(tabs);
        panelContainer.add(cardView);
        add(panelContainer);
    }

    protected WebMarkupContainer newPanelContainer(String id) {
        WebMarkupContainer container = new WebMarkupContainer(id);
        container.setOutputMarkupId(true);
        return container;
    }

    private WebMarkupContainer createContextMenu(String contextMenu, final int index) {
        final TabsPlugin.Tab tab = getTabs().get(index);
        WebMarkupContainer menuContainer = new WebMarkupContainer(contextMenu);
        menuContainer.setOutputMarkupId(true);
        menuContainer.setVisible(false);

        menuContainer.add(new CloseLink<TabsPlugin.Tab>("editor-close", Model.of(tab), tab.getForm()) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                plugin.onClose(tab, target);
            }

        });

        menuContainer.add(new CloseLink<Void>("editor-close-others", null, getPanelContainerForm()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //Create a copy so we won't run into ConcurrentModificationException
                plugin.closeAll(tab, target);
            }
        });

        menuContainer.add(new CloseLink<Void>("editor-close-all", null, getPanelContainerForm()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                plugin.closeAll(null, target);
            }
        });

        menuContainer.add(new CloseLink<Void>("editor-close-unmodified", null, getPanelContainerForm()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //Create a copy so we won't run into ConcurrentModificationException
                List<TabsPlugin.Tab> tabsCopy = new ArrayList<TabsPlugin.Tab>(tabs);
                for (TabsPlugin.Tab currentTab : tabsCopy) {
                    plugin.onCloseUnmodified(currentTab, target);
                }
            }
        });

        return menuContainer;
    }


    protected LoopItem newTabContainer(final int tabIndex) {
        return new LoopItem(tabIndex) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                String cssClass = (String) tag.getAttribute("class");
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
        WebMarkupContainer container = new WebMarkupContainer("container", new Model<Integer>(index));
        final TabsPlugin.Tab tab = getTabs().get(index);
        final IModel<TabsPlugin.Tab> tabModel = new Model<TabsPlugin.Tab>(tab);
        if (tab.isEditorTab()) {
            container.add(new CloseLink<TabsPlugin.Tab>("close", tabModel, tab.getForm()) {

                @Override
                protected void onClick(AjaxRequestTarget target) {
                    plugin.onClose(tab, target);
                }

            });
        } else {
            container.add(new Label("close").setVisible(false));
        }
        WebMarkupContainer link = new AjaxLink<TabsPlugin.Tab>("link", tabModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                plugin.onSelect(getModelObject(), target);
            }
        };

        ResourceReference iconResource = tab.getIcon(iconType);
        Component image;
        if (iconResource == null) {
            image = new EmptyPanel("icon");
            image.setVisible(false);
        } else {
            image = new Image("icon", iconResource);
        }
        IModel<String> sizeModel = new Model<String>(Integer.valueOf(iconType.getSize()).toString());
        image.add(new AttributeModifier("width", true, sizeModel));
        image.add(new AttributeModifier("height", true, sizeModel));
        link.add(image);

        link.add(new Label("title", new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                IModel<String> titleModel = tabModel.getObject().getTitle();
                if (titleModel != null) {
                    String title = titleModel.getObject();
                    if (title.length() > maxTabLength) {
                        // leave space for two .. then add them
                        title = title.substring(0, maxTabLength - 2) + "..";
                    }
                    return title;
                }
                return "title";
            }
        }));
        link.add(new AttributeAppender("title", new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                IModel<String> titleModel = tabModel.getObject().getTitle();
                if (titleModel != null) {
                    return titleModel.getObject();
                }
                return "";
            }

        }, ""));
        container.add(link);

        return container;
    }

    protected Behavior newBehavior(final int tabIndex) {
        return new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                plugin.onSelect(tabs.get(tabIndex), target);
            }

        };
    }

    protected Form getPanelContainerForm() {
        return null;
    }

    public void setMaxTitleLength(int maxTitleLength) {
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

    public void render(PluginRequestTarget target) {
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
        }
    }

    public final List<TabsPlugin.Tab> getTabs() {
        return tabs;
    }

    public void setSelectedTab(int index) {
        if (index >= tabs.size()) {
//            panelContainer.replace(plugin.getEmptyPanel());
            return;
        }

        setDefaultModelObject(index);

        if (index < 0) {
            cardView.select(null);
            return;
        }

        ITab tab = tabs.get(index);

        WebMarkupContainer panel = tab.getPanel(TAB_PANEL_ID);

        if (panel == null) {
            throw new WicketRuntimeException(
                    "ITab.getPanel() returned null. TabbedPanel [" + getPath() + "] ITab index [" + index + "]");

        }

        if (!panel.getId().equals(TAB_PANEL_ID)) {
            throw new WicketRuntimeException(
                    "ITab.getPanel() returned a panel with invalid id [" + panel.getId() + "]. You must always return a panel with id equal to the provided panelId parameter. TabbedPanel [" + getPath() + "] ITab index [" + index + "]");
        }

        cardView.select((TabsPlugin.Tab) tab);
//        panelContainer.replace(panel);
    }

    public final int getSelectedTab() {
        return (Integer) getDefaultModelObject();
    }

    public void setIconType(IconSize iconType) {
        this.iconType = iconType;
    }

    public IconSize getIconType() {
        return iconType;
    }

    private static class CardView extends AbstractRepeater implements ICardView {

        private final List<TabsPlugin.Tab> tabs;
        private Set<TabsPlugin.Tab> added = new HashSet<TabsPlugin.Tab>();
        private Set<TabsPlugin.Tab> removed = new HashSet<TabsPlugin.Tab>();
        private TabsPlugin.Tab selected;
        private int counter = 0;
        private boolean populated = false;

        public CardView(final List<TabsPlugin.Tab> tabs) {
            super("cards");
            this.tabs = tabs;
            setRenderBodyOnly(true);
        }

        @Override
        public boolean isActive(Component component) {
            Component container = component;
            if (selected != null) {
                MarkupContainer selectedPanel = selected.getPanel(TAB_PANEL_ID);
                while (container != null) {
                    if (container == selectedPanel) {
                        return true;
                    }
                    container = container.getParent();
                }
            }
            return false;
        }

        void select(TabsPlugin.Tab tabbie) {
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
            TabsPlugin.Tab tabbie = tabs.get(tabs.size() - 1);
            if (removed.contains(tabbie)) {
                removed.remove(tabbie);
            } else {
                added.add(tabbie);
            }
        }

        void addFirst() {
            TabsPlugin.Tab tabbie = tabs.get(0);
            if (removed.contains(tabbie)) {
                removed.remove(tabbie);
            } else {
                added.add(tabbie);
            }
        }

        protected ListItem<TabsPlugin.Tab> newItem(TabsPlugin.Tab tabbie) {
            return new ListItem<TabsPlugin.Tab>(counter++, new Model<TabsPlugin.Tab>(tabbie)) {
                {
                    add(new AttributeAppender("style", new LoadableDetachableModel<Object>() {
                        @Override
                        protected Object load() {
                            if (getModelObject() == selected) {
                                return "display: block;";
                            } else {
                                return "display: none;";
                            }
                        }
                    }, " "));

                    add(getModelObject().getPanel(TAB_PANEL_ID));
                    setOutputMarkupId(true);
                }

            };
        }

        @Override
        protected void onPopulate() {
            if (!populated) {
                populated = true;
                for (TabsPlugin.Tab tabbie : tabs) {
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

                    Iterator<? extends Component> iterator = iterator();
                    while (iterator.hasNext()) {
                        Component component = iterator.next();
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

        public void updateCards(AjaxRequestTarget target) {
            for (TabsPlugin.Tab tabbie : added) {
                ListItem<TabsPlugin.Tab> item = newItem(tabbie);
                add(item);

                target.prependJavaScript(
                        "var element = document.createElement('div');" +
                                "element.setAttribute('id', '" + item.getMarkupId() + "');" +
                                "Wicket.$('" + getParent().getMarkupId() + "').appendChild(element);");
                target.add(item);
            }
            Iterator<Component> children = iterator();
            while (children.hasNext()) {
                Component item = children.next();
                if (removed.contains(item.getDefaultModelObject())) {
                    target.appendJavaScript(
                            "var element = Wicket.$('" + item.getMarkupId() + "');" +
                                    "HippoAjax.cleanupElement(element);" +
                                    "element.parentNode.removeChild(element);");
                    children.remove();
                }
            }

            children = iterator();
            while (children.hasNext()) {
                Component item = children.next();
                String display = "none";
                if (item.getDefaultModelObject() == selected) {
                    display = "block";
                }

                target.appendJavaScript(
                        "var element = Wicket.$('" + item.getMarkupId() + "');" +
                                "element.setAttribute('style', 'display: " + display + ";');");

                if (item.getDefaultModelObject() == selected) {
                    renderWireframes((MarkupContainer) item, target);
                }
            }

            added.clear();
            removed.clear();
        }

        void renderWireframes(MarkupContainer cont, final AjaxRequestTarget target) {
            //Visit child components in order to find components that contain a {@link WireframeBehavior}.
            cont.visitChildren(Component.class, new IVisitor<Component, Void>() {
                public void component(Component component, IVisit<Void> visit) {
                    for (Object behavior : component.getBehaviors()) {
                        if (behavior instanceof IWireframe) {
                            IWireframe wireframe = (IWireframe) behavior;
                            wireframe.resize(target);
                            visit.dontGoDeeper();
                            return;
                        }
                    }
                }
            });

        }

    }
}
