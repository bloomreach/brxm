/**
 * Copyright 2010 Hippo
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

package org.onehippo.cms7.channelmanager.channels;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.IPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;
import org.onehippo.cms7.channelmanager.templatecomposer.TemplateComposerPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple listing panel that uses {@link ListDataTable} to render the list of channels.
 */
public class ChannelsListPanel extends BreadCrumbPanel {

    private static final Logger log = LoggerFactory.getLogger(ChannelsListPanel.class);
    private static final String HOST_GROUP_CONFIG_PROP = "hst.virtualhostgroup.path";

    private IPluginContext context;
    private IPluginConfig config;


    public ChannelsListPanel(IPluginContext context, IPluginConfig config, String id,
                             IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);
        this.context = context;
        this.config = config;

        final String hstConfigLocation = config.getString(HOST_GROUP_CONFIG_PROP);
        if (hstConfigLocation == null) {
            log.error("No host group is configured for retrieving list of channels, please set the property " +
                    HOST_GROUP_CONFIG_PROP + " on the channel-manager configuration!");
            add(new Label("channels-data-table", "No host group is configured for retrieving list of channels, " +
                    "please set the property " + HOST_GROUP_CONFIG_PROP + " on the channel-manager configuration!"));
        } else {
            JcrNodeModel hstConfigNodeModel = new JcrNodeModel(hstConfigLocation);

            ChannelDataProvider channelDataProvider = new ChannelDataProvider(hstConfigNodeModel);
            ListDataTable listDataTable = new ListDataTable<Channel>("channels-data-table",
                    new TableDefinition<Channel>(getTableColumns()),
                    channelDataProvider,
                    new ListDataTable.TableSelectionListener<Channel>() {
                        @Override
                        public void selectionChanged(IModel<Channel> channelIModel) {
                            activate(new IBreadCrumbPanelFactory() {
                                @Override
                                public BreadCrumbPanel create(String componentId, IBreadCrumbModel breadCrumbModel) {
                                    return new TemplateComposerPanel(componentId,
                                                                     breadCrumbModel,
                                                                     ChannelsListPanel.this.config);
                                }
                            });

                        }
                    },
                    false,
                    new IPagingDefinition() {
                        @Override
                        public int getPageSize() {
                            return 50;
                        }

                        @Override
                        public int getViewSize() {
                            return 50;
                        }
                    }
            );

            add(listDataTable);
            listDataTable.setOutputMarkupId(true);
        }
    }

    @Override
    public String getTitle() {
        return "Channels";
    }


    @SuppressWarnings({"unchecked"})
    private List<ListColumn<Channel>> getTableColumns() {
        List<ListColumn<Channel>> columns = new ArrayList<ListColumn<Channel>>();
        ListColumn<Channel> nameColumn = new ListColumn<Channel>(new ResourceModel("title", "Channel Name"), "title");

        nameColumn.setRenderer(new IListCellRenderer<Channel>() {
            @Override
            public Component getRenderer(String id, IModel<Channel> channelModel) {
                return new Label(id, channelModel.getObject().getTitle());
            }

            @Override
            public IObservable getObservable(IModel<Channel> channelIModel) {
                Channel channel = channelIModel.getObject();
                IModel<String> displayName = new Model<String>(channel.getTitle());
                if (displayName instanceof IObservable) {
                    return (IObservable) displayName;
                }
                return null;
            }
        });

        columns.add(nameColumn);

        ListColumn<Channel> contentRootColumn = new ListColumn<Channel>(new ResourceModel("contentRoot", "Content Root"), "contentRoot");

        contentRootColumn.setRenderer(new IListCellRenderer<Channel>() {
            @Override
            public Component getRenderer(String id, IModel<Channel> channelIModel) {
                return new ContentRootRenderer(id, channelIModel);
            }

            @Override
            public IObservable getObservable(IModel<Channel> channelIModel) {
                Channel channel = channelIModel.getObject();
                IModel<String> displayName = new Model<String>(channel.getContentRoot());
                if (displayName instanceof IObservable) {
                    return (IObservable) displayName;
                }
                return null;
            }
        });

        columns.add(contentRootColumn);
        return columns;
    }


    private class ContentRootRenderer extends Panel {

        public ContentRootRenderer(String id, final IModel<Channel> model) {
            super(id, model);
            AjaxLink link = new AjaxLink("link") {

                @Override
                public void onClick(AjaxRequestTarget target) {
                    //TODO Browse to the content root of the channel.
                }
            };

            link.add(new Label("link-text", model.getObject().getContentRoot()));
            add(link);
        }
    }
}
