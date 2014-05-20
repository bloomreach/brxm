/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import java.nio.charset.Charset;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.encoding.UrlDecoder;
import org.apache.wicket.util.encoding.UrlEncoder;
import org.apache.wicket.util.string.*;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.richtext.ILinkDecorator;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PreviewLinksBehavior extends AbstractDefaultAjaxBehavior implements ILinkDecorator {

    private static final long serialVersionUID = 1L;

    private static final String JS_PREVENT_DEFAULT = "Wicket.Event.fix(event).preventDefault();";
    private static final String JS_STOP_EVENT = "Wicket.Event.stop(event);";
    private static final Logger log = LoggerFactory.getLogger(PreviewLinksBehavior.class);

    private final IModel<Node> model;
    private final IBrowseService browser;
    /**
     * When this behavior is used in the context of a diff view, the diffing process already
     * encodes the urls. So this boolean is to prevent links being double encoded in that case.
     */
    private final boolean encode;

    PreviewLinksBehavior(final IModel<Node> model, final IBrowseService browser, boolean encode) {
        this.model = model;
        this.browser = browser;
        this.encode = encode;
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        Request request = RequestCycle.get().getRequest();
        final StringValue linkValue = request.getRequestParameters().getParameterValue("link");
        if (linkValue != null) {
            String link = linkValue.toString();
            link = UrlDecoder.QUERY_INSTANCE.decode(link, request.getCharset());
            if (browser != null) {
                Node node = model.getObject();
                try {
                    if (node.hasNode(link)) {
                        node = node.getNode(link);
                        if (node.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                            final String uuid = node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                            final Session jcrSession = UserSession.get().getJcrSession();
                            node = jcrSession.getNodeByIdentifier(uuid);
                            browser.browse(new JcrNodeModel(node));
                        }
                    }
                } catch (ItemNotFoundException ex) {
                    log.info("Could not resolve link", ex);
                } catch (RepositoryException e) {
                    log.error("Error while browing to link", e);
                }
            }
        }
    }

    @Override
    public String internalLink(String link) {
        final AjaxRequestAttributes attributes = getAttributes();
        final Charset charset = RequestCycle.get().getRequest().getCharset();
        if (encode) {
            link = UrlEncoder.QUERY_INSTANCE.encode(link, charset);
        }
        attributes.getExtraParameters().put("link", link);
        CharSequence asString = renderAjaxAttributes(getComponent(), attributes);
        return "href=\"#\" onclick='" + JS_PREVENT_DEFAULT + JS_STOP_EVENT + "Wicket.Ajax.get(" + asString + ");'";
    }

    @Override
    public String externalLink(String link) {
        return "href=\"" + link + "\" onclick=\"" + JS_STOP_EVENT + "\"";
    }
}
