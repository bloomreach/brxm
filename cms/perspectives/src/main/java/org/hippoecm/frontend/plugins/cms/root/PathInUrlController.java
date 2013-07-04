/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.root;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PathInUrlController extends Behavior implements IObserver<IModelReference<Node>> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(PathInUrlController.class);
    private static final JavaScriptResourceReference URLCONTROLLER_JS = new JavaScriptResourceReference(PathInUrlController.class, "PathInUrlController.js");

    private final IModelReference<Node> modelReference;
    private final String parameterName;

    public PathInUrlController(final IModelReference<Node> modelReference, final String parameterName) {
        this.modelReference = modelReference;
        this.parameterName = parameterName;
    }

    @Override
    public void renderHead(Component component, final IHeaderResponse response) {
        response.render(JavaScriptHeaderItem.forReference(URLCONTROLLER_JS));
    }

    @Override
    public IModelReference<Node> getObservable() {
        return modelReference;
    }

    @Override
    public void onEvent(final Iterator<? extends IEvent<IModelReference<Node>>> events) {
        showPathInUrl(modelReference.getModel());
    }

    private void showPathInUrl(final IModel<Node> nodeModel) {
        final String path = getPathToShow(nodeModel.getObject());
        showPathInUrl(path);
    }

    private String getPathToShow(final Node node) {
        if (node != null) {
            try {
                return node.getPath();
            } catch (RepositoryException e) {
                log.warn("Could not retrieve path of node model, path to the node will not be shown in the URL", e);
            }
        }
        return StringUtils.EMPTY;
    }

    private void showPathInUrl(final String path) {
        final AjaxRequestTarget requestTarget = RequestCycle.get().find(AjaxRequestTarget.class);
        if (requestTarget != null) {
            final String javascript = String.format("Hippo.showParameterInUrl('%s', '%s');", parameterName, path);
            requestTarget.appendJavaScript(javascript);
        }
    }

}
