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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class EffectiveNodeTypeImpl extends Sealable implements EffectiveNodeType {

    private final long version;
    private String name;
    private String prefix;

    private Set<String> superTypes = new TreeSet<String>();
    private Set<String> aggregatedTypes = new TreeSet<String>();
    private boolean aggregate;
    private boolean abstractType;
    private boolean mixin;
    private boolean ordered;

    private String primaryItemName;

    private Map<String, List<EffectiveNodeTypeChild>> children = new TreeMap<String, List<EffectiveNodeTypeChild>>();
    private Map<String, List<EffectiveNodeTypeProperty>> properties = new TreeMap<String, List<EffectiveNodeTypeProperty>>();

    private static final Comparator<EffectiveNodeTypeProperty> propertyComparator = new Comparator<EffectiveNodeTypeProperty>() {
        @Override
        public int compare(final EffectiveNodeTypeProperty p1, final EffectiveNodeTypeProperty p2) {
            int val = p1.getRequiredType() - p2.getRequiredType();
            if (val == 0) {
                val = p1.isMultiple() ? p2.isMultiple() ? 0 : 1 : -1;
            }
            return val;
        }
    };

    private static final Comparator<EffectiveNodeTypeChild> childComparator = new Comparator<EffectiveNodeTypeChild>() {
        @Override
        public int compare(final EffectiveNodeTypeChild c1, final EffectiveNodeTypeChild c2) {
            int val = c1.getRequiredPrimaryTypes().size() - c2.getRequiredPrimaryTypes().size();
            if (val == 0) {
                Iterator<String> i1 = c1.getRequiredPrimaryTypes().iterator();
                Iterator<String> i2 = c2.getRequiredPrimaryTypes().iterator();
                // assuming ordered set (e.g. TreeSet) of requiredPrimaryTypes
                while (i1.hasNext() && (val = i1.next().compareTo(i2.next()))==0) ;
            }
            return val;
        }
    };

    public EffectiveNodeTypeImpl(String name, long effectiveNodeTypesVersion) {
        this.version = effectiveNodeTypesVersion;
        this.aggregate = false;
        this.name = name;
        this.prefix = name.substring(0, name.indexOf(":"));
        this.aggregatedTypes.add(this.name);
    }

    public EffectiveNodeTypeImpl(EffectiveNodeTypeImpl other) {
        this.version = other.version;
        aggregate = other.aggregate;
        name = other.name;
        prefix = other.prefix;
        aggregatedTypes.addAll(other.getAggregatedTypes());
        superTypes.addAll(other.getSuperTypes());
        for (Map.Entry<String, List<EffectiveNodeTypeChild>> entry : other.children.entrySet()) {
            children.put(entry.getKey(), new ArrayList<EffectiveNodeTypeChild>(entry.getValue()));
        }
        for (Map.Entry<String, List<EffectiveNodeTypeProperty>> entry : other.properties.entrySet()) {
            properties.put(entry.getKey(), new ArrayList<EffectiveNodeTypeProperty>(entry.getValue()));
        }
        abstractType = other.abstractType;
        mixin = other.mixin;
        ordered = other.ordered;
        primaryItemName = other.primaryItemName;
    }

    @Override
    protected void doSeal() {
        superTypes = Collections.unmodifiableSet(superTypes);
        aggregatedTypes = Collections.unmodifiableSet(aggregatedTypes);
        for (Map.Entry<String, List<EffectiveNodeTypeChild>> entry : children.entrySet()) {
            Collections.sort(entry.getValue(),childComparator);
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
            for (EffectiveNodeTypeChild i : entry.getValue())
                ((Sealable)i).seal();
        }
        for (Map.Entry<String, List<EffectiveNodeTypeProperty>> entry : properties.entrySet()) {
            Collections.sort(entry.getValue(),propertyComparator);
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
            for (EffectiveNodeTypeProperty i : entry.getValue())
                ((Sealable)i).seal();
        }
        children = Collections.unmodifiableMap(children);
        properties = Collections.unmodifiableMap(properties);
    }

    @Override
    public long version() {
        return version;
    }

    @Override
    public boolean isAggregate() {
        return aggregate;
    }

    @Override
    public Set<String> getAggregatedTypes() {
        return aggregatedTypes;
    }

    @Override
    public String getName() {
        if (name == null) {
            Iterator<String> iterator = aggregatedTypes.iterator();
            StringBuilder sb = new StringBuilder(iterator.next());
            while (iterator.hasNext()) {
                sb.append(',');
                sb.append(iterator.next());
            }
            name = sb.toString();
        }
        return name;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public Set<String> getSuperTypes() {
        return superTypes;
    }

    @Override
    public boolean isNodeType(String nodeTypeName) {
        return superTypes.contains(nodeTypeName) || aggregatedTypes.contains(nodeTypeName);
    }

    @Override
    public boolean isAbstract() {
        return abstractType;
    }

    public void setAbstract(boolean abstractType) {
        checkSealed();
        this.abstractType = abstractType;
    }

    @Override
    public boolean isMixin() {
        return mixin;
    }

    public void setMixin(boolean mixin) {
        checkSealed();
        this.mixin = mixin;
    }

    @Override
    public boolean isOrdered() {
        return ordered;
    }

    public void setOrdered(boolean ordered) {
        checkSealed();
        this.ordered = ordered;
    }

    @Override
    public String getPrimaryItemName() {
        return primaryItemName;
    }

    public void setPrimaryItemName(String primaryItemName) {
        checkSealed();
        this.primaryItemName = primaryItemName;
    }

    @Override
    public Map<String, List<EffectiveNodeTypeChild>> getChildren() {
        return children;
    }

    @Override
    public Map<String, List<EffectiveNodeTypeProperty>> getProperties() {
        return properties;
    }

    public int hashCode() {
        if (isSealed()) {
            return this.getPrefix().hashCode() + this.getName().hashCode();
        }
        return super.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof EffectiveNodeTypeImpl && this.isSealed() && ((Sealable)obj).isSealed()) {
            return this.getName().equals(((EffectiveNodeTypeImpl)obj).getName());
        }
        return false;
    }

    public boolean contains(EffectiveNodeTypeImpl other) {
        for (String s : other.superTypes) {
            if (!isNodeType(s)) {
                return false;
            }
        }
        for (String s : other.aggregatedTypes) {
            if (!isNodeType(s)) {
                return false;
            }
        }
        return true;
    }

    public boolean merge(EffectiveNodeTypeImpl other, boolean superType) {
        if (contains(other)) {
            return false;
        }

        aggregate = true;
        name = null;
        prefix = null;

        // merge properties: assuming merging these are all allowed and has been validated by the JCR Repository itself before
        for (Map.Entry<String, List<EffectiveNodeTypeProperty>> entry : other.properties.entrySet()) {
            for (EffectiveNodeTypeProperty p : entry.getValue()) {
                if (!isNodeType(p.getDefiningType())) {
                    List<EffectiveNodeTypeProperty> props = properties.get(entry.getKey());
                    if (props == null) {
                        props = new ArrayList<EffectiveNodeTypeProperty>();
                        properties.put(entry.getKey(), props);
                    }
                    props.add(p);
                }
            }
        }

        // merge children: assuming merging these are all allowed and has been validated by the JCR Repository itself before
        for (Map.Entry<String, List<EffectiveNodeTypeChild>> entry : other.children.entrySet()) {
            for (EffectiveNodeTypeChild c : entry.getValue()) {
                if (!isNodeType(c.getDefiningType())) {
                    List<EffectiveNodeTypeChild> childs = children.get(entry.getKey());
                    if (childs == null) {
                        childs = new ArrayList<EffectiveNodeTypeChild>();
                        children.put(entry.getKey(), childs);
                    }
                    childs.add(c);
                }
            }
        }

        if (superType) {
            superTypes.addAll(other.aggregatedTypes);
        }
        else {
            aggregatedTypes.addAll(other.aggregatedTypes);
        }
        superTypes.addAll(other.superTypes);

        if (other.ordered) {
            this.ordered = true;
        }

        if (primaryItemName == null && other.primaryItemName != null) {
            this.primaryItemName = other.primaryItemName;
        }

        return true;
    }
}
