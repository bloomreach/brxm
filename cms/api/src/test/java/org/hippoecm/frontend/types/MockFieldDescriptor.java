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

import java.util.Objects;
import java.util.Set;

import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;

public class MockFieldDescriptor implements IFieldDescriptor {
    
    private ITypeDescriptor iTypeDescriptor = new MockTypeDescriptor();
    private String name;

    public MockFieldDescriptor(final String name) {
        this.name = name;
    }

    public void setMockTypeDescriptor(final ITypeDescriptor typeDescriptor) {
        this.iTypeDescriptor = typeDescriptor;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MockFieldDescriptor that = (MockFieldDescriptor) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public ITypeDescriptor getTypeDescriptor() {
        return iTypeDescriptor;
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public boolean isMultiple() {
        return false;
    }

    @Override
    public boolean isAutoCreated() {
        return false;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Override
    public boolean isMandatory() {
        return false;
    }

    @Override
    public boolean isOrdered() {
        return false;
    }

    @Override
    public boolean isPrimary() {
        return false;
    }

    @Override
    public Set<String> getExcluded() {
        return null;
    }

    @Override
    public Set<String> getValidators() {
        return null;
    }

    @Override
    public void setPath(final String path) throws TypeException {

    }

    @Override
    public void setMultiple(final boolean multiple) {

    }

    @Override
    public void setAutoCreated(final boolean autocreated) {

    }

    @Override
    public void setMandatory(final boolean mandatory) {

    }

    @Override
    public void setOrdered(final boolean isOrdered) {

    }

    @Override
    public void addValidator(final String validator) {

    }

    @Override
    public void setExcluded(final Set<String> set) {

    }

    @Override
    public void removeValidator(final String validator) {

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
