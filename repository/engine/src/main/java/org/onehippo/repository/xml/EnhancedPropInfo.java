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
package org.onehippo.repository.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.xml.Importer;
import org.apache.jackrabbit.core.xml.PropInfo;
import org.apache.jackrabbit.core.xml.TextValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PATHS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_RELATED;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.APPEND;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.INSERT;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.OVERRIDE;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.SKIP;

/**
 * Information about a property being imported. This class is used
 * by the XML import handlers to pass the parsed property information
 * through the {@link Importer} interface to the actual import process.
 * <p>
 * In addition to carrying the actual property data, instances of this
 * class also know how to apply that data when imported either to a
 * {@link NodeImpl} instance through a session or directly to a
 * {@link NodeState} instance in a workspace.
 */
public class EnhancedPropInfo extends PropInfo {

    private static Logger log = LoggerFactory.getLogger(EnhancedPropInfo.class);

    private final NamePathResolver resolver;
    private final Name name;
    private final int type;
    private final Boolean multiple;
    private boolean isPathReference = false;
    private String mergeBehavior = null;
    private String mergeLocation = null;
    private final TextValue[] values;
    private final URL[] binaryURLs;
    private final ValueFactory valueFactory;

    EnhancedPropInfo(TextValue[] values, String mergeBehavior, String mergeLocation, boolean multiple) {
        super(null, -1, values);
        this.mergeBehavior = mergeBehavior;
        this.mergeLocation = mergeLocation;
        this.values = values;
        this.multiple = multiple;
        resolver = null;
        name = null;
        type = -1;
        binaryURLs = null;
        valueFactory = null;
    }

    public EnhancedPropInfo(NamePathResolver resolver, Name name, int type, Boolean multiple, TextValue[] values, String mergeBehavior,
                            String mergeLocation, final URL[] binaryURLs, final ValueFactory valueFactory, final ImportContext importContext) {
        super(name, type, values);
        this.multiple = multiple != null ? multiple : Boolean.FALSE;
        this.mergeBehavior = mergeBehavior;
        this.mergeLocation = mergeLocation;
        if (name.getLocalName().endsWith(Reference.REFERENCE_SUFFIX)) {
            String localName = StringUtils.substringBefore(name.getLocalName(), Reference.REFERENCE_SUFFIX);
            this.name =  NameFactoryImpl.getInstance().create(name.getNamespaceURI(), localName);
            this.isPathReference = true;
            this.type = PropertyType.STRING;
        } else {
            this.name = name;
            this.type = type;
        }
        this.values = values.clone();
        this.resolver = resolver;
        this.binaryURLs = binaryURLs;
        this.valueFactory = valueFactory;
    }

    private boolean mergeOverride() {
        return OVERRIDE.equalsIgnoreCase(mergeBehavior);
    }

    private boolean mergeAppend() {
        return APPEND.equalsIgnoreCase(mergeBehavior);
    }

    private boolean mergeInsert() {
        return INSERT.equalsIgnoreCase(mergeBehavior);
    }

    private boolean mergeCombine() {
        return mergeAppend() || mergeInsert();
    }

    private boolean mergeSkip() {
        return SKIP.equalsIgnoreCase(mergeBehavior);
    }

    private int mergeLocation(Value[] values) {
        if (mergeAppend()) {
            return values.length;
        } else if (mergeInsert()) {
            if (StringUtils.isEmpty(mergeLocation)) {
                return 0;
            } else {
                return Integer.parseInt(mergeLocation);
            }
        } else {
            return 0;
        }
    }

    @Override
    public void dispose() {
        for (final TextValue value : values) {
            value.dispose();
        }
    }

    @Override
    public int getTargetType(QPropertyDefinition def) {
        int target = def.getRequiredType();
        if (target != PropertyType.UNDEFINED) {
            return target;
        } else if (type != PropertyType.UNDEFINED) {
            return type;
        } else {
            return PropertyType.STRING;
        }
    }

    @Override
    public Value[] getValues(final int targetType, final NamePathResolver resolver) throws RepositoryException {
        if (targetType == PropertyType.BINARY && binaryURLs.length != 0) {
            Value[] values = new Value[binaryURLs.length];
            try {
                for (int i = 0; i < binaryURLs.length; i++) {
                    values[i] = valueFactory.createValue(valueFactory.createBinary(binaryURLs[i].openStream()));
                }
                return values;
            } catch (IOException e) {
                for (Value value : values) {
                    if (value != null) {
                        try {
                            Binary binary = value.getBinary();
                            binary.dispose();
                        } catch (Exception ignore) {
                        }
                    }
                }
                throw new RepositoryException(e);
            }
        } else if (targetType == PropertyType.STRING && binaryURLs.length != 0) {
            Value[] values = new Value[binaryURLs.length];
            InputStream input = null;
            try {
                for (int i = 0; i < binaryURLs.length; i++) {
                    input = binaryURLs[i].openStream();
                    values[i] = valueFactory.createValue(IOUtils.toString(input, "UTF-8"));
                    input.close();
                    input = null;
                }
                return values;
            } catch (IOException e) {
                throw new RepositoryException(e);
            } finally {
                IOUtils.closeQuietly(input);
            }
        } else {
            Value[] va = new Value[values.length];
            for (int i = 0; i < values.length; i++) {
                if (isPathReference) {
                    // the string value is needed, but the target type is reference
                    va[i] = values[i].getValue(PropertyType.STRING, resolver);
                } else {
                    va[i] = values[i].getValue(targetType, resolver);
                }
            }
            return va;
        }
    }

    @Override
    public QPropertyDefinition getApplicablePropertyDef(EffectiveNodeType ent)
            throws ConstraintViolationException {

        // The eventual target type has to be checked not the current in between type.
        // This is relevant for dereferenced Reference's, because they are exported as String's.
        int checkType = type;
        if (isPathReference) {
            checkType = PropertyType.REFERENCE;
        }
        if (values.length == 1 || binaryURLs.length == 1) {
            // could be single- or multi-valued (n == 1)
            return ent.getApplicablePropertyDef(name, checkType);
        } else {
            // can only be multi-valued (n == 0 || n > 1)
            return ent.getApplicablePropertyDef(name, checkType, true);
        }
    }

    public void apply(NodeImpl node, NamePathResolver resolver, Map<NodeId, List<Reference>> derefNodes)
            throws RepositoryException {

        // find applicable definition
        QPropertyDefinition def = getApplicablePropertyDef(node.getEffectiveNodeType());
        if (def.isProtected()) {
            // skip protected property
            log.debug("skipping protected property {}", name);
            return;
        }
        if (isGeneratedProperty(name)) {
            // skip auto-generated property, let the repository handle recreation
            log.debug("skipping auto-generated property {}", name);
            return;
        }

        // convert serialized values to Value objects
        Value[] values = getValues(getTargetType(def), resolver);
        Value[] oldValues = null;
        boolean oldMultiple = false;
        int oldType = -1;
        if (node.hasProperty(name)) {
            final Property oldProperty = node.getProperty(name);
            if (oldProperty.isMultiple()) {
                oldMultiple = true;
                oldValues = oldProperty.getValues();
            } else {
                oldValues = new Value[1];
                oldValues[0] = oldProperty.getValue();
            }
            oldType = oldProperty.getType();
            if (mergeOverride()) {
                oldProperty.remove();
            } else if (mergeCombine()) {
                values = calculateNewValues(values, oldValues);
            } else if (mergeSkip()) {
                log.debug("skipping existing property {}", name);
                return;
            } else {
                if (!def.isAutoCreated()) {
                    log.warn("Property "+node.safeGetJCRPath()+"/"+name+" already exist");
                }
            }
        }

        if (isPathReference) {
            Reference ref =  new Reference(name, values, def.isMultiple());
            if (derefNodes.containsKey(node.getNodeId())) {
                List<Reference> refs = derefNodes.get(node.getNodeId());
                refs.add(ref);
                derefNodes.put(node.getNodeId(), refs);
            } else {
                List<Reference> refs = new ArrayList<Reference>();
                refs.add(ref);
                derefNodes.put(node.getNodeId(), refs);
            }

            // References will be set in the post processing.
            // Generate a stub if needed:
            // 0. add nodeId and Reference for later processing
            // 1. if prop != mandatory => don't set
            // 2. if prop == mandatory
            // 2.1 if prop is multi => set empty
            // 2.2 if prop is single => set ref to root

            if (!def.isMandatory()) {
                return;
            }

            if (multiple || def.isMultiple()) {
                node.setProperty(name, new Value[] {}, type);
                return;
            }

            // single value mandatory property, temporary set ref to rootNode
            Value rootRef = node.getSession().getValueFactory().createValue(node.getSession().getRootNode().getIdentifier(), PropertyType.REFERENCE);
            node.setProperty(name, rootRef);

            return;
        }

        if (!multiple && values.length == 1 && !def.isMultiple()) {
            try {
                // set single-value
                node.setProperty(name, values[0]);
            } catch (ValueFormatException | ConstraintViolationException e) {
                // setting single-value failed, try setting value array
                // as a last resort (in case there are ambiguous property
                // definitions)
                node.setProperty(name, values, type);
            }
        } else {
            // can only be multi-valued (n == 0 || n > 1)
            node.setProperty(name, values, type);
        }
    }

    Value[] calculateNewValues(Value[] addValues, final Value[] oldValues) {
        Value[] newValues = new Value[addValues.length + oldValues.length];
        int location = mergeLocation(oldValues);
        if (validateLocation(location, oldValues.length)) {
            System.arraycopy(oldValues, 0, newValues, 0, location);
            System.arraycopy(addValues, 0, newValues, location, addValues.length);
            System.arraycopy(oldValues, location, newValues, location+addValues.length, oldValues.length-location);
            return newValues;
        } else {
            return oldValues;
        }
    }

    private boolean validateLocation(final int location, final int length) {
        if (location < 0) {
            log.warn("Invalid insert location {}", location);
            return false;
        }
        if (location > length) {
            log.warn("Invalid insert location {}: beyond values length", location);
            return false;
        }
        return true;
    }

    private boolean isGeneratedProperty(Name name) throws RepositoryException {
        final String jcrName = resolver.getJCRName(name);
        return HIPPO_PATHS.equals(jcrName) || HIPPO_RELATED.equals(jcrName);
    }

}
