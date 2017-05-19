/*
 * Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.richtext.view;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_DELETED;

class PreviewLinksBehavior extends AbstractDefaultAjaxBehavior {

    private static final Logger log = LoggerFactory.getLogger(PreviewLinksBehavior.class);

    private static final ResourceReference PREVIEW_LINKS_SERVICE_JS =
            new JavaScriptResourceReference(PreviewLinksBehavior.class, "preview-links-service.js");

    private final IBrowseService browser;

    PreviewLinksBehavior(final IBrowseService browser) {
        this.browser = browser;
    }

    @Override
    protected void respond(final AjaxRequestTarget target) {
        final Request request = RequestCycle.get().getRequest();
        final String uuid = request.getRequestParameters().getParameterValue("uuid").toString();
        if (uuid != null && !"null".equals(uuid) && browser != null) {
            final Node targetNode = getNodeById(uuid);
            if (targetNode == null) {
                log.info("Node with UUID {} could not be loaded", uuid);
                final String message = new ClassResourceModel("brokenlink-alert",
                                                              PreviewLinksBehavior.class).getObject();
                target.appendJavaScript("alert('" + message + "');");
            } else {
                browser.browse(new JcrNodeModel(targetNode));
            }
        }
    }

    private Node getNodeById(final String uuid) {
        try {
            final Session jcrSession = UserSession.get().getJcrSession();
            final Node targetNode = jcrSession.getNodeByIdentifier(uuid);
            if (!targetNode.getNode(targetNode.getName()).isNodeType(NT_DELETED)) {
                return targetNode;
            }
        } catch (final RepositoryException ignore) {
        }
        return null;
    }

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        super.renderHead(component, response);

        final String markupId = getComponent().getMarkupId();
        final CharSequence config = renderAjaxAttributes(getComponent(), getAttributes());
        final CharSequence script = String.format("Hippo.augmentInternalLinks('%s', %s);", markupId, config);

        response.render(JavaScriptReferenceHeaderItem.forReference(PREVIEW_LINKS_SERVICE_JS));
        response.render(OnLoadHeaderItem.forScript(script));
    }
}
