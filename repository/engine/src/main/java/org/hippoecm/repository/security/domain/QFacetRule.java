/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
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

import static org.hippoecm.repository.api.HippoNodeType.NT_FEDERATEDDOMAINFOLDER;
import static org.hippoecm.repository.security.domain.QFacetRule.FacetRuleType.JCR_PATH;
import static org.hippoecm.repository.security.domain.QFacetRule.FacetRuleType.JCR_PRIMARYTYPE;
import static org.hippoecm.repository.security.domain.QFacetRule.FacetRuleType.JCR_UUID;
import static org.hippoecm.repository.security.domain.QFacetRule.FacetRuleType.NODENAME;
import static org.hippoecm.repository.security.domain.QFacetRule.FacetRuleType.NODETYPE;
import static org.hippoecm.repository.security.domain.QFacetRule.FacetRuleType.OTHER;
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
     * If this rule is a Reference Rule
     */
    private final boolean referenceRule;

    /**
     * In case this rule is a Reference Rule the UUID of the rule is needed to synchronize changes to the reference itself
     */
    private final String facetUUID;

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

    private final FacetRuleType facetRuleType;

    public enum FacetRuleType {
        NODETYPE,
        NODENAME,
        JCR_UUID,
        JCR_PATH,
        JCR_PRIMARYTYPE,
        OTHER
    }

    public QFacetRule(FacetRule facetRule, NameResolver nameResolver) throws RepositoryException {
        // TODO this is used by Session delegation but DOES not work for PropertyType.REFERENCE !! FIX??
        this.referenceRule = false;
        this.facetUUID = null;
        this.type = facetRule.getType();
        this.facet = facetRule.getFacet();
        facetRuleType = getFacetRuleType(facet);
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

        facetRuleType = getFacetRuleType(facet);

        facetUUID = node.getIdentifier();

        // Set the JCR Name for the facet (string)
        facetName = NameParser.parse(facet, new SessionNamespaceResolver(node.getSession()), NameFactoryImpl.getInstance());
        equals = node.getProperty(HippoNodeType.HIPPO_EQUALS).getBoolean();

        optional = getBooleanProperty(node, HippoNodeType.HIPPOSYS_FILTER, false);

        int tmpType = PropertyType.valueFromName(node.getProperty(HippoNodeType.HIPPOSYS_TYPE).getString());
        String tmpValue = node.getProperty(HippoNodeType.HIPPOSYS_VALUE).getString();

        Name tmpName = null;

        referenceRule = tmpType == PropertyType.REFERENCE;
        if (referenceRule) {
            // convert to a String matcher on UUID
            tmpType = PropertyType.STRING;
            tmpValue = parseReferenceTypeValue(node);
        } else if (tmpType == PropertyType.NAME && !tmpValue.equals(FacetAuthConstants.WILDCARD)) {
            tmpName = NameParser.parse(tmpValue, new SessionNamespaceResolver(node.getSession()), NameFactoryImpl.getInstance());
        }

        // set final values
        type = tmpType;
        value = tmpValue;
        valueName = tmpName;

    }

    private FacetRuleType getFacetRuleType(final String facet) {
        if (facet.equalsIgnoreCase("nodetype")) {
            return NODETYPE;
        } else if (facet.equalsIgnoreCase("nodename")) {
            return NODENAME;
        } else if  (facet.equalsIgnoreCase("jcr:uuid")) {
            return JCR_UUID;
        } else if (facet.equalsIgnoreCase("jcr:path")) {
            return JCR_PATH;
        } else if (facet.equalsIgnoreCase("jcr:primaryType")) {
            return JCR_PRIMARYTYPE;
        } else {
            return  OTHER;
        }
    }

    public boolean isHierarchicalAllowlistRule() {
        if (JcrConstants.JCR_PATH.equals(facet) && equals) {
            return true;
        }
        return false;
    }

    public boolean isReferenceRule() {
        return referenceRule;
    }

    public FacetRuleType getFacetRuleType() {
        return facetRuleType;
    }

    /**
     *
     */
    public boolean referenceExists() {
        if (!isReferenceRule()) {
            throw new UnsupportedOperationException("referenceExists check should only be checked when " +
                    "isReferenceRule() return true");
        }
        return StringUtils.isNotEmpty(value);
    }


    /**
     * Parse the facet rule of type Reference. Try to find the UUID of the
     * value of the QFacetRule.
     * @param facetNode the node of the QFacetRule
     * @return String the String representation of the UUID
     * @throws RepositoryException
     */
    private String parseReferenceTypeValue(final Node facetNode) throws RepositoryException {
        final String fullyQualifiedPath  = getFullyQualifiedPath(facetNode);
        try {
            return facetNode.getSession().getNode(fullyQualifiedPath).getIdentifier();
        } catch (PathNotFoundException e) {
            log.info("Path not found for facetRule '{}'", facetNode.getPath());
            return StringUtils.EMPTY;
        }
    }

    /**
     * Facet rules located below a federated domain have as 'root scope' the parent of the domain folder: federated
     * domains have sandboxed values for the jcr:path/jcr:uuid
     * @param facetNode the node of type {@link HippoNodeType#NT_FACETRULE}
     * @return the fully qualified path starting with '/'
     */
    public static String getFullyQualifiedPath(final Node facetNode) throws RepositoryException {

        final Node domainsFolder = facetNode.getParent().getParent().getParent();
        final String pathValue = facetNode.getProperty(HippoNodeType.HIPPOSYS_VALUE).getString();
        if (domainsFolder.isNodeType(NT_FEDERATEDDOMAINFOLDER)) {
            if (pathValue.startsWith("/")) {
                return domainsFolder.getParent().getPath() + (pathValue.equals("/") ? "" : pathValue);
            }
            return domainsFolder.getParent().getPath() + "/" + pathValue;
        } else {
            if (pathValue.startsWith("/")) {
                return pathValue;
            }
            return "/" + pathValue;
        }
    }

    /**
     * Get the string representation of the facet
     * @return the facet
     */
    public String getFacet() {
        return facet;
    }

    /**
     * Get the UUID of the facet rule itself
     * @return the facet rule UUID
     */
    public String getFacetUUID() {
        return facetUUID;
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
     * reference does not (yet) existed during initialization, it can be set later on. Then {@link #setUUIDValue(String)}
     * gets invoked (for all existing JCR Sessions containing this {@link QFacetRule}.
     * @param value the UUID to set
     * @throws UnsupportedOperationException when not a {{@link #isReferenceRule()}}
     */
    public void setUUIDValue(final String value) {
        if (!isReferenceRule()) {
            throw new UnsupportedOperationException("Setting the UUID Value is only supported when " +
                    "isReferenceRule() return true");
        }
        this.value = value;
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
