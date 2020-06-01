/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.validator.FilteredValidationModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.Observer;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.validation.IValidationListener;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ModelPathElement;
import org.hippoecm.frontend.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldItem<C extends IModel> implements IDetachable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(FieldItem.class);

    private IPluginContext context;
    private IClusterControl clusterControl;
    private ModelReference<?> modelRef;
    private FilteredValidationModel filteredValidationModel;
    private IObserver validationObserver;
    private IValidationService validationService;

    public FieldItem(final IPluginContext context, final C model,
            final IModel<IValidationResult> validationModel, IClusterControl control, final ModelPathElement element) {
        this.context = context;

        String modelId = control.getClusterConfig().getString(RenderService.MODEL_ID);
        modelRef = new ModelReference(modelId, model);
        modelRef.init(context);

        validationService = null;
        if (validationModel != null) {
            if (validationModel.getObject() != null) {
                filteredValidationModel = new FilteredValidationModel(validationModel, element);
                final String validationServiceId = control.getClusterConfig().getString(IValidationService.VALIDATE_ID);
                if (validationServiceId != null) {
                    validationService = new IValidationService() {
                        private static final long serialVersionUID = 1L;
    
                        public IValidationResult getValidationResult() {
                            return filteredValidationModel.getObject();
                        }
    
                        public void validate() throws ValidationException {
                            throw new ValidationException("Initiating validation from template is unsupported");
                        }
    
                    };
                    context.registerService(validationService, validationServiceId);
                    if (validationModel instanceof IObservable) {
                        context.registerService(validationObserver = new Observer((IObservable) validationModel) {
    
                            public void onEvent(Iterator events) {
                                List<IValidationListener> listeners = context.getServices(validationServiceId,
                                        IValidationListener.class);
                                for (IValidationListener listener : listeners) {
                                    listener.onValidation(filteredValidationModel.getObject());
                                }
                            }
    
                        }, IObserver.class.getName());
                    }
                }
            } else {
                if (control.getClusterConfig().containsKey(IValidationService.VALIDATE_ID)) {
                    log.warn("Template supports validation, but container does not provide validator model");
                }
            }
        }

        this.clusterControl = control;

        control.start();
    }

    public void destroy() {
        String validationServiceId = clusterControl.getClusterConfig().getString(IValidationService.VALIDATE_ID);
        if (validationService != null) {
            if (validationObserver != null) {
                context.unregisterService(validationObserver, IObserver.class.getName());
                validationObserver = null;
            }
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

    public boolean isValid() {
        if (filteredValidationModel != null && filteredValidationModel.getObject() != null) {
            return filteredValidationModel.getObject().isValid();
        }
        return true;
    }

    public void detach() {
        if (filteredValidationModel != null) {
            filteredValidationModel.detach();
        }
    }

}
