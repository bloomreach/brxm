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
package org.hippoecm.frontend.types;

import java.util.List;
import java.util.Map;

import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;

public class MockTypeDescriptor implements ITypeDescriptor {
    
    private boolean node = false;
    
    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public List<String> getSuperTypes() {
        return null;
    }

    @Override
    public List<ITypeDescriptor> getSubTypes() {
        return null;
    }

    @Override
    public Map<String, IFieldDescriptor> getFields() {
        return null;
    }

    @Override
    public Map<String, IFieldDescriptor> getDeclaredFields() {
        return null;
    }

    @Override
    public IFieldDescriptor getField(final String key) {
        return null;
    }

    @Override
    public boolean isNode() {
        return node;
    }

    @Override
    public boolean isMixin() {
        return false;
    }

    @Override
    public boolean isType(final String typeName) {
        return false;
    }

    @Override
    public boolean isValidationCascaded() {
        return false;
    }

    @Override
    public void setSuperTypes(final List<String> superTypes) {

    }

    @Override
    public void addField(final IFieldDescriptor descriptor) throws TypeException {

    }

    @Override
    public void removeField(final String name) throws TypeException {

    }

    @Override
    public void setPrimary(final String name) {

    }

    @Override
    public void setIsNode(final boolean isNode) {
        this.node = isNode;
    }

    @Override
    public void setIsMixin(final boolean isMixin) {

    }

    @Override
    public void setIsValidationCascaded(final boolean isCascaded) {

    }

    @Override
    public void setObservationContext(final IObservationContext<? extends IObservable> context) {

    }

    @Override
    public void startObservation() {

    }

    @Override
    public void stopObservation() {

    }
}
