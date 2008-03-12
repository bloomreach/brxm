/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.tabs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.DialogPageCreator;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.ExceptionModel;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The TabsPlugin is an editor-aware container of plugins.  The tabs correspond
 * to child plugins (perspectives).  Tab switching is implemented by handling the
 * "edit" and "focus" requests.  The edit operation is sent as a notification
 * to the perspectives.  If one of those wishes to obtain focus, it can request it
 * by sending a "focus" request.
 *
 */
public class TabsPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TabsPlugin.class);

    private Map<JcrNodeModel, Tab> editors;
    private TabbedPanel tabbedPanel;
    private int selectCount;
    private int editCount;

    private String editPerspective;
    private PluginDescriptor editDescriptor;
    private DialogWindow dialogWindow;
    private OnCloseDialog onCloseDialog;
    private Channel channel;

    public TabsPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        editors = new HashMap<JcrNodeModel, Tab>();
        List<Tab> tabs = new ArrayList<Tab>();
        tabbedPanel = new TabbedPanel("tabs", tabs, this);

        add(tabbedPanel);

        // a place holder for the onclose dialog
        channel = getTopChannel();
        add(new Label("onclose", ""));
        List<String> parameter = pluginDescriptor.getParameter("editor");
        if (parameter != null && parameter.size() > 0) {
            editPerspective = parameter.get(0);
        } else {
            editPerspective = null;
        }

        selectCount = 0;
        editCount = 0;
    }

    // invoked by the TabbedPanel when a tab is selected    
    protected void onSelect(AjaxRequestTarget target, Tab tabbie) {
        tabbie.select();
        if (target != null) {
            target.addComponent(tabbedPanel);
        }
    }

    protected void onClose(AjaxRequestTarget target, Tab tabbie) {
        JcrNodeModel closedJcrNodeModel = null;
        ArrayList<JcrNodeModel> jcrNewNodeModelList = new ArrayList<JcrNodeModel>();
        if (editors.containsValue(tabbie)) {
            for (Map.Entry<JcrNodeModel, Tab> entry : editors.entrySet()) {
                if (entry.getValue().equals(tabbie)) {
                    closedJcrNodeModel = entry.getKey();
                } else if (((JcrNodeModel) entry.getKey()).getNode().isNew()) {
                    // keep track of new added nodes, because if child of the closed tab
                    // they need to be closed as well if the parent is *not* saved
                    jcrNewNodeModelList.add(entry.getKey());
                }
            }
        }
        if (closedJcrNodeModel != null) {
            try {
                if (closedJcrNodeModel.getNode().getSession().hasPendingChanges()) {
                    // if there are any changes, inform whether the user wants to save or discard changes
                    dialogWindow = new DialogWindow("onclose", new JcrNodeModel(closedJcrNodeModel), null, null);
                    onCloseDialog = new OnCloseDialog(dialogWindow, channel, this, tabbie, closedJcrNodeModel,
                            jcrNewNodeModelList, editors);
                    dialogWindow.setPageCreator(new DialogPageCreator(onCloseDialog));
                    this.replace(dialogWindow);
                    dialogWindow.show(target);
                } else {
                    // save to close without informing and without saving
                    tabbie.destroy();
                }
            } catch (RepositoryException e) {
                if (target != null) {
                    Request request = channel.createRequest("exception", new ExceptionModel(e));
                    channel.send(request);
                    request.getContext().apply(target);
                }
                log.info(e.getClass().getName() + ": " + e.getMessage());
            }
        }
        if (target != null) {
            target.addComponent(this);
        }
    }

    @Override
    public Plugin addChild(final PluginDescriptor childDescriptor) {
        if (editPerspective == null || !editPerspective.equals(childDescriptor.getPluginId())) {
            Tab tab = new Tab(childDescriptor, getPluginModel(), false);
            return tab.getPlugin();
        }
        editDescriptor = childDescriptor;
        return null;
    }

    @Override
    public void removeChild(PluginDescriptor childDescriptor) {
        Tab tabbie = getPluginTab(childDescriptor.getPluginId());
        tabbie.destroy();
    }

    @Override
    public void handle(Request request) {
        if ("edit".equals(request.getOperation())) {
            JcrNodeModel model = new JcrNodeModel(request.getModel());
            if (!editors.containsKey(model) && editDescriptor != null) {
                // create a descriptor for the plugin
                PluginDescriptor descriptor = editDescriptor.clone();
                try {
                    // set the title to the name of the node
                    List<String> titleParam = new LinkedList<String>();
                    titleParam.add(model.getNode().getName());
                    descriptor.addParameter("title", titleParam);
                } catch (RepositoryException ex) {
                    log.error("Couldn't obtain name of item " + ex.getMessage());
                }

                // add the plugin
                Tab tabbie = new Tab(descriptor, model, true);
                editors.put(model, tabbie);
                request.getContext().addRefresh(this);

                // HACK: add children before setting the final pluginId.
                // Each perspective needs to have a unique Id to be able to fulfill
                // focus requests.  The plugin configuration uses the pluginId to
                // find any children.
                tabbie.getPlugin().addChildren();
                descriptor.setPluginId((++editCount) + ":" + editPerspective);
            }

            // notify children; if tabs should be switched,
            // they should send a focus request.
            Channel channel = getBottomChannel();
            if (channel != null) {
                Notification notification = channel.createNotification(request);
                channel.publish(notification);
            }

            // don't send request to parent
            return;
        } else if ("focus".equals(request.getOperation())) {
            String pluginId = (String) request.getModel().getMapRepresentation().get("plugin");
            Tab tab = getPluginTab(pluginId);
            if (tab != null) {
                tab.select();
                request.getContext().addRefresh(this);

                // notify children of focus event
                Channel channel = getBottomChannel();
                if (channel != null) {
                    Notification notification = channel.createNotification(request);
                    channel.publish(notification);
                }
                return;
            }
        }
        // TODO: handle close tab request
        super.handle(request);
    }

    Tab getPluginTab(String pluginId) {
        Iterator<Tab> tabIter = tabbedPanel.getTabs().iterator();
        while (tabIter.hasNext()) {
            Tab tabbie = tabIter.next();
            if (tabbie.getPlugin().getDescriptor().getPluginId() == pluginId) {
                return tabbie;
            }
        }
        return null;
    }

    // tab implementation that manages a plugin and keeps track of the previously
    // selected tab.  (that one is selected when the current tab is destroyed)
    // Things will go wrong after 2<sup>31</sup>-1 tabs have been selected.
    class Tab implements ITab {
        private static final long serialVersionUID = 1L;

        Plugin plugin;
        int lastSelected;
        boolean close;

        Tab(PluginDescriptor descriptor, IPluginModel model, boolean close) {
            this.close = close;

            descriptor.setWicketId(TabbedPanel.TAB_PANEL_ID);
            PluginFactory pluginFactory = new PluginFactory(getPluginManager());
            plugin = pluginFactory.createPlugin(descriptor, model, TabsPlugin.this);

            // add to the list of tabs in the tabbed panel
            tabbedPanel.getTabs().add(this);
        }

        // implement ITab interface

        public Model getTitle() {
            PluginDescriptor descriptor = plugin.getDescriptor();
            String title = descriptor.getPluginId();
            if (descriptor.getParameter("title") != null) {
                title = descriptor.getParameter("title").get(0);
            }
            return new Model(title);
        }

        public Panel getPanel(String panelId) {
            assert (panelId.equals(TabbedPanel.TAB_PANEL_ID));

            return plugin;
        }

        // package internals

        Plugin getPlugin() {
            return plugin;
        }

        boolean canClose() {
            return close;
        }

        void select() {
            tabbedPanel.setSelectedTab(tabbedPanel.getTabs().indexOf(this));
            lastSelected = ++TabsPlugin.this.selectCount;
        }

        void destroy() {
            if (editors.containsValue(this)) {
                for (Map.Entry<JcrNodeModel, Tab> entry : editors.entrySet()) {
                    if (entry.getValue().equals(this)) {
                        editors.remove(entry.getKey());
                        break;
                    }
                }
            }
            tabbedPanel.getTabs().remove(this);

            // let plugin clean up any resources
            plugin.destroy();

            // look for previously selected tab
            int lastCount = 0;
            Tab lastTab = null;
            Iterator<Tab> tabs = tabbedPanel.getTabs().iterator();
            while (tabs.hasNext()) {
                Tab tabbie = tabs.next();
                if (tabbie.lastSelected > lastCount) {
                    lastCount = tabbie.lastSelected;
                    lastTab = tabbie;
                }
            }
            if (lastTab != null) {
                lastTab.select();
            }

        }

        public void popup(AjaxRequestTarget target, RepositoryException e) {
            if (target != null) {
                Request request = channel.createRequest("exception", new ExceptionModel(e));
                channel.send(request);
                request.getContext().apply(target);
            }
            log.info(e.getClass().getName() + ": " + e.getMessage());
        }
    }

}
