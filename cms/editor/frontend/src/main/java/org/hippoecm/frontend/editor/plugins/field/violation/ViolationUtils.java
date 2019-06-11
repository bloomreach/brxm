/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.field.violation;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.validation.FeedbackScope;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ModelPath;
import org.hippoecm.frontend.validation.ModelPathElement;
import org.hippoecm.frontend.validation.Violation;
import org.hippoecm.repository.api.HippoNodeType;

public class ViolationUtils {

    private ViolationUtils() {}

    public static Optional<ViolationMessage> getFirstFieldViolation(final IFieldDescriptor field,
                                                                    final IModel<IValidationResult> validationModel) {

        // show no validation messages for content blocks
        if (field != null && field.getTypeDescriptor().isType(HippoNodeType.NT_COMPOUND)) {
            return Optional.empty();
        }

        return getViolationMessages(field, validationModel, FeedbackScope.FIELD)
                .findFirst();
    }

    public static Stream<ViolationMessage> getViolationPerCompound(final IFieldDescriptor field,
                                                                   final IModel<IValidationResult> validationModel) {

        return getViolationMessages(field, validationModel, FeedbackScope.COMPOUND)
                .filter(distinctByKey(ViolationMessage::getIndex));
    }

    private static Stream<ViolationMessage> getViolationMessages(final IFieldDescriptor field,
                                                                 final IModel<IValidationResult> validationModel,
                                                                 final FeedbackScope scope) {
        if (field == null) {
            return Stream.empty();
        }

        if (validationModel == null) {
            return Stream.empty();
        }

        final IValidationResult validationResult = validationModel.getObject();
        if (validationResult == null || validationResult.isValid()) {
            return Stream.empty();
        }

        final Set<Violation> violations = validationResult.getViolations();
        if (violations == null || violations.isEmpty()) {
            return Stream.empty();
        }

        return violations.stream()
                .filter(violation -> violation.getFeedbackScope().equals(scope))
                .map(violation -> toViolationMessage(field, violation))
                .filter(Objects::nonNull);
    }

    private static <T> Predicate<T> distinctByKey(final Function<? super T, ?> keyExtractor) {
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private static ViolationMessage toViolationMessage(final IFieldDescriptor field, final Violation violation) {
        final Set<ModelPath> dependentPaths = violation.getDependentPaths();
        for (final ModelPath path : dependentPaths) {
            if (path.getElements().length > 0) {
                final ModelPathElement first = path.getElements()[0];
                if (first.getField().equals(field)) {
                    final String message = violation.getMessage().getObject();
                    final int index = first.getIndex();
                    return new ViolationMessage(message, index);
                }
            }
        }
        return null;
    }

    public static class ViolationMessage {

        private final String message;
        private final int index;

        ViolationMessage(final String message, final int index) {
            this.message = message;
            this.index = index;
        }

        public String getMessage() {
            return message;
        }

        public int getIndex() {
            return index;
        }
    }
}
