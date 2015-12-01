/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.templatecomposer.channeleditor;

import org.apache.wicket.Session;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.ExtStoreFuture;
import org.onehippo.cms7.channelmanager.templatecomposer.TemplateComposerHeaderItem;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.JSONIdentifier;

@ExtClass("Hippo.ChannelManager.TemplateComposer.ChannelEditor")
public class ChannelEditor extends ExtPanel {

    private static final Boolean DEFAULT_PREVIEW_MODE = Boolean.TRUE;

    @ExtProperty
    @SuppressWarnings("unused")
    private String locale;

    @ExtProperty
    @SuppressWarnings("unused")
    private String composerRestMountPath;

    @ExtProperty
    @SuppressWarnings("unused")
    private String renderPathInfo = "";

    @ExtProperty
    @SuppressWarnings("unused")
    private String contextPath = "";

    @ExtProperty
    @SuppressWarnings("unused")
    private String cmsPreviewPrefix;

    @ExtProperty
    @SuppressWarnings("unused")
    private String channelId;

    @ExtProperty
    private Boolean previewMode = DEFAULT_PREVIEW_MODE;

    private ExtStoreFuture<Object> channelStoreFuture;
    private boolean redraw = false;


    public ChannelEditor(final IPluginContext context, final IPluginConfig config, final String defaultContextPath,
                         final String composerRestMountPath, final ExtStoreFuture<Object> channelStoreFuture) {
        this.channelStoreFuture = channelStoreFuture;
        this.composerRestMountPath = composerRestMountPath;
        this.contextPath = defaultContextPath;

        this.locale = Session.get().getLocale().toString();

        //TODO: register event listener (like edit-document)
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(TemplateComposerHeaderItem.get());
    }

    @Override
    public void buildInstantiationJs(StringBuilder js, String extClass, JSONObject properties) {
        js.append(String.format(
                " try { Ext.namespace(\"%s\"); window.%s = new %s(%s); } catch (e) { Ext.Msg.alert('Error', 'Error instantiating template composer. '+e); }; \n",
                getMarkupId(), getMarkupId(), extClass, properties.toString()));
    }

    @Override
    public String getMarkupId() {
        return "Hippo.ChannelManager.TemplateComposer.Instance";
    }

    @Override
    protected void onRenderProperties(final JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        properties.put("channelStoreFuture", new JSONIdentifier(this.channelStoreFuture.getJsObjectId()));
    }

    public void render(final PluginRequestTarget target) {
        if (redraw) {
            redraw = false;
        }
    }

    public void setChannel(final String channelId) {
        this.channelId = channelId;
        redraw();
    }

    public void setRenderPathInfo(String pathInfo) {
        this.renderPathInfo = pathInfo;
        redraw();
    }

    public void setRenderContextPath(String contextPath) {
        this.contextPath = contextPath;
        redraw();
    }

    public void setCmsPreviewPrefix(final String cmsPreviewPrefix) {
        this.cmsPreviewPrefix = cmsPreviewPrefix;
    }

    public void setPreviewMode(final Boolean previewMode) {
        this.previewMode = previewMode;
        redraw();
    }

    public void redraw() {
        redraw = true;
    }

}
