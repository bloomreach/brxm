/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.mixin;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixinLoaderPlugin extends RenderPlugin<Node> {

    private static final Logger log = LoggerFactory.getLogger(MixinLoaderPlugin.class);

    private final Map<String, IClusterControl> controllers;

    protected final IEditor.Mode mode;

    public MixinLoaderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        controllers = new HashMap<>();

        mode = IEditor.Mode.fromString(config.getString(ITemplateEngine.MODE), IEditor.Mode.VIEW);

        addExtensionPoint("mixins");

        final IModel<Node> model = getModel();
        final Node node = model.getObject();
        if (node != null) {
            final String mixin = config.getString("mixin");
            try {
                if (JcrHelper.isNodeType(node, mixin)) {
                    controllers.put(mixin, startMixin(mixin));
                }
            } catch (TemplateEngineException ex) {
                log.error("Unable to start editor for mixin " + mixin, ex);
            } catch (RepositoryException e) {
                log.error("Unable to determine whether document has mixin " + mixin, e);
            }
        }

    }

    @Override
    protected ExtensionPoint createExtensionPoint(final String extension) {
        return new RenderService<Node>.ExtensionPoint(extension) {

            @Override
            protected void register() {
                final IPluginConfig config = getPluginConfig();
                final IPluginContext context = getPluginContext();
                if (config.containsKey(extension)) {
                    context.registerTracker(this, config.getString(extension));
                } else {
                    context.registerTracker(this, getServiceName(extension));
                }
            }

            @Override
            protected void unregister() {
                final IPluginConfig config = getPluginConfig();
                final IPluginContext context = getPluginContext();
                if (config.containsKey(extension)) {
                    context.unregisterTracker(this, config.getString(extension));
                } else {
                    context.unregisterTracker(this, getServiceName(extension));
                }
            }
        };
    }

    protected IClusterControl startMixin(String mixin) throws TemplateEngineException {
        final IPluginContext context = getPluginContext();
        final IPluginConfig config = getPluginConfig();
        final ITemplateEngine engine = context.getService(config.getString(ITemplateEngine.ENGINE), ITemplateEngine.class);
        final IClusterConfig template = engine.getTemplate(engine.getType(mixin), mode);

        IPluginConfig parameters = config.getPluginConfig("cluster.options");
        if (parameters == null) {
            parameters = new JavaPluginConfig();
        } else {
            parameters = new JavaPluginConfig(parameters);
        }
        parameters.put("wicket.id", getServiceName("mixins"));
        parameters.put("mode", mode.toString());
        parameters.put("engine", getPluginConfig().get("engine"));
        parameters.put("wicket.model", getPluginConfig().get("wicket.model"));
        parameters.put("model.compareTo", getPluginConfig().get("model.compareTo"));

        final IClusterControl control = getPluginContext().newCluster(template, parameters);
        control.start();
        return control;
    }

    private String getServiceName(String extension) {
        return getPluginContext().getReference(this).getServiceId() + "." + extension;
    }

}
