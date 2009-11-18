/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.editor.plugins;

import java.util.Iterator;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextTemplatePlugin extends RenderPlugin<String> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TextTemplatePlugin.class);

    public TextTemplatePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        // FIXME: remove this code; 
        if (config.containsKey("validator.model")) {
            String validatorModelId = config.getString("validator.model");
            IModelReference<IValidationResult> validationModelRef = context.getService(validatorModelId,
                    IModelReference.class);
            if (validationModelRef != null) {
                final IModel<IValidationResult> validationModel = validationModelRef.getModel();
                if (validationModel instanceof IObservable) {
                    context.registerService(new IObserver<IObservable>() {
                        private static final long serialVersionUID = 1L;

                        public IObservable getObservable() {
                            return (IObservable) validationModel;
                        }

                        public void onEvent(Iterator<? extends IEvent<IObservable>> events) {
                            if (!validationModel.getObject().isValid()) {
                                getModel().setObject("Invalid!");
                                redraw();
                            }
                        }

                    }, IObserver.class.getName());
                }
            }
        }

        JcrPropertyValueModel valueModel = (JcrPropertyValueModel) getDefaultModel();
        String mode = config.getString("mode", "view");
        if (ITemplateEngine.EDIT_MODE.equals(mode)) {
            TextAreaWidget widget = new TextAreaWidget("value", valueModel);
            if (config.getString("rows") != null) {
                widget.setRows(config.getString("rows"));
            }
            if (config.getString("cols") != null) {
                widget.setCols(config.getString("cols"));
            }
            add(widget);
        } else {
            add(new Label("value", valueModel));
        }
    }

}
