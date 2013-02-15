/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.search.jcr.service;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.services.search.jcr.HippoSearchNodeType;
import org.onehippo.cms7.services.search.query.QueryUtils;
import org.onehippo.cms7.services.search.query.constraint.AndConstraint;
import org.onehippo.cms7.services.search.query.constraint.Constraint;
import org.onehippo.cms7.services.search.query.constraint.DateConstraint;
import org.onehippo.cms7.services.search.query.constraint.ExistsConstraint;
import org.onehippo.cms7.services.search.query.constraint.NotConstraint;
import org.onehippo.cms7.services.search.query.constraint.OrConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JcrConstraintNode {

    static final Logger log = LoggerFactory.getLogger(JcrConstraintNode.class);

    public enum CompoundType {
        UNKNOWN, AND, OR
    }

    public enum ValueType {
        UNKNOWN, STRING, INTEGER, DATE
    }

    public enum RelationType {
        UNKNOWN(null), NON_NULL("nonnull"), EQUAL("equal"), CONTAINS("contains"), LESS_OR_EQUAL(
                "lessorequal"), GREATER_OR_EQUAL("greaterorequal"), BETWEEN("between");

        private final String name;

        RelationType(String name) {
            this.name = name;
        }

        String getName() {
            return name;
        }

        static RelationType fromString(String name) {
            for (RelationType type : RelationType.values()) {
                if (type == UNKNOWN) {
                    continue;
                }
                if (type.name.equals(name)) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }

    private final Node node;

    JcrConstraintNode(final Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    public CompoundType getType() {
        try {
            if (node.hasProperty(HippoSearchNodeType.TYPE)) {
                CompoundType.valueOf(node.getProperty(HippoSearchNodeType.TYPE).getString().toUpperCase());
            }
        } catch (RepositoryException re) {
            log.warn("Unable to read compound type", re);
        }
        return CompoundType.UNKNOWN;
    }

    public void setType(final CompoundType type) {
        try {
            node.setProperty(HippoSearchNodeType.TYPE, type.name().toLowerCase());
        } catch (RepositoryException re) {
            log.warn("Unable to set compound type", re);
        }
    }

    public String getProperty() {
        try {
            if (node.hasProperty(HippoSearchNodeType.PROPERTY)) {
                return node.getProperty(HippoSearchNodeType.PROPERTY).getString();
            }
        } catch (RepositoryException re) {
            log.warn("Unable to read property", re);
        }
        return null;
    }

    public ValueType getValueType() {
        try {
            if (node.hasProperty(HippoSearchNodeType.VALUE)) {
                switch (node.getProperty(HippoSearchNodeType.VALUE).getType()) {
                    case PropertyType.STRING:
                        return ValueType.STRING;
                    case PropertyType.LONG:
                        return ValueType.INTEGER;
                    case PropertyType.DATE:
                        return ValueType.DATE;
                }
            }
        } catch (RepositoryException re) {
            log.warn("Unable to obtain value type", re);
        }
        return ValueType.UNKNOWN;
    }

    public String getStringValue() {
        try {
            if (node.hasProperty(HippoSearchNodeType.VALUE)) {
                return node.getProperty(HippoSearchNodeType.VALUE).getString();
            }
        } catch (RepositoryException re) {
            log.warn("Unable to read value", re);
        }
        return null;
    }

    public int getIntValue() {
        try {
            if (node.hasProperty(HippoSearchNodeType.VALUE)) {
                return (int) node.getProperty(HippoSearchNodeType.VALUE).getLong();
            }
        } catch (RepositoryException re) {
            log.warn("Unable to read value", re);
        }
        return -1;
    }

    public Calendar getDateValue() {
        try {
            if (node.hasProperty(HippoSearchNodeType.VALUE)) {
                return node.getProperty(HippoSearchNodeType.VALUE).getDate();
            }
        } catch (RepositoryException re) {
            log.warn("Unable to read value", re);
        }
        return null;
    }

    public int getIntUpper() {
        try {
            if (node.hasProperty(HippoSearchNodeType.UPPER)) {
                return (int) node.getProperty(HippoSearchNodeType.UPPER).getLong();
            }
        } catch (RepositoryException re) {
            log.warn("Unable to read value", re);
        }
        return -1;
    }

    public Calendar getDateUpper() {
        try {
            if (node.hasProperty(HippoSearchNodeType.UPPER)) {
                return node.getProperty(HippoSearchNodeType.UPPER).getDate();
            }
        } catch (RepositoryException re) {
            log.warn("Unable to read value", re);
        }
        return null;
    }

    public RelationType getRelationType() {
        try {
            if (node.hasProperty(HippoSearchNodeType.RELATION)) {
                return RelationType.fromString(node.getProperty(HippoSearchNodeType.RELATION).getString());
            }
        } catch (RepositoryException re) {
            log.warn("Unable to read value", re);
        }
        return RelationType.UNKNOWN;
    }

    public void addNotNull(final String property) {
        if (setProperty(property)) {
            setRelation(RelationType.NON_NULL);
        }
    }

    public void addEqualTo(final String property, final int value) {
        if (setProperty(property) && setValue(value)) {
            setRelation(RelationType.EQUAL);
        }
    }

    public void addEqualTo(final String property, final String value) {
        if (setProperty(property) && setValue(value)) {
            setRelation(RelationType.EQUAL);
        }
    }

    public void addEqualTo(final String property, final Calendar value) {
        if (setProperty(property) && setValue(value)) {
            setRelation(RelationType.EQUAL);
        }
    }

    public void addContains(final String property, final String value) {
        boolean haveSetProperty = ".".equals(property) || setProperty(property);
        if (haveSetProperty && setValue(value)) {
            setRelation(RelationType.CONTAINS);
        }
    }

    public void addLessOrEqualThan(final String property, final int value) {
        if (setProperty(property) && setValue(value)) {
            setRelation(RelationType.LESS_OR_EQUAL);
        }
    }

    public void addLessOrEqualThan(final String property, final Calendar value) {
        if (setProperty(property) && setValue(value)) {
            setRelation(RelationType.LESS_OR_EQUAL);
        }
    }

    public void addGreaterOrEqualThan(final String property, final int value) {
        if (setProperty(property) && setValue(value)) {
            setRelation(RelationType.GREATER_OR_EQUAL);
        }
    }

    public void addGreaterOrEqualThan(final String property, final Calendar value) {
        if (setProperty(property) && setValue(value)) {
            setRelation(RelationType.GREATER_OR_EQUAL);
        }
    }

    public void addBetween(final String property, final int value, final int upper) {
        if (setProperty(property) && setValue(value) && setUpper(upper)) {
            setRelation(RelationType.BETWEEN);
        }
    }

    public void addBetween(final String property, final Calendar value, final Calendar upper) {
        if (setProperty(property) && setValue(value) && setUpper(upper)) {
            setRelation(RelationType.BETWEEN);
        }
    }

    public void negate() {
        try {
            node.setProperty(HippoSearchNodeType.NEGATE, true);
        } catch (RepositoryException re) {
            log.warn("Unable to set negate");
        }
    }

    public void setResolution(DateConstraint.Resolution resolution) {
        try {
            final String resolutionAsString = resolution.toString().toLowerCase();
            node.setProperty(HippoSearchNodeType.RESOLUTION, resolutionAsString);
        } catch (RepositoryException re) {
            log.warn("Unable to set resolution");
        }
    }

    public DateConstraint.Resolution getResolution() {
        try {
            if (node.hasProperty(HippoSearchNodeType.RESOLUTION)) {
                final String resolutionAsString = node.getProperty(HippoSearchNodeType.RESOLUTION).getString();
                return DateConstraint.Resolution.fromString(resolutionAsString);
            }
        } catch (RepositoryException re) {
            log.warn("Unable to retrieve resolution");
        }
        return DateConstraint.Resolution.DAY;
    }

    public Constraint getConstraint() {
        try {
            Constraint constraint;
            if (node.isNodeType(HippoSearchNodeType.NT_COMPOUNDCONSTRAINT)) {
                constraint = getCompoundConstraint();
            } else {
                constraint = getPrimitiveConstraint();
            }
            if (node.hasProperty(HippoSearchNodeType.NEGATE)) {
                if (node.getProperty(HippoSearchNodeType.NEGATE).getBoolean()) {
                    return new NotConstraint(constraint);
                }
            }
            return constraint;
        } catch (RepositoryException re) {
            log.warn("Unable to retrieve node type", re);
        }
        return null;
    }

    private Constraint getCompoundConstraint() {
        try {
            NodeIterable nodes = new NodeIterable(node.getNodes(HippoSearchNodeType.CONSTRAINT));
            switch (getType()) {
                case OR: {
                    OrConstraint orConstraint = null;
                    for (Node childNode : nodes) {
                        JcrConstraintNode constraintNode = new JcrConstraintNode(childNode);
                        Constraint constraint = constraintNode.getConstraint();
                        if (constraint == null) {
                            continue;
                        }
                        if (orConstraint == null) {
                            orConstraint = QueryUtils.either(constraint);
                        } else {
                            orConstraint = orConstraint.or(constraint);
                        }
                    }
                    return orConstraint;
                }
                case AND: {
                    AndConstraint andConstraint = null;
                    for (Node childNode : nodes) {
                        JcrConstraintNode constraintNode = new JcrConstraintNode(childNode);
                        Constraint constraint = constraintNode.getConstraint();
                        if (constraint == null) {
                            continue;
                        }
                        if (andConstraint == null) {
                            andConstraint = QueryUtils.both(constraint);
                        } else {
                            andConstraint = andConstraint.and(constraint);
                        }
                    }
                    return andConstraint;
                }
            }
        } catch (RepositoryException re) {
            log.warn("Unable to read compound constraint", re);
        }
        return null;
    }

    private Constraint getPrimitiveConstraint() {
        boolean hasProperty = getProperty() != null;
        DateConstraint.Resolution resolution = DateConstraint.Resolution.DAY;
        if (getValueType() == ValueType.DATE) {
            resolution = getResolution();
        }
        switch (getRelationType()) {
            case NON_NULL:
                return new ExistsConstraint(getProperty());

            case EQUAL:
                switch (getValueType()) {
                    case STRING:
                        if (hasProperty) {
                            return QueryUtils.text(getProperty()).isEqualTo(getStringValue());
                        } else {
                            return QueryUtils.text().isEqualTo(getStringValue());
                        }
                    case INTEGER:
                        return QueryUtils.integer(getProperty()).isEqualTo(getIntValue());
                    case DATE:
                        return QueryUtils.date(getProperty()).isEqualTo(getDateValue().getTime());
                }
                break;

            case CONTAINS:
                if (hasProperty) {
                    return QueryUtils.text(getProperty()).contains(getStringValue());
                } else {
                    return QueryUtils.text().contains(getStringValue());
                }

            case LESS_OR_EQUAL:
                switch (getValueType()) {
                    case INTEGER:
                        return QueryUtils.integer(getProperty()).to(getIntValue());
                    case DATE:
                        return QueryUtils.date(getProperty()).to(getDateValue().getTime(), resolution);
                }
                break;

            case GREATER_OR_EQUAL:
                switch (getValueType()) {
                    case INTEGER:
                        return QueryUtils.integer(getProperty()).from(getIntValue());
                    case DATE:
                        return QueryUtils.date(getProperty()).from(getDateValue().getTime(), resolution);
                }
                break;

            case BETWEEN:
                switch (getValueType()) {
                    case INTEGER:
                        return QueryUtils.integer(getProperty()).from(getIntValue()).andTo(getIntUpper());
                    case DATE:
                        return QueryUtils.date(getProperty()).from(getDateValue().getTime(), resolution).andTo(
                                getDateUpper().getTime());
                }
                break;
        }
        return null;
    }

    private boolean setProperty(String property) {
        try {
            node.setProperty(HippoSearchNodeType.PROPERTY, property);
            return true;
        } catch (RepositoryException re) {
            log.warn("Unable to set property");
            return false;
        }
    }

    private boolean setValue(int value) {
        try {
            node.setProperty(HippoSearchNodeType.VALUE, value);
            return true;
        } catch (RepositoryException re) {
            log.warn("Unable to set value");
            return false;
        }
    }

    private boolean setValue(String value) {
        try {
            node.setProperty(HippoSearchNodeType.VALUE, value);
            return true;
        } catch (RepositoryException re) {
            log.warn("Unable to set value");
            return false;
        }
    }

    private boolean setValue(Calendar value) {
        try {
            node.setProperty(HippoSearchNodeType.VALUE, value);
            return true;
        } catch (RepositoryException re) {
            log.warn("Unable to set value");
            return false;
        }
    }

    private boolean setUpper(int value) {
        try {
            node.setProperty(HippoSearchNodeType.UPPER, value);
            return true;
        } catch (RepositoryException re) {
            log.warn("Unable to set upper bound");
            return false;
        }
    }

    private boolean setUpper(Calendar value) {
        try {
            node.setProperty(HippoSearchNodeType.UPPER, value);
            return true;
        } catch (RepositoryException re) {
            log.warn("Unable to set upper bound");
            return false;
        }
    }

    private boolean setRelation(RelationType relation) {
        try {
            node.setProperty(HippoSearchNodeType.RELATION, relation.getName());
            return true;
        } catch (RepositoryException re) {
            log.warn("Unable to set relation");
            return false;
        }
    }

}
