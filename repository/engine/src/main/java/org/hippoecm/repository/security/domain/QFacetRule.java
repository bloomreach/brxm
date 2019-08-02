/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security.domain;

import java.io.Serializable;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.math3.util.Pair;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.SessionNamespaceResolver;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.FacetAuthConstants;
import org.onehippo.repository.security.domain.FacetRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.util.JcrUtils.getBooleanProperty;

/**
 * The facet rule consist of a facet (property) and a value.
 * The rule can be equals, the values MUST match, or NOT equal,
 * the values MUST NOT match and the property MUST exist. The
 * property can be of type String or of type {@link Name}.
 *
 * A special facet value is "nodetype" in which case the facet rule
 * matches if the node is of nodeType or is a subtype of nodetype or
 * has a mixin type of nodetype.
 */
public class QFacetRule implements Serializable {

    private final static Logger log = LoggerFactory.getLogger(QFacetRule.class);

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
    private String value;

    /**
     * In case this rule is a Reference Rule, this returns the absolute path to the reference
     */
    private String pathReference;

    /**
     * The Name representation of value to match the facet
     */
    private final Name valueName;

    /**
     * If the match is equals or not equals
     */
    private final boolean equals;

    /**
     * If the facet is optional or not
     */
    private final boolean optional;

    /**
     * The hash code
     */
    private transient int hash;


    public QFacetRule(FacetRule facetRule, NameResolver nameResolver) throws RepositoryException {
        // TODO this is used by Session delegation but DOES not work for PropertyType.REFERENCE !! FIX??
        this.type = facetRule.getType();
        this.facet = facetRule.getFacet();
        this.facetName = nameResolver.getQName(facet);
        this.value = facetRule.getValue();
        this.valueName = nameResolver.getQName(value);
        this.equals = facetRule.isEqual();
        this.optional = facetRule.isOptional();
    }

    /**
     * Create the facet rule based on the rule
     * @param node
     * @throws RepositoryException
     */
    public QFacetRule(final Node node) throws RepositoryException {
        // get mandatory properties
        facet = node.getProperty(HippoNodeType.HIPPO_FACET).getString();

        // Set the JCR Name for the facet (string)
        facetName = NameParser.parse(facet, new SessionNamespaceResolver(node.getSession()), NameFactoryImpl.getInstance());
        equals = node.getProperty(HippoNodeType.HIPPO_EQUALS).getBoolean();

        optional = getBooleanProperty(node, HippoNodeType.HIPPOSYS_FILTER, false);

        int tmpType = PropertyType.valueFromName(node.getProperty(HippoNodeType.HIPPOSYS_TYPE).getString());
        String tmpValue = node.getProperty(HippoNodeType.HIPPOSYS_VALUE).getString();

        //NameResolver nRes = new ParsingNameResolver(NameFactoryImpl.getInstance(), new SessionNamespaceResolver(node.getSession()));
        // if it's a name property set valueName
        Name tmpName = null;
        if (tmpType == PropertyType.NAME && !tmpValue.equals(FacetAuthConstants.WILDCARD)) {
            tmpName = NameParser.parse(tmpValue, new SessionNamespaceResolver(node.getSession()), NameFactoryImpl.getInstance());
        } else if (tmpType == PropertyType.REFERENCE) {
            // convert to a String matcher on UUID
            tmpType = PropertyType.STRING;
            final Pair<String, String> uuidAbsPath = parseReferenceTypeValue(node);
            tmpValue = uuidAbsPath.getFirst();
            pathReference = uuidAbsPath.getSecond();
        }

        // set final values
        type = tmpType;
        value = tmpValue;
        valueName = tmpName;

    }

    public boolean isHierarchicalWhiteListRule() {
        if (JcrConstants.JCR_PATH.equals(facet) && equals) {
            return true;
        }
        return false;
    }

    public boolean isReferenceRule() {
        return pathReference != null;
    }

    /**
     *
     */
    public boolean referenceExists() {
        if (!isReferenceRule()) {
            throw new UnsupportedOperationException("referenceExists check should only be checked when " +
                    "isReferenceRule() return true");
        }
        return value != null;
    }


    /**
     * Parse the facet rule of type Reference. Try to find the UUID of the
     * value of the QFacetRule.
     * @param facetNode the node of the QFacetRule
     * @return String the String representation of the UUID
     * @throws RepositoryException
     */
    private Pair<String, String> parseReferenceTypeValue(Node facetNode) throws RepositoryException {
        final String uuid;
        final String pathValue = facetNode.getProperty(HippoNodeType.HIPPOSYS_VALUE).getString();
        final String path = pathValue.startsWith("/") ? pathValue.substring(1) : pathValue;
        final String absPath = "/" + path;
        if ("".equals(path)) {
            uuid = facetNode.getSession().getRootNode().getIdentifier();
        } else {
            try {
                uuid = facetNode.getSession().getRootNode().getNode(path).getIdentifier();
            } catch (PathNotFoundException e) {

                log.info("Path not found for facetRule '{}'", facetNode.getPath());

                return new Pair<>(null, absPath);
            }
        }
        return new Pair<>(uuid, absPath);

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
     * A path reference gets translated to a UUID value, see {@link #parseReferenceTypeValue}. However, if the path
     * reference does not (yet) existed during initialization, it can be set later on. Then {@link #setUUIDReference(UUID)}
     * gets invoked (for all existing JCR Sessions containing this {@link QFacetRule}.
     * @param reference the UUID to set
     */
    public void setUUIDReference(final UUID reference) {
        if (reference == null) {
            value = null;
            return;
        }
        value = reference.toString();
    }

    /**
     * The value of the *absolute* path reference in case JcrConstants.JCR_PATH.equals(facet) and otherwise returns
     * {@code null}
     * @return the path reference
     */
    public String getPathReference() {
        return pathReference;
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
     * When the facet is optional, it does not need to be present on a node for the rule to match.
     * If it <strong>is</strong> present on the node, it's value must conform to the #isEqual and #getValue.
     * <p>
     * When the facet is not optional, the rule only matches when the facet is available on the node.
     *
     * @return true if the facet is optional
     */
    public boolean isFacetOptional() {
        return optional;
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
        StringBuilder sb = new StringBuilder();
        sb.append("QFacetRule");
        sb.append("(").append(PropertyType.nameFromValue(type)).append(")");
        sb.append("[");
        sb.append(facet);
        if (equals) {
            sb.append(" == ");
        } else {
            sb.append(" != ");
        }
        sb.append(value);
        if (pathReference != null) {
            sb.append(", pathReference = ").append(pathReference);
        }

        sb.append("]");

        return sb.toString();
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final QFacetRule that = (QFacetRule) o;

        if (type != that.type) return false;
        if (optional != that.optional) return false;
        if (equals != that.equals) return false;
        if (facet != null ? !facet.equals(that.facet) : that.facet != null) return false;
        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        if (hash != 0) {
            return hash;
        }
        int result = type;
        result = 31 * result + (facet != null ? facet.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (equals ? 1 : 0);
        result = 31 * result + (optional ? 1 : 0);
        hash = result;
        return hash;
    }
}
