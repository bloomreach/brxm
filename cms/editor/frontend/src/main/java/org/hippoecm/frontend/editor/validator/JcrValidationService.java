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
package org.hippoecm.frontend.editor.validator;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.editor.type.JcrTypeLocator;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.hippoecm.frontend.validation.IValidationListener;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.ValidationResult;
import org.hippoecm.frontend.validation.Violation;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A validation engine that registers itself as an {@link org.hippoecm.frontend.validation.IValidationService}.  All supported
 * validation rules are hardcoded.  Generic node types will be validated according to the
 * "required" and "non-empty" rules that apply to fields (see {@link org.hippoecm.frontend.editor.validator.JcrFieldValidator}).
 * The template type node type is validated by the {@link org.hippoecm.frontend.editor.validator.TemplateTypeValidator}.
 * <p>
 * Validation can be triggered by invoking the {@link org.hippoecm.frontend.validation.IValidationService#validate()} method.
 * Results of the validation are returned and made available on the validator.model model
 * service.
 * <p>
 * Configure with
 * <ul>
 * <li><b>validator.id (IValidateService)</b>
 * The name that will be used to register the validation service.
 * <li><b>wicket.model</b>
 * The model that is used in validation.
 * <li><b>validator.model</b>
 * The model where the {@link org.hippoecm.frontend.validation.IValidationResult} is made available.  Plugins that need to
 * change their appearance or functionality based on the validation status can observe the
 * model that is registered here.
 * </ul>
 */
public class JcrValidationService implements IValidationService, IDetachable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrValidationService.class);

    private IPluginContext context;
    private IPluginConfig config;

    private ITypeLocator locator;
    private IFeedbackLogger logger;
    private ValidationResult result;
    private boolean validated = false;

    @SuppressWarnings("unchecked")
    public JcrValidationService(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;

        if (!config.containsKey("wicket.model") || !config.containsKey(IValidationService.VALIDATE_ID)) {
            log.warn("No wicket.model or validator.id configured for validation engine");
            return;
        }

        locator = new JcrTypeLocator();

        result = new ValidationResult();
    }

    public void start(IFeedbackLogger logger) {
        if (!config.containsKey("wicket.model") || !config.containsKey(IValidationService.VALIDATE_ID)) {
            return;
        }

        this.logger = logger;

        context.registerService(this, config.getString(IValidationService.VALIDATE_ID));
    }

    public void stop() {
        context.unregisterService(this, config.getString(IValidationService.VALIDATE_ID));
    }

    public void validate() throws ValidationException {
        if (validated) {
            return;
        }
        IModel<Node> model = getModel();
        if (model == null || model.getObject() == null) {
            throw new ValidationException("No model found, skipping validation");
        }
        try {
            String nodeType = model.getObject().getPrimaryNodeType().getName();
            ITypeValidator validator;
            if (HippoNodeType.NT_TEMPLATETYPE.equals(nodeType)) {
                validator = new TemplateTypeValidator();
            } else {
                ITypeDescriptor descriptor = locator.locate(nodeType);
                ValidatorService validatorService = context.getService("field.validator.service", ValidatorService.class);
                validator = new JcrTypeValidator(descriptor, validatorService);
            }
            result.setViolations(validator.validate(model));
            List<IValidationListener> listeners = context.getServices(config.getString(IValidationService.VALIDATE_ID),
                    IValidationListener.class);
            for (IValidationListener listener : new ArrayList<IValidationListener>(listeners)) {
                listener.onValidation(result);
            }
            for (Violation violation : result.getViolations()) {
                logger.error(violation.getMessage());
            }
        } catch (RepositoryException e) {
            throw new ValidationException("Repository error", e);
        } catch (StoreException e) {
            throw new ValidationException("Could not construct validator", e);
        } finally {
            validated = true;
        }
    }

    public IValidationResult getValidationResult() {
        return result;
    }

    @SuppressWarnings("unchecked")
    private IModelReference<Node> getModelReference() {
        return context.getService(config.getString("wicket.model"), IModelReference.class);
    }

    public IModel<Node> getModel() {
        IModelReference<Node> modelRef = getModelReference();
        if (modelRef != null) {
            return modelRef.getModel();
        }
        return null;
    }

    public void detach() {
        locator.detach();
        result.detach();
        validated = false;
    }

}
