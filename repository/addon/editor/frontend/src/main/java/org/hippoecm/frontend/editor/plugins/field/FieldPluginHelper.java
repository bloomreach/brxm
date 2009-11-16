/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.editor.plugins.field;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for field plugins. It reads the field
 * descriptor and produces a JcrItemModel.
 */
public class FieldPluginHelper implements IClusterable {

    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private static final long serialVersionUID = 3671506410576836811L;

    private static final Logger log = LoggerFactory.getLogger(FieldPluginHelper.class);

    private IPluginContext context;
    private IPluginConfig config;

    private IFieldDescriptor field;

    /** Constructor */
    public FieldPluginHelper(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;

        String typeName = config.getString(AbstractFieldPlugin.TYPE);

        String fieldName = config.getString(AbstractFieldPlugin.FIELD);
        if (fieldName == null) {
            log.error("No field was specified in the configuration");
        } else {
            ITemplateEngine engine = getTemplateEngine();
            if (engine != null) {
                ITypeDescriptor type;
                try {
                    if (typeName == null) {
                        type = engine.getType(getNodeModel());
                    } else {
                        type = engine.getType(typeName);
                    }
                    field = type.getField(fieldName);
                    if (field == null) {
                        log.error("Could not resolve field for name " + fieldName);
                    } else if ("*".equals(field.getPath())) {
                        throw new UnsupportedOperationException("Field path * is not supported, field name is "
                                + fieldName);
                    }
                } catch (TemplateEngineException tee) {
                    log.error("Could not resolve field for name " + fieldName, tee);
                }
            } else {
                log.error("No template engine available for " + fieldName);
            }
        }
    }

    public IFieldDescriptor getField() {
        return field;
    }

    protected IPluginConfig getPluginConfig() {
        return config;
    }

    protected IPluginContext getPluginContext() {
        return context;
    }

    protected ITemplateEngine getTemplateEngine() {
        return getPluginContext()
                .getService(getPluginConfig().getString(ITemplateEngine.ENGINE), ITemplateEngine.class);
    }

    protected JcrNodeModel getNodeModel() {
        return (JcrNodeModel) getPluginContext().getService(getPluginConfig().getString("wicket.model"),
                IModelReference.class).getModel();
    }

    protected JcrItemModel getFieldItemModel() {
        return new JcrItemModel(getNodeModel().getItemModel().getPath() + "/" + field.getPath());
    }

}