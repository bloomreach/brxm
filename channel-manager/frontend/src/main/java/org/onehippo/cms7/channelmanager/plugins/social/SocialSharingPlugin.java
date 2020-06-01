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
package org.onehippo.cms7.channelmanager.plugins.social;

import java.io.Serializable;
import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.cms7.channelmanager.service.IDocumentUrlService;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders a menu to share a document url on social media, wich a separate menu item for each supported social
 * medium. The document URL is constructed via the {@link IDocumentUrlService}} specified by the property
 * "document.url.service.id". The document URL can be shared on all media supported by the {@link ISocialMediaService}
 * specified by the property "social.media.service.id".
 */
public class SocialSharingPlugin extends CompatibilityWorkflowPlugin<Workflow> {

    private static final String CONFIG_DOCUMENT_URL_SERVICE_ID = "document.url.service.id";
    private static final String CONFIG_SOCIAL_MEDIA_SERVICE_ID = "social.media.service.id";

    private static final Logger log = LoggerFactory.getLogger(SocialSharingPlugin.class);

    private IDocumentUrlService documentUrlService;
    private final ISocialMediaService socialMediaService;
    private final IModel<String> documentUrlModel;
    private boolean isPublished;

    public SocialSharingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        
        final String documentUrlServiceId = config.getString(CONFIG_DOCUMENT_URL_SERVICE_ID, IDocumentUrlService.DEFAULT_SERVICE_ID);
        documentUrlService = context.getService(documentUrlServiceId, IDocumentUrlService.class);
        if (documentUrlService == null) {
            throw new IllegalStateException("Unknown document url service '" + documentUrlServiceId
                    + "' configured for the website actions plugin. Please set the configuration property '"
                    + CONFIG_DOCUMENT_URL_SERVICE_ID + "'");
        }

        final String socialMediaServiceId = config.getString(CONFIG_SOCIAL_MEDIA_SERVICE_ID, ISocialMediaService.DEFAULT_SERVICE_ID);
        socialMediaService = context.getService(socialMediaServiceId, ISocialMediaService.class);
        if (socialMediaService == null) {
            throw new IllegalStateException("Unknown social media service '" + socialMediaServiceId
                    + "' configured for the website actions plugin. Please set the configuration property '"
                    + CONFIG_SOCIAL_MEDIA_SERVICE_ID + "'");
        }

        for (ISocialMedium medium : socialMediaService.getAllSocialMedia()) {
            add(new ShareWorkflow(medium));
        }


        WorkflowDescriptorModel model = (WorkflowDescriptorModel) getDefaultModel();
        if (model != null) {
            try {
                Node node = model.getNode();
                if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    WorkflowManager workflowManager = UserSession.get().getWorkflowManager();
                    DocumentWorkflow workflow = (DocumentWorkflow) workflowManager.getWorkflow(model.getObject());
                    Serializable isLive = workflow.hints().get("isLive");
                    isPublished = (isLive instanceof Boolean) && ((Boolean) isLive);
                }
            } catch (RepositoryException | RemoteException | WorkflowException e) {
                log.error("Error getting document node from WorkflowDescriptorModel", e);
            }
        }

        documentUrlModel = new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                if (isPublished) {
                    WorkflowDescriptorModel model = (WorkflowDescriptorModel) getDefaultModel();
                    if (model != null) {
                        try {
                            Node node = model.getNode();
                            return documentUrlService.getUrl(node);
                        } catch (RepositoryException e) {
                            log.error("Error getting document node from WorkflowDescriptorModel", e);
                        }
                    }
                }
                return null;
            }
        };
    }

    @Override
    protected void onDetach() {
        documentUrlModel.detach();
        super.onDetach();
    }

    private class ShareWorkflow extends StdWorkflow {
        private static final long serialVersionUID = 1L;

        private final ISocialMedium medium;

        public ShareWorkflow(ISocialMedium medium) {
            super("share-on-" + medium.getDisplayName(), "share-on-" + medium.getDisplayName());
            this.medium = medium;
        }

        @Override
        public String getSubMenu() {
            return "socialsharing";
        }

        @Override
        public boolean isVisible() {
            return isPublished;
        }

        @Override
        protected IModel<String> getTitle() {
            return new LoadableDetachableModel<String>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected String load() {
                    return medium.getDisplayName() + "...";
                }
            };
        }

        @Override
        protected ResourceReference getIcon() {
            return medium.getIcon16();
        }

        @Override
        protected void invoke() {
            medium.shareUrl(documentUrlModel.getObject());
        }

    }

}
