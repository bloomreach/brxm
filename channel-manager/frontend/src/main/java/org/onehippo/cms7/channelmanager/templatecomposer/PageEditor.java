/**
  * Copyright 2011 Hippo
  *
  * Licensed under the Apache License, Version 2.0 (the  "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
*/
package org.onehippo.cms7.channelmanager.templatecomposer;

import java.util.Arrays;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.wicket.Application;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.templatecomposer.iframe.IFrameBundle;
import org.onehippo.cms7.jquery.JQueryBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtEventAjaxBehavior;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtEventListener;
import org.wicketstuff.js.ext.util.ExtProperty;

@ExtClass("Hippo.ChannelManager.TemplateComposer.PageEditor")
public class PageEditor extends ExtPanel {

    static final Logger log = LoggerFactory.getLogger(PageEditor.class);

    @ExtProperty
    public Boolean debug = false;

    @ExtProperty
    public String composerMountUrl = "/site/";

    @ExtProperty
    public String composerRestMountUrl = "/site/_rp/";

    @ExtProperty
    public String renderHostSubMountPath = "";

    @ExtProperty
    public String renderHost = "";

    @ExtProperty
    public String renderHostParameterName = ContainerConstants.RENDERING_HOST;

    @ExtProperty
    public String ignoreRenderHostParameterName = ContainerConstants.IGNORE_RENDERING_HOST;

    @ExtProperty
    public Boolean previewMode = true;

    @ExtProperty
    public Long initialHstConnectionTimeout = 60000L;

    @ExtProperty
    private String locale;

    private IPluginContext context;

    public PageEditor(final IPluginContext context, final IPluginConfig config) {
        this("item", config);
        this.context = context;
    }

    public PageEditor(final String id, final IPluginConfig config) {
        super(id);
        if (config != null) {
            this.composerMountUrl = config.getString("composerMountUrl", "/site/");
            this.composerRestMountUrl = config.getString("composerRestMountUrl", "/site/_rp/");
            this.renderHost = config.getString("renderHost", "localhost");
            this.renderHostSubMountPath = config.getString("renderHostSubMountPath", "");
            this.initialHstConnectionTimeout = config.getLong("initialHstConnectionTimeout", 60000L);
            if (config.get("previewMode") != null) {
                this.previewMode = config.getBoolean("previewMode");
            }
        }
        this.debug = Application.get().getDebugSettings().isAjaxDebugModeEnabled();
        this.locale = Session.get().getLocale().toString();

        add(CSSPackageResource.getHeaderContribution(PageEditor.class, "plugins/colorfield/colorfield.css"));
        add(new TemplateComposerResourceBehavior());

        addEventListener("edit-document", new ExtEventListener() {
            @Override
            public void onEvent(final AjaxRequestTarget target, final Map<String, JSONArray> parameters) {
                if (context == null) {
                    return;
                }

                JSONArray values = parameters.get("uuid");
                if (values == null || values.length() == 0) {
                    return;
                }
                try {
                    final Object value = values.get(0);
                    IBrowseService browseService = context.getService("service.browse", IBrowseService.class);
                    if (browseService != null) {
                        browseService.browse(new JcrNodeModel(UserSession.get().getJcrSession().getNodeByIdentifier(value.toString())));
                    } else {
                        log.warn("Could not find document " + value);
                    }
                } catch (JSONException e) {
                    throw new WicketRuntimeException("Invalid JSON parameters", e);
                } catch (ItemNotFoundException e) {
                    log.warn("Could not find document to browse to", e);
                } catch (RepositoryException e) {
                    log.error("Internal error when browsing to document", e);
                }

            }
        });
    }

    @Override
    public void buildInstantiationJs(StringBuilder js, String extClass, JSONObject properties) {
        js.append(String.format(" try { Ext.namespace(\"%s\"); window.%s = new %s(%s); } catch (e) { Ext.Msg.alert('Error', 'Error instantiating template composer. '+e); }; \n", getMarkupId(), getMarkupId(), extClass, properties.toString()));
    }

    @Override
    public String getMarkupId() {
        return "Hippo.ChannelManager.TemplateComposer.Instance";
    }

    @Override
    protected void onRenderProperties(final JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        RequestCycle rc = RequestCycle.get();
        properties.put("iFrameErrorPage", Arrays.asList(
            rc.urlFor(new ResourceReference(IFrameBundle.class, IFrameBundle.ERROR_HTML)).toString()
        ));
        properties.put("iFrameCssHeadContributions", Arrays.asList(
            rc.urlFor(new ResourceReference(IFrameBundle.class, IFrameBundle.PAGE_EDITOR_CSS)).toString(),
            rc.urlFor(new ResourceReference(IFrameBundle.class, IFrameBundle.SURFANDEDIT_CSS)).toString()
        ));
        if (debug) {
            properties.put("iFrameJsHeadContributions", Arrays.asList(
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_CORE)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_CLASS_PLUGIN)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_NAMESPACE_PLUGIN)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_UI)).toString(),

                rc.urlFor(new ResourceReference(PageEditor.class, "globals.js")).toString(),
                rc.urlFor(new ResourceReference(IFrameBundle.class, IFrameBundle.MAIN)).toString(),
                rc.urlFor(new ResourceReference(IFrameBundle.class, IFrameBundle.UTIL)).toString(),
                rc.urlFor(new ResourceReference(IFrameBundle.class, IFrameBundle.FACTORY)).toString(),
                rc.urlFor(new ResourceReference(IFrameBundle.class, IFrameBundle.PAGE)).toString(),
                rc.urlFor(new ResourceReference(IFrameBundle.class, IFrameBundle.WIDGETS)).toString(),
                rc.urlFor(new ResourceReference(IFrameBundle.class, IFrameBundle.SURFANDEDIT)).toString()
            ));
        } else {
            properties.put("iFrameJsHeadContributions", Arrays.asList(
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_ALL_MIN)).toString(),
                rc.urlFor(new ResourceReference(IFrameBundle.class, IFrameBundle.ALL)).toString()
            ));
        }
    }

    @Override
    protected ExtEventAjaxBehavior newExtEventBehavior(final String event) {
        if ("edit-document".equals(event)) {
            return new ExtEventAjaxBehavior("uuid");
        }
        return super.newExtEventBehavior(event);
    }

    public void setChannel(String renderHost, String mountPath) {
        this.renderHost = renderHost;
        this.renderHostSubMountPath = mountPath;
    }

    public Boolean getPreviewMode() {
        return previewMode;
    }

    public void setPreviewMode(final Boolean previewMode) {
        this.previewMode = previewMode;
    }

    public String getComposerRestMountUrl() {
        return composerRestMountUrl;
    }

}
