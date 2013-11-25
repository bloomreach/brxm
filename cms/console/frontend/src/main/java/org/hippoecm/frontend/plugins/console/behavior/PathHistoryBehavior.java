/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.string.StringValue;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;

public class PathHistoryBehavior extends AbstractDefaultAjaxBehavior implements IObserver {

    private static final String PATH_PARAMETER = "path";

    private IModelReference reference;
    private transient boolean myUpdate;

    public PathHistoryBehavior(IModelReference reference) {
        this.reference = reference;

        setPathFromRequest();
    }

    private void setPathFromRequest() {
        final RequestCycle requestCycle = RequestCycle.get();
        StringValue path = requestCycle.getRequest().getQueryParameters().getParameterValue(PATH_PARAMETER);
        if (!path.isNull()) {
            try {
                myUpdate = true;
                reference.setModel(new JcrNodeModel(path.toString()));
            } finally {
                myUpdate = false;
            }
        }
    }

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        super.renderHead(component, response);

        response.render(JavaScriptHeaderItem.forReference(new JavaScriptResourceReference(PathHistoryBehavior.class, "js/pathhistory/pathhistory.js")));

        String attributesAsJson = renderAjaxAttributes(component).toString();
        response.render(OnLoadHeaderItem.forScript(
                "Hippo.PathHistory.init(function(path) {\n"
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
    }

    @Override
    protected void respond(final AjaxRequestTarget target) {
        setPathFromRequest();
    }

    @Override
    public IObservable getObservable() {
        return reference;
    }

    @Override
    public void onEvent(final Iterator events) {
        if (!myUpdate) {
            JcrNodeModel nodeModel = (JcrNodeModel) reference.getModel();
            String path = nodeModel.getItemModel().getPath();
            AjaxRequestTarget ajax = RequestCycle.get().find(AjaxRequestTarget.class);
            ajax.appendJavaScript("Hippo.PathHistory.setPath('" + path + "')");
        }
    }
}
