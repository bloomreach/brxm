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
package org.hippoecm.frontend.plugins.documentworkflow;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import org.hippoecm.addon.workflow.AbstractWorkflowManagerPlugin;
import org.hippoecm.addon.workflow.ActionDescription;
import org.hippoecm.addon.workflow.MenuHierarchy;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class DocumentWorkflowInvokerPlugin extends AbstractWorkflowManagerPlugin {
    private static final Logger log = LoggerFactory.getLogger(DocumentWorkflowInvokerPlugin.class);

    private static final String JS_FILE = "document-workflow-invoker-plugin.js";
    private static final List<String> REQUEST_ACTIONS = Arrays.asList( "cancel", "rejected");
    private static final Map<String, String> ACTIONS_TO_HINTS = ImmutableMap.<String, String>builder()
            .put("PUB", "publish")
            .put("SCHED_PUB", "publish")
            .put("REQ_PUB", "requestPublication")
            .put("REQ_SCHED_PUB", "requestPublication")
            .put("DEPUB", "depublish")
            .put("SCHED_DEPUB", "depublish")
            .put("REQ_DEPUB", "requestDepublication")
            .put("REQ_SCHED_DEPUB", "requestDepublication")
            .put("copy", "copy")
            .put("move", "move")
            .put("delete", "delete")
            .put("accept", "acceptRequest")
            .put("reject", "rejectRequest")
            .put("cancel", "cancelRequest")
            .put("rejected", "cancelRequest")
            .build();

    private final Ajax ajax;

    public DocumentWorkflowInvokerPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(ajax = new Ajax());
    }

    private class Ajax extends AbstractDefaultAjaxBehavior {

        @Override
        protected void respond(final AjaxRequestTarget target) {
            final Request request = RequestCycle.get().getRequest();
            final IRequestParameters requestParameters = request.getRequestParameters();
            final String uuid = requestParameters.getParameterValue("documentId").toString();
            final String category = requestParameters.getParameterValue("category").toString();
            final String action = requestParameters.getParameterValue("action").toString();

            if (isValidWorkflowRequest(action, uuid, target)) {
                executeWorkflowRequest(uuid, category, action, target);
            }
        }

        @Override
        public void renderHead(final Component component, final IHeaderResponse response) {
            super.renderHead(component, response);
            response.render(OnLoadHeaderItem.forScript(createScript()));
        }
    }

    private boolean isValidWorkflowRequest(final String action, final String uuid, final AjaxRequestTarget target) {
        if (!ACTIONS_TO_HINTS.containsKey(action)) {
            log.warn("Unknown workflow action '{}'", action);
            target.prependJavaScript(String.format("Hippo.Workflow.reject('Unknown workflow action %s');", action));
            return false;
        }

        final HippoSession userSession = UserSession.get().getJcrSession();
        try {
            final Node handle = userSession.getNodeByIdentifier(uuid);
            final HippoWorkspace workspace = userSession.getWorkspace();
            final WorkflowManager workflowManager = workspace.getWorkflowManager();
            final Workflow wf = workflowManager.getWorkflow("default", handle);
            final Map<String, Serializable> hints = wf.hints();
            final String hint = ACTIONS_TO_HINTS.get(action);

            final Map<String, Serializable> requestsHints = new HashMap<>();
            if (hints.containsKey("requests")) {
                final Map<String, Serializable> requests = (Map<String, Serializable>) hints.get("requests");
                requests.values().forEach(serializable -> {
                    final Map<String, Serializable> requestHints = (Map<String, Serializable>) serializable;
                    requestHints.forEach((hintId, hintValue) -> {
                        if (Boolean.TRUE.equals(hintValue)) {
                            requestsHints.put(hintId, hintValue);
                        }
                    });
                });
            }

            if (!Boolean.TRUE.equals(hints.get(hint)) && !Boolean.TRUE.equals(requestsHints.get(hint))) {
                log.warn("Workflow hint '{}' is not allowed on document '{}'", hint, uuid);
                target.prependJavaScript("Hippo.Workflow.reject();");
                return false;
            }
        } catch (RepositoryException | RemoteException | WorkflowException e) {
            log.error("Error validating workflow request", e);
            target.prependJavaScript(String.format("Hippo.Workflow.reject('%s');", e.getMessage()));
            return false;
        }

        return true;
    }

    private void executeWorkflowRequest(final String uuid, final String category, final String action, final AjaxRequestTarget target) {
        final Node documentNode = getDocumentHandle(uuid);
        final MenuHierarchy menu = buildMenu(Collections.singleton(documentNode), getPluginConfig());

        final MenuHierarchy publicationMenu = menu.getSubmenu(category);
        if (publicationMenu == null) {
            log.warn("Failed to retrieve workflow category {} for document {}", category, uuid);
            return;
        }

        final List<ActionDescription> items = publicationMenu.getItems();
        final ActionDescription actionDescription = items.stream()
                .filter(component -> representsAction(component, action))
                .reduce((first, second) -> second)
                .orElse(null);

        if (actionDescription == null) {
            log.warn("Failed to retrieve workflow action {}.{} for document {}", category, action, uuid);
            return;
        }

        try {
            actionDescription.invokeAsPromise();
        } catch (final Exception e) {
            log.error("Failed to invoke workflow action {}.{} on document {}", category, action, uuid, e);
        }

        target.add(DocumentWorkflowInvokerPlugin.this);
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

    private static boolean representsAction(final Component component, final String action) {
        if (!REQUEST_ACTIONS.contains(action)) {
            return component.getId().equals(action);
        }

        final org.hippoecm.frontend.plugins.reviewedactions.model.Request request = parentObjectAsRequest(component);
        if (request == null) {
            throw new IllegalStateException("Expected parent to contain a workflow request object");
        }

        switch (action) {
            case "cancel":
                return request.getCancel() && !request.getState().equals("request-rejected");
            case "rejected":
                return request.getCancel() && request.getState().equals("request-rejected");
        }

        log.warn("Workflow action '{}' was not found on component {}", action, component.getPath());
        return false;
    }

    private static org.hippoecm.frontend.plugins.reviewedactions.model.Request parentObjectAsRequest(final Component component) {
        final Object parentObject = component.getParent().getDefaultModelObject();
        if (parentObject instanceof org.hippoecm.frontend.plugins.reviewedactions.model.Request) {
            return (org.hippoecm.frontend.plugins.reviewedactions.model.Request) parentObject;
        }
        return null;
    }
}
