/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.dialog;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;

public abstract class DialogBehavior extends AbstractDefaultAjaxBehavior {

    @Override
    protected void respond(final AjaxRequestTarget target) {
        showDialog(getParameters());
    }

    protected Map<String, String> getParameters() {
        final Request request = RequestCycle.get().getRequest();
        final IRequestParameters parameters = request.getPostParameters();
        return parameters.getParameterNames()
                .stream()
                .collect(Collectors.toMap(
                        name -> name,
                        name -> parameters.getParameterValue(name).toString()
                ));
    }

    protected abstract void showDialog(final Map<String, String> parameters);
}
