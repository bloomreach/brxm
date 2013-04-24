/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.services.contenttype;

import java.util.List;

/**
 * Represents a PropertyDefinition for its containing EffectiveNodeType
 * @see javax.jcr.nodetype.PropertyDefinition
 */
public interface EffectiveNodeTypeProperty extends EffectiveNodeTypeItem {

    /**
     * @return The required JCR type for the property, matching the {@link #getType} value for this property
     * @see javax.jcr.nodetype.PropertyDefinition#getRequiredType()
     */
    int getRequiredType();

    /**
     * @return The value(s) constraints for the property, if any. May be empty, never null.
     * @see javax.jcr.nodetype.PropertyDefinition#getValueConstraints()
     */
    List<String> getValueConstraints();

    /**
     * @return The default value(s) of the property, if any. May be empty, never null.
     * @see javax.jcr.nodetype.PropertyDefinition#getDefaultValues()
     */
    List<String> getDefaultValues();
}
