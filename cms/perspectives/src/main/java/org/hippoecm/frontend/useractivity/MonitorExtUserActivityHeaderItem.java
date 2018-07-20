/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.useractivity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.HippoHeaderItem;
import org.wicketstuff.js.ext.util.ExtResourcesHeaderItem;

public class MonitorExtUserActivityHeaderItem extends HippoHeaderItem {

    private static final ResourceReference EXT_USER_ACTIVITY_JS = new JavaScriptResourceReference(MonitorExtUserActivityHeaderItem.class, "monitor-ext-user-activity.js");

    private final UserActivityHeaderItem api;

    public MonitorExtUserActivityHeaderItem(UserActivityHeaderItem api) {
        this.api = api;
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("ext-user-activity-header-item");
    }

    @Override
    public List<HeaderItem> getDependencies() {
        return Arrays.asList(api, ExtResourcesHeaderItem.get());
    }

    @Override
    public void render(final Response response) {
        JavaScriptHeaderItem.forReference(EXT_USER_ACTIVITY_JS).render(response);
    }

}


