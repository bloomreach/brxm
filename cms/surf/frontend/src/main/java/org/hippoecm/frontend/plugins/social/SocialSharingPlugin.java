/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.frontend.plugins.social;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.documenturl.IDocumentUrlService;
import org.hippoecm.frontend.service.social.ISocialMediaService;
import org.hippoecm.frontend.service.social.ISocialMedium;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Workflow;
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
                isPublished = isPublished(node);
            } catch (RepositoryException e) {
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

    private static boolean isPublished(Node documentNode) {
        try {
            if (documentNode.hasProperty(HippoStdNodeType.HIPPOSTD_STATE)) {
                final String state = documentNode.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                return HippoStdNodeType.PUBLISHED.equals(state);
            }
        } catch (RepositoryException e) {
            log.error("Error getting " + HippoStdNodeType.HIPPOSTD_STATE + " property from document", e);
        }
        return false;
    }

    private class ShareWorkflow extends StdWorkflow {
        private static final long serialVersionUID = 1L;

        private final ISocialMedium medium;

        public ShareWorkflow(ISocialMedium medium) {
            super("share-on-" + medium.getDisplayName(), "share-on-" + medium.getDisplayName());
            this.medium = medium;
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
