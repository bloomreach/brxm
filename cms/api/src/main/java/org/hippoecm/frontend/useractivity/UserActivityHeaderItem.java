/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.HippoHeaderItem;

public class UserActivityHeaderItem extends HippoHeaderItem {

    public static final String AJAX_ATTR_SYSTEM_ACTIVITY = "isSystemActivity";

    private static final String USER_ACTIVITY_API_JS = "user-activity.js";

    private final int maxInactiveIntervalMinutes;

    public UserActivityHeaderItem(final int maxInactiveIntervalMinutes) {
        this.maxInactiveIntervalMinutes = maxInactiveIntervalMinutes;
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("user-activity-header-item");
    }

    @Override
    public List<HeaderItem> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public void render(final Response response) {
        OnDomReadyHeaderItem.forScript(createUserActivityScript()).render(response);
    }

    private String createUserActivityScript() {
        final Map<String, String> scriptParams = new TreeMap<>();

        scriptParams.put("maxInactiveIntervalMinutes", Integer.toString(maxInactiveIntervalMinutes));
        scriptParams.put("ajaxAttrSystemActivity", AJAX_ATTR_SYSTEM_ACTIVITY);

        final PackageTextTemplate javaScript = new PackageTextTemplate(UserActivityHeaderItem.class, USER_ACTIVITY_API_JS);
        return javaScript.asString(scriptParams);
    }

}
