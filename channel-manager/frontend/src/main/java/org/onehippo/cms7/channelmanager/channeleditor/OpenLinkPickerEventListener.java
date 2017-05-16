/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.channelmanager.channeleditor;

import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.json.JSONArray;
import org.wicketstuff.js.ext.ExtEventAjaxBehavior;
import org.wicketstuff.js.ext.util.ExtEventListener;

/**
 * Opens the link picker to select the UUID and (optionally) the title and target of an internal link.
 */
public class OpenLinkPickerEventListener extends ExtEventListener {

    private static final String PARAM_UUID = "uuid";
    private static final String PARAM_TITLE = "title";
    private static final String PARAM_TARGET = "target";

    private final String channelEditorId;

    OpenLinkPickerEventListener(final String channelEditorId) {
        this.channelEditorId = channelEditorId;
    }

    static ExtEventAjaxBehavior getExtEventBehavior() {
        return new ExtEventAjaxBehavior(PARAM_UUID, PARAM_TITLE, PARAM_TARGET);
    }

    @Override
    public void onEvent(final AjaxRequestTarget ajaxRequestTarget, final Map<String, JSONArray> parameters) {
        String uuid = getParameter(PARAM_UUID, parameters).orElse("");
        String title = getParameter(PARAM_TITLE, parameters).orElse("");
        String target = getParameter(PARAM_TARGET, parameters).orElse("");

        System.out.println("TODO: open link picker, uuid=" + uuid + ", title=" + title + ", target=" + target);

        // TODO: change parameters in the link picker
        returnResult(uuid, title + "-test", target, ajaxRequestTarget);
    }

    private void returnResult(final String uuid, final String title, final String target, final AjaxRequestTarget ajaxRequestTarget) {
        final String resultScript = String.format("Ext.getCmp('%s').onLinkPicked('%s', '%s', '%s');",
                channelEditorId, uuid, title, target);
        ajaxRequestTarget.appendJavaScript(resultScript);
    }
}
