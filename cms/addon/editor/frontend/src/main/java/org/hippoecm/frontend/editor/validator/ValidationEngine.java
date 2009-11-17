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
package org.hippoecm.frontend.editor.validator;

import java.util.Iterator;

import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.editor.tools.JcrTypeStore;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.ObservableModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.types.BuiltinTypeStore;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.TypeLocator;
import org.hippoecm.frontend.validation.IValidateService;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.ValidationResult;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A validation engine that registers itself as an {@link IValidateService}.  All supported
 * validation rules are hardcoded.  Generic node types will be validated according to the
 * "required" and "non-empty" rules that apply to fields (see {@link JcrFieldValidator}).
 * The template type node type is validated by the {@link TemplateTypeValidator}.
 * <p>
 * Validation can be triggered by invoking the {@link IValidateService#validate()} method.
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
 * The model where the {@link IValidationResult} is made available.  Plugins that need to
 * change their appearance or functionality based on the validation status can observe the
 * model that is registered here.
 * </ul>
 */
public class ValidationEngine implements IValidateService {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ValidationEngine.class);

    private IPluginContext context;
    private IPluginConfig config;
    private ObservableModel resultModel;

    private ModelReference resultModelReference;
    private IObserver modelObserver;
    private TypeLocator locator;

    public ValidationEngine(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;

        if (!config.containsKey("wicket.model") || !config.containsKey("validator.model")) {
            log.warn("No wicket.model or validator.model configured for validation engine");
            return;
        }

        resultModel = new ObservableModel/*<IValidationResult>*/(new ValidationResult());

        IStore<ITypeDescriptor>[] stores = new IStore[2];
        stores[0] = new JcrTypeStore();
        stores[1] = new BuiltinTypeStore();
        locator = new TypeLocator(stores);
    }

    public void start() {
        if (!config.containsKey("wicket.model") || !config.containsKey("validator.model")
                || !config.containsKey(IValidateService.VALIDATE_ID)) {
            return;
        }

        final IModelReference wicketModelRef = context.getService(config.getString("wicket.model"),
                IModelReference.class);
        if (wicketModelRef != null) {
            context.registerService(modelObserver = new IObserver<IObservable>() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return wicketModelRef;
                }

                public void onEvent(Iterator<? extends IEvent<IObservable>> events) {
                    resultModel.setObject(new ValidationResult());
                }

            }, IObserver.class.getName());

            resultModelReference = new ModelReference(config.getString("validator.model"), resultModel);
            resultModelReference.init(context);

            context.registerService(this, config.getString(IValidateService.VALIDATE_ID));
        } else {
            log.warn("No model service found");
        }
    }

    public void stop() {
        if (modelObserver != null) {
            context.unregisterService(this, config.getString("validator.service"));

            resultModelReference.destroy();
            resultModelReference = null;

            context.unregisterService(modelObserver, IObserver.class.getName());
            modelObserver = null;
        }
    }

    public IValidationResult validate() throws ValidationException {
        IModel model = getModel();
        if (model == null) {
            throw new ValidationException("No model found, skipping validation");
        }
        try {
            String nodeType = ((JcrNodeModel) model).getNode().getPrimaryNodeType()
            .getName();
            ITypeValidator validator;
            if (HippoNodeType.NT_TEMPLATETYPE.equals(nodeType)) {
                validator = new TemplateTypeValidator();
            } else {
                ITypeDescriptor descriptor = locator.locate(nodeType);
                validator = new JcrTypeValidator(descriptor, locator);
            }
            ((ValidationResult) resultModel.getObject()).setViolations(validator.validate(model));
            resultModel.notifyObservers(new EventCollection());
            return (IValidationResult) resultModel.getObject();
        } catch (RepositoryException e) {
            throw new ValidationException("Repository error", e);
        } catch (StoreException e) {
            throw new ValidationException("Could not construct validator", e);
        }
    }

    private IModelReference getModelReference() {
        return context.getService(config.getString("wicket.model"), IModelReference.class);
    }

    private IModel getModel() {
        IModelReference modelRef = getModelReference();
        if (modelRef != null) {
            return modelRef.getModel();
        }
        return null;
    }

}
