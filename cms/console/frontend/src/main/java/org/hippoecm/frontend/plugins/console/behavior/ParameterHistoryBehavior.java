/*
 * Copyright 2012-2022 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.console.behavior;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.encoding.UrlEncoder;
import org.apache.wicket.util.string.StringValue;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParameterHistoryBehavior extends AbstractDefaultAjaxBehavior implements IObserver {

    private static final Logger log = LoggerFactory.getLogger(ParameterHistoryBehavior.class);

    private static final JavaScriptResourceReference SCRIPT_RESOURCE_REFERENCE = new JavaScriptResourceReference(ParameterHistoryBehavior.class, "js/parameterhistory/parameterhistory.js");

    private static final String PATH_PARAMETER = "_path";
    private static final String UUID_PARAMETER = "_uuid";

    private IModelReference<Node> reference;
    private transient boolean myUpdate;

    public ParameterHistoryBehavior(IModelReference<Node> reference) {
        this.reference = reference;
        setReferenceModelFromRequest();
    }

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        super.renderHead(component, response);

        response.render(JavaScriptHeaderItem.forReference(SCRIPT_RESOURCE_REFERENCE));

        String attributesAsJson = renderAjaxAttributes(component).toString();
        response.render(OnLoadHeaderItem.forScript(
                "Hippo.ParameterHistory.init(function(path, uuid) {\n"
                        + "    var call = new Wicket.Ajax.Call(),"
                        + "        attributes = jQuery.extend({}, " + attributesAsJson + ");\n"
                        + "    call.ajax(attributes);\n"
                        + "});"));
    }

    @Override
    protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
        super.updateAjaxAttributes(attributes);
        final List<CharSequence> dep = attributes.getDynamicExtraParameters();
        dep.add("return { " + PATH_PARAMETER + ": path };");
        dep.add("return { " + UUID_PARAMETER + ": uuid };");
    }

    @Override
    protected void respond(final AjaxRequestTarget target) {
        setReferenceModelFromRequest();
    }

    @Override
    public IObservable getObservable() {
        return reference;
    }

    @Override
    public void onEvent(final Iterator events) {
        if (myUpdate) {
            return;
        }

        final IModel<Node> model = reference.getModel();
        final String path = JcrUtils.getNodePathQuietly(model != null ? model.getObject() : null);
        if (path != null) {
            setPathWithAjax(path, false);
        }
    }

    private void setReferenceModelFromRequest() {
        myUpdate = true;

        final RequestCycle requestCycle = RequestCycle.get();
        final IRequestParameters queryParameters = requestCycle.getRequest().getQueryParameters();

        final StringValue path = queryParameters.getParameterValue(PATH_PARAMETER);
        if (!path.isEmpty()) {
            setReferenceFromPath(path.toString());
        } else {
            final StringValue uuid = queryParameters.getParameterValue(UUID_PARAMETER);
            if (!uuid.isEmpty()) {
                setReferenceFromUuid(uuid.toString());
            }
        }

        myUpdate = false;
    }

    private void setReferenceFromUuid(final String uuid) {
        final Session jcrSession = UserSession.get().getJcrSession();
        Node node = null;
        try {
            node = jcrSession.getNodeByIdentifier(uuid);
        } catch (RepositoryException e) {
            log.info("Could not find node by uuid: {}", uuid);
        }

        if (node != null) {
            final JcrNodeModel model = new JcrNodeModel(node);
            reference.setModel(model);
            setPathWithAjax(model.getItemModel().getPath(), true);
        }
    }

    private void setReferenceFromPath(final String path) {
        final JcrNodeModel startModel = new JcrNodeModel(path);
        JcrNodeModel model = startModel;
        while (model != null && model.getNode() == null) {
            model = model.getParentModel();
        }

        if (!startModel.equals(model)) {
            if (model == null) {
                model = new JcrNodeModel("/");
            }
            setPathWithAjax(model.getItemModel().getPath(), true);
        }
        reference.setModel(model);
    }

    private void setPathWithAjax(final String path, final boolean replace) {
        final Optional<AjaxRequestTarget> ajax = RequestCycle.get().find(AjaxRequestTarget.class);
        ajax.ifPresent(ajaxRequestTarget -> {
            final String encoding = Application.get().getRequestCycleSettings().getResponseRequestEncoding();
            final String encodedPath = UrlEncoder.QUERY_INSTANCE.encode(path, encoding);
            final String script = String.format("Hippo.ParameterHistory.setPath('%s', %s);", encodedPath, replace);
            ajaxRequestTarget.appendJavaScript(script);
        });
    }
}
