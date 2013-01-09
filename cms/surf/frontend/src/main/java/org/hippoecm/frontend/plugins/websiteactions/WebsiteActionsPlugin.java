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
package org.hippoecm.frontend.plugins.websiteactions;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.popup.IPopupService;
import org.hippoecm.frontend.service.documenturl.IDocumentUrlService;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated From CMS 7.7, this plugin has been replaced by the ChannelActionsPlugin in the channel manager.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class WebsiteActionsPlugin extends CompatibilityWorkflowPlugin<Workflow> {

    private static final String CONFIG_POPUP_SERVICE_ID = "popup.service.id";
    private static final String CONFIG_PREVIEW_DOCUMENT_URL_SERVICE_ID = "preview.document.url.service.id";
    private static final String CONFIG_LIVE_DOCUMENT_URL_SERVICE_ID = "live.document.url.service.id";

    private static final Logger log = LoggerFactory.getLogger(WebsiteActionsPlugin.class);

    private final IPopupService popupService;
    private final IDocumentUrlService previewDocumentUrlService;
    private IDocumentUrlService liveDocumentUrlService;
    private String documentUrl;
    private boolean documentLive;

    public WebsiteActionsPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        popupService = context.getService(IPopupService.class.getName(), IPopupService.class);

        final String previewSiteDocumentUrlServiceId = config.getString(CONFIG_PREVIEW_DOCUMENT_URL_SERVICE_ID, IDocumentUrlService.DEFAULT_SERVICE_ID);
        previewDocumentUrlService = context.getService(previewSiteDocumentUrlServiceId, IDocumentUrlService.class);
        if (previewDocumentUrlService == null) {
            throw new IllegalStateException("No preview document url service configured for the website actions plugin. "
                    + "Please set the configuration property " + CONFIG_PREVIEW_DOCUMENT_URL_SERVICE_ID);
        }

        final String liveSiteDocumentUrlServiceId = config.getString(CONFIG_LIVE_DOCUMENT_URL_SERVICE_ID, IDocumentUrlService.DEFAULT_SERVICE_ID);
        liveDocumentUrlService = context.getService(liveSiteDocumentUrlServiceId, IDocumentUrlService.class);
        if (liveDocumentUrlService == null) {
            log.info("No live document url service configured, reusing the preview document url service");
            liveDocumentUrlService = previewDocumentUrlService;
        }

        documentUrl = null;
        documentLive = false;

        add(new StdWorkflow("websiteactions", "websiteactions") {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> getTitle() {
                return new LoadableDetachableModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String load() {
                        return documentLive ? WebsiteActionsPlugin.this.getString("live-link") : WebsiteActionsPlugin.this.getString("preview-link");
                    }
                };
            }

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), (documentLive ? "live" : "preview") + "-icon-16.png");
            }

            @Override
            protected void invoke() {
                popupService.openPopupWindow(new PopupSettings(IPopupService.DEFAULT_POPUP_SETTINGS), documentUrl);
            }
            
        });

        WorkflowDescriptorModel model = (WorkflowDescriptorModel) getDefaultModel();
        if (model != null) {
            try {
                Node node = model.getNode();
                documentLive = isPublished(node);
                documentUrl = documentLive ? liveDocumentUrlService.getUrl(node) : previewDocumentUrlService.getUrl(node);
            } catch (RepositoryException e) {
                log.error("Error getting document node from WorkflowDescriptorModel", e);
            }
        }
    }

    private static boolean isPublished(Node documentNode) {
        try {
            if(documentNode.hasProperty(HippoStdNodeType.HIPPOSTD_STATE)) {
                final String state = documentNode.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                return HippoStdNodeType.PUBLISHED.equals(state);
            }
        } catch (RepositoryException e) {
            log.error("Error getting " + HippoStdNodeType.HIPPOSTD_STATE + " property from document", e);
        }
        return false;
    }

}
