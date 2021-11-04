/*
 * Copyright 2021 Bloomreach Inc. (www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public final class SvgValidationResult {

    private final Set<String> offendingElements;
    private final Set<String> offendingAttributes;

    SvgValidationResult(Set<String> offendingElements, Set<String> offendingAttributes) {
        this.offendingElements = offendingElements;
        this.offendingAttributes = offendingAttributes;
    }

    public static SvgValidationResultBuilder builder() {
        return new SvgValidationResultBuilder();
    }

    public boolean isValid(){
        return offendingAttributes.isEmpty() && offendingElements.isEmpty();
    }

    public Set<String> getOffendingElements() {
        return this.offendingElements;
    }

    public Set<String> getOffendingAttributes() {
        return this.offendingAttributes;
    }

    public static class SvgValidationResultBuilder {
        private ArrayList<String> offendingElements;
        private ArrayList<String> offendingAttributes;

        SvgValidationResultBuilder() {
        }

        public org.hippoecm.frontend.validation.SvgValidationResult.SvgValidationResultBuilder offendingElement(
                String offendingElement) {
            if (this.offendingElements == null) {
                this.offendingElements = new ArrayList<String>();
            }
            this.offendingElements.add(offendingElement);
            return this;
        }

        public org.hippoecm.frontend.validation.SvgValidationResult.SvgValidationResultBuilder offendingElements(
                Collection<? extends String> offendingElements) {
            if (this.offendingElements == null) {
                this.offendingElements = new ArrayList<String>();
            }
            this.offendingElements.addAll(offendingElements);
            return this;
        }

        public org.hippoecm.frontend.validation.SvgValidationResult.SvgValidationResultBuilder clearOffendingElements() {
            if (this.offendingElements != null) {
                this.offendingElements.clear();
            }
            return this;
        }

        public org.hippoecm.frontend.validation.SvgValidationResult.SvgValidationResultBuilder offendingAttribute(
                String offendingAttribute) {
            if (this.offendingAttributes == null) {
                this.offendingAttributes = new ArrayList<String>();
            }
            this.offendingAttributes.add(offendingAttribute);
            return this;
        }

        public org.hippoecm.frontend.validation.SvgValidationResult.SvgValidationResultBuilder offendingAttributes(
                Collection<? extends String> offendingAttributes) {
            if (this.offendingAttributes == null) {
                this.offendingAttributes = new ArrayList<String>();
            }
            this.offendingAttributes.addAll(offendingAttributes);
            return this;
        }

        public org.hippoecm.frontend.validation.SvgValidationResult.SvgValidationResultBuilder clearOffendingAttributes() {
            if (this.offendingAttributes != null) {
                this.offendingAttributes.clear();
            }
            return this;
        }

        public org.hippoecm.frontend.validation.SvgValidationResult build() {
            Set<String> offendingElements;
            switch (this.offendingElements == null ? 0 : this.offendingElements.size()) {
                case 0:
                    offendingElements = java.util.Collections.emptySet();
                    break;
                case 1:
                    offendingElements = java.util.Collections.singleton(this.offendingElements.get(0));
                    break;
                default:
                    offendingElements = new java.util.LinkedHashSet<String>(
                            this.offendingElements.size() < 1073741824 ? 1 + this.offendingElements.size() + (this.offendingElements.size() - 3) / 3 : Integer.MAX_VALUE);
                    offendingElements.addAll(this.offendingElements);
                    offendingElements = java.util.Collections.unmodifiableSet(offendingElements);
            }
            Set<String> offendingAttributes;
            switch (this.offendingAttributes == null ? 0 : this.offendingAttributes.size()) {
                case 0:
                    offendingAttributes = java.util.Collections.emptySet();
                    break;
                case 1:
                    offendingAttributes = java.util.Collections.singleton(this.offendingAttributes.get(0));
                    break;
                default:
                    offendingAttributes = new java.util.LinkedHashSet<String>(
                            this.offendingAttributes.size() < 1073741824 ? 1 + this.offendingAttributes.size() + (this.offendingAttributes.size() - 3) / 3 : Integer.MAX_VALUE);
                    offendingAttributes.addAll(this.offendingAttributes);
                    offendingAttributes = java.util.Collections.unmodifiableSet(offendingAttributes);
            }

            return new SvgValidationResult(offendingElements, offendingAttributes);
        }

    }
}
