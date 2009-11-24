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
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.validator.FilteredValidationModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ModelPathElement;
import org.hippoecm.frontend.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldItemRenderer<C extends IModel> implements IClusterable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(FieldItemRenderer.class);

    private IPluginContext context;
    private IClusterControl clusterControl;
    private ModelReference<?> modelRef;
    private IModel<IValidationResult> filteredValidationModel;
    private IValidationService validationService;

    public FieldItemRenderer(IPluginContext context, final C model, IModel<IValidationResult> validationModel,
            IClusterControl control, ModelPathElement element) {
        this.context = context;

        String modelId = control.getClusterConfig().getString(RenderService.MODEL_ID);
        modelRef = new ModelReference(modelId, model);
        modelRef.init(context);

        validationService = null;
        if (validationModel != null) {
            filteredValidationModel = new FilteredValidationModel(validationModel, element);
            String validationServiceId = control.getClusterConfig().getString(IValidationService.VALIDATE_ID);
            if (validationServiceId != null) {
                validationService = new IValidationService() {
                    private static final long serialVersionUID = 1L;

                    public IValidationResult getValidationResult() {
                        return filteredValidationModel.getObject();
                    }

                    public void validate() throws ValidationException {
                        // TODO Auto-generated method stub

                    }

                };
                context.registerService(validationService, validationServiceId);
            }
        } else {
            if (control.getClusterConfig().containsKey("validator.model")) {
                log.warn("Template supports validation, but container does not provide validator model");
            }
        }

        this.clusterControl = control;

        control.start();
    }

    public void destroy() {
        String validationServiceId = clusterControl.getClusterConfig().getString(IValidationService.VALIDATE_ID);
        if (validationServiceId != null) {
            context.unregisterService(validationService, validationServiceId);
            validationService = null;
        }
        modelRef.destroy();
        clusterControl.stop();
    }

    public String getRendererId() {
        return clusterControl.getClusterConfig().getString("wicket.id");
    }

    public C getModel() {
        return (C) modelRef.getModel();
    }

    boolean hasValidation() {
        return validationService != null;
    }

    boolean isValid() {
        if (filteredValidationModel != null && filteredValidationModel.getObject() != null) {
            return filteredValidationModel.getObject().isValid();
        }
        return true;
    }

}