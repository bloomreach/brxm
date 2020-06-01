/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.frontend.model.event.IEvent;

public class TypeDescriptorEvent implements IEvent<ITypeDescriptor> {

    public enum EventType {
        FIELD_ADDED, FIELD_CHANGED, FIELD_REMOVED
    }

    private ITypeDescriptor descriptor;
    private IFieldDescriptor field;
    private EventType type;

    public TypeDescriptorEvent(ITypeDescriptor descriptor, IFieldDescriptor field, EventType type) {
        this.descriptor = descriptor;
        this.field = field;
        this.type = type;
    }

    public EventType getType() {
        return type;
    }

    public IFieldDescriptor getField() {
        return field;
    }

    public ITypeDescriptor getSource() {
        return descriptor;
    }

}
