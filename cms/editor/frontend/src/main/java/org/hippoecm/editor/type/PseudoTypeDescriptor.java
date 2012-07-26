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
package org.hippoecm.editor.type;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;

public class PseudoTypeDescriptor implements ITypeDescriptor, IDetachable {

    private static final long serialVersionUID = 1L;

    private String name;
    private ITypeDescriptor upstream;
    private IObservationContext obContext;
    private IObserver observer;

    public PseudoTypeDescriptor(String name, ITypeDescriptor upstream) {
        this.name = name;
        this.upstream = upstream;
    }

    public String getName() {
        return name;
    }

    public void addField(IFieldDescriptor descriptor) {
        throw new UnsupportedOperationException();
    }

    public Map<String, IFieldDescriptor> getDeclaredFields() {
        return upstream.getDeclaredFields();
    }

    public IFieldDescriptor getField(String key) {
        return upstream.getField(key);
    }

    public Map<String, IFieldDescriptor> getFields() {
        return upstream.getFields();
    }

    public List<String> getSuperTypes() {
        return upstream.getSuperTypes();
    }

    public List<ITypeDescriptor> getSubTypes() {
        return upstream.getSubTypes();
    }

    public String getType() {
        return upstream.getType();
    }

    public boolean isMixin() {
        return upstream.isMixin();
    }

    public boolean isNode() {
        return upstream.isNode();
    }

    public boolean isType(String typeName) {
        return upstream.isType(typeName);
    }

    public boolean isValidationCascaded() {
        return upstream.isValidationCascaded();
    }

    public void removeField(String name) {
        throw new UnsupportedOperationException();
    }

    public void setIsMixin(boolean isMixin) {
        throw new UnsupportedOperationException();
    }

    public void setIsNode(boolean isNode) {
        throw new UnsupportedOperationException();
    }

    public void setPrimary(String name) {
        throw new UnsupportedOperationException();
    }

    public void setSuperTypes(List<String> superTypes) {
        throw new UnsupportedOperationException();
    }

    public void setIsValidationCascaded(boolean isCascaded) {
        throw new UnsupportedOperationException();
    }

    public void setObservationContext(final IObservationContext context) {
        obContext = context;
    }

    public void startObservation() {
        obContext.registerObserver(observer = new IObserver<ITypeDescriptor>() {
            private static final long serialVersionUID = 1L;

            public ITypeDescriptor getObservable() {
                return upstream;
            }

            public void onEvent(Iterator<? extends IEvent<ITypeDescriptor>> events) {
                obContext.notifyObservers(new EventCollection(events));
            }

        });
    }

    public void stopObservation() {
        obContext.unregisterObserver(observer);
        observer = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PseudoTypeDescriptor) {
            PseudoTypeDescriptor that = (PseudoTypeDescriptor) obj;
            return that.name.equals(name) && that.upstream.equals(upstream);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 317 ^ upstream.hashCode();
    }

    public void detach() {
        if (upstream instanceof IDetachable) {
            ((IDetachable) upstream).detach();
        }
    }

}
