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
package org.hippoecm.repository.jackrabbit.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.ItemExistsException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.xml.Importer;
import org.apache.jackrabbit.core.xml.TextValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class PropInfo extends org.apache.jackrabbit.core.xml.PropInfo {


    private static Logger log = LoggerFactory.getLogger(PropInfo.class);


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


    public PropInfo(NamePathResolver resolver, Name name, int type, Boolean multiple, TextValue[] values, String mergeBehavior,
                    String mergeLocation, final URL[] binaryURLs, final ValueFactory valueFactory) {
        super(name, type, values);
        this.multiple = multiple != null ? multiple : Boolean.FALSE;
        this.mergeBehavior = mergeBehavior;
        this.mergeLocation = mergeLocation;
        if (name.getLocalName().endsWith(Reference.REFERENCE_SUFFIX)) {
            String local = name.getLocalName();
            local = local.substring(0, (local.length() - Reference.REFERENCE_SUFFIX.length()));
            this.name =  NameFactoryImpl.getInstance().create(name.getNamespaceURI(), local);
            this.isPathReference = true;
            // paths are strings
            //this.type = PropertyType.REFERENCE;
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

    public boolean mergeOverride() {
        return "override".equalsIgnoreCase(mergeBehavior);
    }

    public boolean mergeCombine() {
        return "append".equalsIgnoreCase(mergeBehavior) || "insert".equalsIgnoreCase(mergeBehavior);
    }

    public boolean mergeConflict() {
        return "conflict".equalsIgnoreCase(mergeBehavior);
    }

    public int mergeLocation(Value[] values) {
        if("append".equalsIgnoreCase(mergeBehavior)) {
            return values.length;
        } else if("insert".equalsIgnoreCase(mergeBehavior)) {
            if(mergeLocation == null || mergeLocation.trim().equals("")) {
                return 0;
            } else {
                return Integer.parseInt(mergeLocation);
            }
        } else {
            return 0;
        }
    }

    /**
     * Disposes all values contained in this property.
     */
    public void dispose() {
        for (int i = 0; i < values.length; i++) {
            values[i].dispose();
        }
    }

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

    public void apply(NodeImpl node, NamePathResolver resolver,
            Map<NodeId, List<Reference>> derefNodes, String basePath, int referenceBehavior) throws RepositoryException {

        // find applicable definition
        QPropertyDefinition def = getApplicablePropertyDef(node.getEffectiveNodeType());
        if (def.isProtected()) {
            // skip protected property
            log.debug("skipping protected property " + name);
            return;
        }
        if (isGeneratedProperty(name)) {
            // skip autogenerated property, let the repository handle recreation
            log.debug("skipping autogenerated property " + name);
            return;
        }

        // convert serialized values to Value objects
        Value[] va = getValues(getTargetType(def), resolver);

        if (node.hasProperty(name)) {
            if (mergeOverride()) {
                node.getProperty(name).remove();
            } else if (mergeCombine()) {
                Property oldProperty = node.getProperty(name);
                Value[] oldValues;
                if (oldProperty.isMultiple()) {
                    oldValues = oldProperty.getValues();
                } else {
                    oldValues = new Value[1];
                    oldValues[0] = oldProperty.getValue();
                }
                Value[] newValues = new Value[va.length + oldValues.length];
                int pos = mergeLocation(oldValues);
                System.arraycopy(oldValues, 0, newValues, 0, pos);
                System.arraycopy(va, 0, newValues, pos, va.length);
                System.arraycopy(oldValues, pos, newValues, pos+va.length, oldValues.length-pos);
                va = newValues;
            } else if (mergeConflict()) {
                throw new ItemExistsException("Property " + node.getPath() + "/" + name + " already exist");
            } else {
                if (!def.isAutoCreated()) {
                    log.warn("Property "+node.getPath()+"/"+name+" already exist");
                }
            }
        }

        if (isPathReference) {
            Reference ref =  new Reference(name, va, def.isMultiple());
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
            Value rootRef = node.getSession().getValueFactory().createValue(node.getSession().getRootNode().getUUID(), PropertyType.REFERENCE);
            node.setProperty(name, rootRef);

            return;
        }

        // multi- or single-valued property?
        if (!multiple && va.length == 1 && !def.isMultiple()) {
            Exception e = null;
            try {
                // set single-value
                node.setProperty(name, va[0]);
            } catch (ValueFormatException vfe) {
                e = vfe;
            } catch (ConstraintViolationException cve) {
                e = cve;
            }
            if (e != null) {
                // setting single-value failed, try setting value array
                // as a last resort (in case there are ambiguous property
                // definitions)
                node.setProperty(name, va, type);
            }
        } else {
            // can only be multi-valued (n == 0 || n > 1)
            node.setProperty(name, va, type);
        }
    }

    private boolean isGeneratedProperty(Name name) throws RepositoryException {
        
        if (HippoNodeType.HIPPO_PATHS.equals(resolver.getJCRName(name))) {
            return true;
        }
        if (HippoNodeType.HIPPO_RELATED.equals(resolver.getJCRName(name))) {
            return true;
        }
        return false;
    }
}
