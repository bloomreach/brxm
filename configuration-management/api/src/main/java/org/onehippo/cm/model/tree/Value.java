/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.tree;

import java.io.IOException;
import java.io.InputStream;

import org.onehippo.cm.model.definition.Definition;
import org.onehippo.cm.model.source.ResourceInputProvider;

/**
 * Represents a (potential) single JCR Value.
 */
public interface Value {
    /**
     * @return the representation of this Value that is most appropriate for its type
     */
    Object getObject();

    /**
     * @return a String representation of this Value, corresponding to the JCR method {@link javax.jcr.Value#getString()}
     */
    String getString();

    /**
     * @return the type of this Value, corresponding to the JCR {@link javax.jcr.Value#getType()}
     */
    ValueType getType();

    /**
     * The Property for which this object provides the value. NOTE: When Value is used in the context of a
     * NamespaceDefinition, this will return null.
     * @return the DefinitionProperty to which this Value belongs, or null in case this Value is used in a NamespaceDefinition
     */
    DefinitionProperty<?> getParent();

    /**
     * @return the overall Definition to which this Value contributes, which may be a ContentDefinition or a NamespaceDefinition
     */
    Definition<?> getDefinition();

    /**
     * Returns whether this value represents a path to a resource rather than the actual value, only applicable for
     * {@link ValueType#STRING} and {@link ValueType#BINARY}
     * @return
     */
    boolean isResource();

    /**
     * When this {@link #isResource() resource} value represents a (possible) new resource, the resource
     * path provided through {@link #getString()} must be considered a <em>candidate</em> resource path, possibly clashing
     * with existing resource paths. The actual resource path needs to be uniquified before serialization.
     * @return true when this {@link #isResource() resource} its {@link #getString() path} must be treated as
     * a <em>candidate</em> path only.
     */
    boolean isNewResource();

    /**
     * Provides access to the ResourceInputProvider backing this value -- can be used to compare src and dest when
     * writing.
     */
    ResourceInputProvider getResourceInputProvider();

    /**
     * Uses a ResourceInputProvider to create an InputStream for this Value's content.
     * @return an InputStream that must be closed when the caller is finished using it
     * @throws IOException if there is any problem in creating the InputStream
     * @throws IllegalStateException iff (this.isResource() == false)
     */
    InputStream getResourceInputStream() throws IOException;

    /**
     * Returns whether this value represents a path to an item in the repository rather than its UUID, only applicable
     * for {@link ValueType#REFERENCE} and {@link ValueType#WEAKREFERENCE}
     * @return
     */
    boolean isPath();
}
