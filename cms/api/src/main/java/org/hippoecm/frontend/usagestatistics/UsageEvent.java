/*
 * Copyright 2015-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.usagestatistics;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.cycle.RequestCycle;

import net.sf.json.JSONObject;

import java.util.Optional;

public class UsageEvent {

    private final String name;
    private final JSONObject parameters;

    public UsageEvent(final String name) {
        this.name = name;
        this.parameters = new JSONObject();
    }

    public void setParameter(final String name, final String value) {
        parameters.put(name, value);
    }

    public void publish() {
        final Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
        target.ifPresent(ajaxRequestTarget -> ajaxRequestTarget.appendJavaScript(getJavaScript()));
    }

    public String getJavaScript() {
        final StringBuilder js = new StringBuilder("Hippo.Events.publish('");
        js.append(name);
        js.append("'");
        if (!parameters.isEmpty()) {
            js.append(",");
            js.append(parameters.toString());
        }
        js.append(");");
        return js.toString();
    }

}
