/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.definition;

import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.Source;
import org.onehippo.cm.model.SourceType;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;

/**
 * Describes the type of data that a {@link Definition} contains, and therefore its textual syntax in YAML. The
 * lower-case form of the enum name matches the YAML map key used to create a definition of that type in a
 * {@link Source} of type {@link SourceType#CONFIG}. For example:
 * <pre>

definitions:
  config:
    /node:
      property: value
  namespace:
    prefix: hippostd
    uri:
    cnd: namespaces/hippostd.cnd
  webfilebundle: site

 * </pre>
 *
 */
public enum DefinitionType {

    /**
     * Describes a {@link Definition} that contains a representation of JCR Nodes of {@link ConfigurationItemCategory#CONFIG}.
     */
    CONFIG,

    /**
     * Describes a {@link Definition} that contains a representation of JCR Nodes of {@link ConfigurationItemCategory#CONTENT}.
     */
    CONTENT,

    /**
     * Describes a {@link Definition} that contains a representation of a JCR node type namespace and (potentially)
     * an associated CND resource defining JCR node types.
     */
    NAMESPACE,

    /**
     * Describes a {@link Definition} that contains a location of a Hippo CMS webfile bundle within the
     * containing {@link Module}.
     */
    WEBFILEBUNDLE;

    /**
     * An array of the string names for the definition types that are allowed in a {@link Source} of type
     * {@link SourceType#CONFIG}.
     */
	public static final String[] CONFIG_NAMES = {
			CONFIG.toString(),
			NAMESPACE.toString(),
			WEBFILEBUNDLE.toString()
	};

    /**
     * @param definition a Definition whose type we want to check
     * @return true iff {@link Definition#getType()} == this
     */
    public final boolean isOfType(final Definition definition) {
        return this == definition.getType();
    }

    public final String toString() {
        return name().toLowerCase();
    }

}
