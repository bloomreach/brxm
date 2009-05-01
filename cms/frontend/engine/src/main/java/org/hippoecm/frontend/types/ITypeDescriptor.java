/*
 *  Copyright 2008 Hippo.
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

import java.util.EventListener;
import java.util.List;
import java.util.Map;

import javax.jcr.Value;

import org.apache.wicket.IClusterable;

public interface ITypeDescriptor extends IClusterable {
    final static String SVN_ID = "$Id$";

    interface ITypeListener extends EventListener, IClusterable {

        void fieldAdded(String field);

        void fieldChanged(String field);

        void fieldRemoved(String field);
    }

    String getName();

    String getType();

    List<String> getSuperTypes();

    void setSuperTypes(List<String> superTypes);

    Map<String, IFieldDescriptor> getFields();

    IFieldDescriptor getField(String key);

    void addField(IFieldDescriptor descriptor);

    void removeField(String name);

    void setPrimary(String name);

    boolean isNode();

    void setIsNode(boolean isNode);

    boolean isMixin();

    void setIsMixin(boolean isMixin);

    Value createValue();

    void addTypeListener(ITypeListener listener);

    void removeTypeListener(ITypeListener listener);
    
}
