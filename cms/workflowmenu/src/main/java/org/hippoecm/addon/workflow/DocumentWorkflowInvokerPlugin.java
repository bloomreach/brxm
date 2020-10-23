/*
 *  Copyright 2020 Bloomreach
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
package org.hippoecm.addon.workflow;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentWorkflowInvokerPlugin extends AbstractWorkflowManagerPlugin {
    private static final Logger log = LoggerFactory.getLogger(DocumentWorkflowInvokerPlugin.class);

    private static final String JS_FILE = "document-workflow-invoker-plugin.js";

    private final Ajax ajax;

    public DocumentWorkflowInvokerPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(ajax = new Ajax());
    }

    private class Ajax extends AbstractDefaultAjaxBehavior {

        @Override
        protected void respond(final AjaxRequestTarget ajaxRequestTarget) {
            final Request request = RequestCycle.get().getRequest();
            final IRequestParameters requestParameters = request.getRequestParameters();
            final String uuid = requestParameters.getParameterValue("documentId").toString();
            final String category = requestParameters.getParameterValue("category").toString();
            final String action = requestParameters.getParameterValue("action").toString();

            final Node documentNode = getDocumentHandle(uuid);
            final MenuHierarchy menu = buildMenu(Collections.singleton(documentNode), getPluginConfig());

            final MenuHierarchy publicationMenu = menu.getSubmenu(category);
            if (publicationMenu == null) {
                log.warn("Failed to retrieve workflow category {} for document {}", category, uuid);
                return;
            }

            final ActionDescription actionDescription = publicationMenu.getOldestAction(action);
            if (actionDescription == null) {
                log.warn("Failed to retrieve workflow action {}.{} for document {}", category, action, uuid);
                return;
            }

            try {
                actionDescription.invokeAsPromise();
            } catch (final Exception e) {
                log.error("Failed to invoke workflow action {}.{} on document {}", category, action, uuid, e);
            }

            ajaxRequestTarget.add(DocumentWorkflowInvokerPlugin.this);
        }

        @Override
        public void renderHead(final Component component, final IHeaderResponse response) {
            super.renderHead(component, response);
            response.render(OnLoadHeaderItem.forScript(createScript()));
        }
    }

    private Node getDocumentHandle(final String uuid) {
        try {
            final HippoSession jcr = UserSession.get().getJcrSession();
            final Node node = jcr.getNodeByIdentifier(uuid);
            if (node != null) {
                if (node.isNodeType(HippoNodeType.NT_DOCUMENT)
                        && node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                    return node.getParent();
                } else {
                    return node;
                }
            }
        } catch (final RepositoryException ex) {
            log.error("Failed to lookup handle for node with id {}", uuid, ex);
        }

        return null;
    }

    private String createScript() {
        final Map<String, String> variables = Collections.singletonMap("callbackUrl", ajax.getCallbackUrl().toString());
        try (final PackageTextTemplate braJs = new PackageTextTemplate(DocumentWorkflowInvokerPlugin.class, JS_FILE)) {
            return braJs.asString(variables);
        } catch (final IOException e) {
            log.warn("Resource {} could not be closed.", JS_FILE, e);
            return null;
        }
    }
}
