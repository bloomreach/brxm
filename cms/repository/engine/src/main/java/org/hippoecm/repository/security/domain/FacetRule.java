/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.security.domain;

import java.io.Serializable;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.FacetAuthHelper;

/**
 * The facet rule consist of a facet (property) and a value. 
 * The rule can be equals, the values MUST match, or NOT equal,
 * the values MUST NOT match and the propery MUST exist. The 
 * property can be of type String or of type {@link Name}.
 * 
 * A special facet value is "nodetype" in which case the facet rule
 * matches if the node is of nodeType or is a subtype of nodetype or 
 * has a mixin type of nodetype.
 */
public class FacetRule implements Serializable {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    /**
     * Serial version id
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * The property type: PropertyType.STRING or PropertyType.NAME
     * @see PropertyType
     */
    private final int type;

    /**
     * The facet (property)
     */
    private final String facet;
    
    /**
     * The Name representation of the facet
     */
    private final Name facetName;
    
    /**
     * The value to match the facet
     */
    private final String value;

    /**
     * The Name representation of value to match the facet
     */
    private final Name valueName;
        
    /**
     * If the match is equals or not equals
     */
    private final boolean equals;

    /**
     * The hash code
     */
    private transient int hash;

    /**
     * Create the facet rule based on the rule
     * @param node
     * @throws RepositoryException
     */
    public FacetRule(final Node node) throws RepositoryException {
        if (node == null) {
            throw new IllegalArgumentException("FacetRule node cannot be null");
        }
        // all properties are mandatory
        facet = node.getProperty(HippoNodeType.HIPPO_FACET).getString();
        equals = node.getProperty(HippoNodeType.HIPPO_EQUALS).getBoolean();
        type = PropertyType.valueFromName(node.getProperty(HippoNodeType.HIPPO_TYPE).getString());
        value = node.getProperty(HippoNodeType.HIPPO_VALUE).getString();

        // catch some illegal options
        if (value.equals(FacetAuthHelper.WILDCARD) && !equals) {
            throw new RepositoryException("Not-equals and wildcard value not allowed together.");
        }
        
        // if it's a name property set valueName
        Name name = null;
        if (type == PropertyType.NAME && !value.equals(FacetAuthHelper.WILDCARD)) {
            name = FacetAuthHelper.getNameFromJCRName(node.getSession(), value);
        }
        valueName = name;
        
        // Set the JCR Name for the facet (string)
        facetName = FacetAuthHelper.getNameFromJCRName(node.getSession(), facet);
    }

    /**
     * Get the string representation of the facet
     * @return the facet
     */
    public String getFacet() {
        return facet;
    }
    
    /**
     * Get the name representation of the facet
     * @return the facet name
     * @see Name
     */
    public Name getFacetName() {
        return facetName;
    }

    /**
     * The value of the facet rule to match
     * @return the value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * The Name of the value of the facet rule to match
     * @return the value name if the type is Name else null
     */
    public Name getValueName() {
        return valueName;
    }
    /**
     * Check for equality or inequality
     * @return true if to rule has to check for equality
     */
    public boolean isEqual() {
        return equals;
    }

    /**
     * Get the PropertyType of the facet
     * @return the type
     */
    public int getType() {
        return type;
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString() {
        String eq = isEqual() ? "==" : "!=";
        return "FacetRule [" + facet + " " + eq + " " + value + "]";
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FacetRule)) {
            return false;
        }
        FacetRule other = (FacetRule) obj;
        return facet.equals(other.getFacet()) && value.equals(other.getValue()) && (equals == other.isEqual());
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        if (hash == 0) {
            hash = this.toString().hashCode();
        }
        return hash;
    }
}
