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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.string.*;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.IAjaxSettings;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.onehippo.yui.YuiNamespace;

public class PathHistoryBehavior extends AbstractYuiAjaxBehavior implements YuiNamespace, IObserver {

    private IModelReference reference;

    public PathHistoryBehavior(IAjaxSettings settings, IModelReference reference) {
        super(settings);

        this.reference  = reference;

        setPathFromRequest();
    }

    private void setPathFromRequest() {
        final RequestCycle requestCycle = RequestCycle.get();
        StringValue path = requestCycle.getRequest().getQueryParameters().getParameterValue("path");
        if (!path.isNull()) {
            reference.setModel(new JcrNodeModel(path.toString()));
        }
    }

    @Override
    public void addHeaderContribution(final IYuiContext context) {
        context.addModule(this, "pathhistory");
        context.addTemplate(PathHistoryBehavior.class, "js/init.js", getParameters());
    }

    private Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("callbackUrl", getCallbackUrl());
        params.put("callbackFunction", getCallbackFunction());
        return params;
    }

    @Override
    protected void respond(final AjaxRequestTarget target) {
        setPathFromRequest();
    }

    @Override
    public String getPath() {
        return "js/";
    }

    @Override
    public IObservable getObservable() {
        return reference;
    }

    @Override
    public void onEvent(final Iterator events) {
        JcrNodeModel nodeModel = (JcrNodeModel) reference.getModel();
        String path = nodeModel.getItemModel().getPath();
        AjaxRequestTarget ajax = RequestCycle.get().find(AjaxRequestTarget.class);
        ajax.appendJavaScript("YAHOO.hippo.PathHistory.setPath('" + path + "')");
    }
}
