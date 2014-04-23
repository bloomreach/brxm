/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils.beansmodel;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @version "$Id: MemoryBean.java 172944 2013-08-06 16:37:37Z mmilicevic $"
 */
public class MemoryBean {


    @SuppressWarnings("StaticVariableOfConcreteClass")
    public static final MemoryBean HIPPO_DOCUMENT = new MemoryBean();

    static {
        HIPPO_DOCUMENT.addJavaImport("org.hippoecm.hst.content.beans.standard");
        HIPPO_DOCUMENT.addJavaImport("org.hippoecm.hst.content.beans.Node");
        HIPPO_DOCUMENT.setBuiltInType(true);
        HIPPO_DOCUMENT.setName("HippoDocument");
    }


    private boolean builtInType = false;
    private List<MemoryProperty> properties = new ArrayList<>();
    private MemoryBean beans;
    private MemoryBean supertype;
    private Path beanPath;
    private String namespace;
    private String name;
    private String prefixedName;
    private Set<String> javaImports = new HashSet<>();
    private Set<String> superTypeValues = new HashSet<>();

    public MemoryBean() {
    }

    public MemoryBean(final String name) {
        this.name = name;
    }

    public MemoryBean(final String name, final String namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    public Path getBeanPath() {
        return beanPath;
    }

    public void setBeanPath(final Path beanPath) {
        this.beanPath = beanPath;
    }

    public void addProperty(final MemoryProperty property) {
        properties.add(property);
    }

    public List<MemoryProperty> getProperties() {
        return properties;
    }

    public void setProperties(final List<MemoryProperty> properties) {
        this.properties = properties;
    }

    public MemoryBean getBeans() {
        return beans;
    }

    public void setBeans(final MemoryBean beans) {
        this.beans = beans;
    }

    public MemoryBean getSupertype() {
        return supertype;
    }

    public void setSupertype(final MemoryBean supertype) {
        this.supertype = supertype;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(final String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getPrefixedName() {
        if (prefixedName == null) {
            prefixedName = namespace + ':' + name;
        }
        return prefixedName;
    }

    public void setPrefixedName(final String prefixedName) {
        this.prefixedName = prefixedName;
    }

    public void addJavaImport(final String javaImport) {
        javaImports.add(javaImport);
    }

    public Set<String> getJavaImports() {
        return javaImports;
    }

    public void setJavaImports(final Set<String> javaImports) {
        this.javaImports = javaImports;
    }

    public boolean isBuiltInType() {
        return builtInType;
    }

    public void setBuiltInType(final boolean builtInType) {
        this.builtInType = builtInType;
    }

    public void addSupertypeValue(final String superTypeValue) {
        superTypeValues.add(superTypeValue);
    }

    public Set<String> getSuperTypeValues() {
        return superTypeValues;
    }

    public void setSuperTypeValues(final Set<String> superTypeValues) {
        this.superTypeValues = superTypeValues;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MemoryBean{");
        sb.append("builtInType=").append(builtInType);
        sb.append(", beans=").append(beans);
        sb.append(", supertype=").append(supertype);
        sb.append(", beanPath=").append(beanPath);
        sb.append(", namespace='").append(namespace).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", prefixedName='").append(prefixedName).append('\'');
        sb.append(", javaImports=").append(javaImports);
        sb.append(", superTypeValues=").append(superTypeValues);
        sb.append('}');
        return sb.toString();
    }
}
