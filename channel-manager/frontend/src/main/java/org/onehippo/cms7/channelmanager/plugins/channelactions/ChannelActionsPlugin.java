/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.plugins.channelactions;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.MenuDescription;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRestProxyService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.hst.rest.DocumentService;
import org.hippoecm.hst.rest.beans.ChannelDocument;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.cms7.channelmanager.restproxy.RestProxyServicesManager;
import org.onehippo.cms7.channelmanager.service.IChannelManagerService;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms7.channelmanager.restproxy.RestProxyServicesManager.getExecutorService;

@SuppressWarnings({ "deprecation", "serial" })
public class ChannelActionsPlugin extends CompatibilityWorkflowPlugin<Workflow> {

    private static final Logger log = LoggerFactory.getLogger(ChannelActionsPlugin.class);
    private static final Comparator<ChannelDocument> DEFAULT_CHANNEL_DOCUMENT_COMPARATOR = new ChannelDocumentNameComparator();

    public static final String CONFIG_CHANNEL_MANAGER_SERVICE_ID = "channel.manager.service.id";

    private final IChannelManagerService channelManagerService;

    public ChannelActionsPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        channelManagerService = loadService("channel manager service", CONFIG_CHANNEL_MANAGER_SERVICE_ID, IChannelManagerService.class);
        if (channelManagerService != null) {
            WorkflowDescriptorModel model = (WorkflowDescriptorModel) getDefaultModel();
            if (model != null) {
                try {
                    Node node = model.getNode();
                    if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        WorkflowManager workflowManager = UserSession.get().getWorkflowManager();
                        DocumentWorkflow workflow = (DocumentWorkflow) workflowManager.getWorkflow(model.getObject());
                        if (Boolean.TRUE.equals(workflow.hints().get("previewAvailable"))) {
                            addMenuDescription(model);
                        }
                    }
                } catch (RepositoryException | RemoteException | WorkflowException e) {
                    log.error("Error getting document node from WorkflowDescriptorModel", e);
                }
            }
        }
        add(new EmptyPanel("content"));
    }

    private void addMenuDescription(final WorkflowDescriptorModel model) {
        add(new MenuDescription() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getLabel() {
                Fragment fragment = new Fragment("label", "description", ChannelActionsPlugin.this);
                fragment.add(new Label("label", new StringResourceModel("label", ChannelActionsPlugin.this, null)));
                return fragment;
            }

            @Override
            public MarkupContainer getContent() {
                Fragment fragment = new Fragment("content", "actions", ChannelActionsPlugin.this);
                try {
                    Node node = model.getNode();
                    String handleUuid = node.getIdentifier();
                    fragment.add(createMenu(handleUuid));
                } catch (RepositoryException e) {
                    log.warn("Unable to create channel menu", e);
                    fragment.addOrReplace(new EmptyPanel("channels"));
                }
                ChannelActionsPlugin.this.addOrReplace(fragment);
                return fragment;
            }
        });
    }

    private <T extends IClusterable> T loadService(final String name, final String configServiceId, final Class<T> clazz) {
        final String serviceId = getPluginConfig().getString(configServiceId, clazz.getName());
        log.debug("Using {} with id '{}'", name, serviceId);

        final T service = getPluginContext().getService(serviceId, clazz);
        if (service == null) {
            log.info("Could not get service '{}' of type {}", serviceId, clazz.getName());
        }

        return service;
    }

    private MarkupContainer createMenu(final String documentUuid) {

        final Map<String, IRestProxyService> liveRestProxyServices = RestProxyServicesManager.getLiveRestProxyServices(getPluginContext(), getPluginConfig());
        if (liveRestProxyServices.isEmpty()) {
            log.info("No rest proxies services available. Cannot create menu for available channels");
            return new EmptyPanel("channels");
        }

        // a rest proxy can only return ChannelDocument for the webapp the proxy belongs to. Hence we need to
        // invoke all rest proxies to get all available channel documents
        List<Callable<List<ChannelDocument>>> restProxyJobs = new ArrayList<>();

        for (final Map.Entry<String, IRestProxyService> entry : liveRestProxyServices.entrySet()) {
            final DocumentService documentService = entry.getValue().createSecureRestProxy(DocumentService.class);
            restProxyJobs.add(new Callable<List<ChannelDocument>>() {
                @Override
                public List<ChannelDocument> call() throws Exception {
                    return documentService.getChannels(documentUuid);
                }
            });
        }

        final List<ChannelDocument> combinedChannelDocuments = new ArrayList<>();
        try {
            final List<Future<List<ChannelDocument>>> futures = getExecutorService().invokeAll(restProxyJobs);
            for (Future<List<ChannelDocument>> future : futures) {
                combinedChannelDocuments.addAll(future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            if (log.isDebugEnabled()) {
                log.warn("Exception while trying to find Channel for document with uuid '{}'.", documentUuid, e);
            } else {
                log.warn("Exception while trying to find Channel for document with uuid '{}' : {}", documentUuid, e.toString());
            }
        }
        Collections.sort(combinedChannelDocuments, getChannelDocumentComparator());

        final Map<String, ChannelDocument> idToChannelMap = new LinkedHashMap<String, ChannelDocument>();
        for (final ChannelDocument channelDocument : combinedChannelDocuments) {
            idToChannelMap.put(channelDocument.getChannelId(), channelDocument);
        }

        return new ListView<String>("channels", new LoadableDetachableModel<List<String>>() {

            @Override
            protected List<String> load() {
                List<String> names = new ArrayList<String>();
                names.addAll(idToChannelMap.keySet());
                return names;
            }

        }) {

            {
                onPopulate();
            }

            @Override
            protected void populateItem(final ListItem<String> item) {
                final String channelId = item.getModelObject();
                ChannelDocument channel = idToChannelMap.get(channelId);
                item.add(new ViewChannelAction("view-channel", channel));
            }
        };

    }

    protected Comparator<ChannelDocument> getChannelDocumentComparator() {
        return DEFAULT_CHANNEL_DOCUMENT_COMPARATOR;
    }

    private class ViewChannelAction extends StdWorkflow {

        private static final long serialVersionUID = 1L;
        private ChannelDocument channelDocument;

        ViewChannelAction(String id, ChannelDocument channelDocument) {
            super(id, "channelactions");
            this.channelDocument = channelDocument;
        }

        @Override
        protected IModel<String> getTitle() {
            return new LoadableDetachableModel<String>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected String load() {
                    return channelDocument.getChannelName();
                }
            };
        }

        @Override
        protected ResourceReference getIcon() {
            return new PackageResourceReference(getClass(), "channel-icon-16.png");
        }

        @Override
        protected void invoke() {
            if (channelManagerService != null) {
                // create the pathInfo of the channel manager url. The pathInfo includes the mountPath & path after the mount
                StringBuilder pathInfo  = new StringBuilder(channelDocument.getMountPath()).append(channelDocument.getPathInfo());
                channelManagerService.viewChannel(channelDocument.getChannelId(), pathInfo.toString(),
                        channelDocument.getContextPath(),
                        channelDocument.getCmsPreviewPrefix() );
            } else {
                log.info("Cannot view channel, no channel manager service available");
            }
        }
    }

    protected static class ChannelDocumentNameComparator implements Comparator<ChannelDocument>, Serializable {

        @Override
        public int compare(final ChannelDocument o1, final ChannelDocument o2) {
            return o1.getChannelName().compareTo(o2.getChannelName());
        }

    }

}
