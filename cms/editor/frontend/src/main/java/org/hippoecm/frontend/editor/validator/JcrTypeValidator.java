/*
 *  Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.JavaFieldDescriptor;
import org.hippoecm.frontend.validation.FeedbackScope;
import org.hippoecm.frontend.validation.ModelPathElement;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.hippoecm.frontend.validation.ViolationUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.services.validation.api.ValueContext;
import org.onehippo.cms.services.validation.api.internal.ValidationService;
import org.onehippo.cms.services.validation.api.internal.ValidatorInstance;
import org.onehippo.cms.services.validation.api.internal.ValueContextImpl;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator for generic jcr node types.
 */
public class JcrTypeValidator implements ITypeValidator {

    private static final Logger log = LoggerFactory.getLogger(JcrTypeValidator.class);

    private final Set<JcrFieldValidator> fieldValidators = new LinkedHashSet<>();
    private final IFieldDescriptor field;
    private final ITypeDescriptor type;
    private final ValidatorService validatorService;

    /**
     * @deprecated Use {@link JcrTypeValidator(IFieldDescriptor, ITypeDescriptor, ValidatorService)} instead
     */
    @Deprecated
    public JcrTypeValidator(final ITypeDescriptor type, final ValidatorService validatorService) throws StoreException {
        this(null, type, validatorService);
    }

    public JcrTypeValidator(final IFieldDescriptor field, final ITypeDescriptor type, final ValidatorService validatorService) throws StoreException {
        this.field = field;
        this.type = type;
        this.validatorService = validatorService;

        for (final IFieldDescriptor fieldDescriptor : type.getFields().values()) {
            fieldValidators.add(new JcrFieldValidator(fieldDescriptor, this));
        }
    }

    public ITypeDescriptor getType() {
        return type;
    }

    public ValidatorService getValidatorService() {
        return validatorService;
    }

    @Override
    public Set<Violation> validate(final IModel<Node> model) throws ValidationException {
        final Set<Violation> violations = new LinkedHashSet<>();
        validateFields(model, violations);
        validateType(model, violations);
        return violations;
    }

    private void validateFields(final IModel<Node> model, final Set<Violation> violations) throws ValidationException {
        final Set<Violation> fieldsViolations = new LinkedHashSet<>();
        for (final JcrFieldValidator fieldValidator : fieldValidators) {
            fieldsViolations.addAll(fieldValidator.validate(model));
        }

        if (type.isType(HippoNodeType.NT_COMPOUND)) {
            final Node node = model.getObject();
            String name = field.getPath();
            if ("*".equals(name)) {
                try {
                    name = node.getName();
                } catch (final RepositoryException e) {
                    throw new ValidationException("Could not resolve path for invalid value", e);
                }
            }
            final int index;
            try {
                index = node.getIndex() - 1;
            } catch (final RepositoryException e) {
                throw new ValidationException("Could not resolve path for invalid value", e);
            }

            final ModelPathElement modelPathElement = new ModelPathElement(field, name, index);
            ViolationUtils.prependModelPathElementToViolations(fieldsViolations, modelPathElement);
        }

        violations.addAll(fieldsViolations);
    }

    private void validateType(final IModel<Node> model, final Set<Violation> violations) {
        final Set<String> validators = type.getValidators();
        if (validators.isEmpty()) {
            return;
        }

        final ValidationService service = HippoServiceRegistry.getService(ValidationService.class);
        if (service == null) {
            log.error("Failed to get ValidationService, cannot validate type '{}'", type.getName());
            return;
        }

        final Node node = model.getObject();
        final ValueContext context;
        try {
            context = createTypeContext(node);
        } catch (RepositoryException e) {
            log.warn("Cannot create validation context for node '{}', cannot validate type '{}'",
                    JcrUtils.getNodePathQuietly(node), type.getName(), e);
            return;
        }

        for (String validatorName : validators) {
            final ValidatorInstance validator = service.getValidator(validatorName);
            if (validator == null) {
                log.warn("Ignoring unknown validator '{}'", validatorName);
            } else {
                validator.validate(context, node)
                           .map(violation -> this.convertViolation(violation, model))
                           .ifPresent(violations::add);
            }
        }
    }

    private ValueContext createTypeContext(final Node node) throws RepositoryException {
        final String jcrName = node.getName();
        final String jcrType = type.getType();
        final Node parent = node.getParent();
        final UserSession userSession = UserSession.get();
        final Locale locale = userSession.getLocale();
        final TimeZone timeZone = userSession.getTimeZone();

        return new ValueContextImpl(jcrName, jcrType, jcrType, node, parent, locale, timeZone);
    }

    private Violation convertViolation(org.onehippo.cms.services.validation.api.Violation violation, IModel<Node> model) {
        final Model<String> messageModel = Model.of(violation.getMessage());

        final boolean isCompound = type.isType(HippoNodeType.NT_COMPOUND);
        final FeedbackScope feedbackScope = isCompound ? FeedbackScope.COMPOUND : FeedbackScope.DOCUMENT;

        final Node node = model.getObject();

        try {
            final IFieldDescriptor fieldDescriptor = getFieldDescriptor(model);
            final JcrFieldValidator fieldValidator = new JcrFieldValidator(fieldDescriptor, this);
            return fieldValidator.newValueViolation(model, messageModel, feedbackScope);
        } catch (StoreException | ValidationException | RepositoryException e) {
            log.warn("Failed to create violation after validating node '{}'", JcrUtils.getNodePathQuietly(node), e);
            return null;
        }
    }

    /**
     * Can be removed once deprecated constructor is removed.
     */
    private IFieldDescriptor getFieldDescriptor(final IModel<Node> model) throws RepositoryException {
        if (field != null){
            return field;
        }
        final Node node = model.getObject();
        return new JavaFieldDescriptor(type, node.getName());
    }
}
