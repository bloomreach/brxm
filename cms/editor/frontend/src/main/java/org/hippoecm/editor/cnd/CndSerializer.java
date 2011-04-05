/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.editor.cnd;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.repository.api.ISO9075Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a CND based on ITypeDescriptors.
 */
public final class CndSerializer {

    static final Logger log = LoggerFactory.getLogger(CndSerializer.class);

    class SortContext {
        HashSet<String> visited;
        LinkedHashSet<ITypeDescriptor> result;

        SortContext() {
            visited = new HashSet<String>();
        }

        void visit(String typeName) throws StoreException {
            if (visited.contains(typeName) || !types.containsKey(typeName)) {
                return;
            }

            ITypeDescriptor descriptor = types.get(typeName);

            visited.add(typeName);
            for (String superType : descriptor.getSuperTypes()) {
                visit(superType);
            }
            for (IFieldDescriptor field : descriptor.getFields().values()) {
                visit(field.getTypeDescriptor().getName());
            }
            result.add(types.get(typeName));
        }

        LinkedHashSet<ITypeDescriptor> sort() throws StoreException {
            result = new LinkedHashSet<ITypeDescriptor>();
            for (String type : types.keySet()) {
                visit(type);
            }
            return result;
        }
    }

    private final Map<String, String> namespaces = new HashMap<String, String>();
    private final Map<String, ITypeDescriptor> types = new TreeMap<String, ITypeDescriptor>();
    private final Session jcrSession;

    public CndSerializer(Session jcrSession) {
        this.jcrSession = jcrSession;
    }

    public String getOutput() {
        resolveNamespaces();

        StringBuffer output = new StringBuffer();
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            output.append("<" + entry.getKey() + "='" + entry.getValue() + "'>\n");
        }
        output.append("\n");

        try {
            Set<ITypeDescriptor> sorted = sortTypes();
            for (ITypeDescriptor type : sorted) {
                renderType(output, type);
            }
        } catch (StoreException ex) {
            throw new RuntimeException("Type has disappeared!", ex);
        }
        return output.toString();
    }

    public String getNamespace(String prefix) {
        return namespaces.get(prefix);
    }

    public void addNamespace(String prefix) {
        if (!namespaces.containsKey(prefix)) {
            try {
                namespaces.put(prefix, jcrSession.getNamespaceURI(prefix));
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    public void addType(ITypeDescriptor type) {
        this.types.put(type.getName(), type);
    }

    public void remap(String prefix, String uri) {
        namespaces.put(prefix, uri);
    }

    private void resolveNamespaces() {
        for (ITypeDescriptor descriptor : types.values()) {
            if (descriptor.isNode()) {
                String type = descriptor.getType();
                addNamespace(type.substring(0, type.indexOf(':')));
                for (String superType : descriptor.getSuperTypes()) {
                    addNamespace(superType.substring(0, superType.indexOf(':')));
                }

                for (IFieldDescriptor field : descriptor.getFields().values()) {
                    ITypeDescriptor sub = field.getTypeDescriptor();
                    if (sub.isNode()) {
                        String subType = sub.getType();
                        addNamespace(subType.substring(0, subType.indexOf(':')));

                        List<String> superTypes = sub.getSuperTypes();
                        for (String superType : superTypes) {
                            addNamespace(superType.substring(0, superType.indexOf(':')));
                        }
                    } else if (field.getPath().indexOf(':') > 0) {
                        addNamespace(field.getPath().substring(0, field.getPath().indexOf(':')));
                    }
                }
            }
        }
    }

    private void renderField(StringBuffer output, IFieldDescriptor field) throws StoreException {
        ITypeDescriptor sub = field.getTypeDescriptor();
        if (sub.isNode()) {
            output.append("+");
        } else {
            output.append("-");
        }

        if (field.getPath() != null) {
            output.append(' ');
            output.append(encode(field.getPath()));
        } else {
            output.append(" *");
        }

        String type = sub.getType();
        if (type.indexOf(':') == -1) {
            type = type.toLowerCase();
        }
        output.append(" (");
        output.append(type);
        output.append(')');
        if (field.isMultiple()) {
            output.append(" multiple");
        }
        if (field.isMandatory()) {
            output.append(" mandatory");
        }
        if (field.isPrimary()) {
            output.append(" primary");
        }
        output.append("\n");
    }

    private void renderType(StringBuffer output, ITypeDescriptor typeDescriptor) throws StoreException {
        String type = typeDescriptor.getType();
        output.append("[" + encode(type) + "]");

        Iterator<String> superTypes = typeDescriptor.getSuperTypes().iterator();
        boolean first = true;
        while (superTypes.hasNext()) {
            String superType = superTypes.next();
            if (first) {
                first = false;
                output.append(" > " + superType);
            } else {
                output.append(", " + superType);
            }
        }

        if (typeDescriptor.isMixin()) {
            output.append(" mixin");
        }

        // type should be orderable if any of the fields is
        for (IFieldDescriptor field : typeDescriptor.getFields().values()) {
            if (field.isOrdered()) {
                output.append(" orderable");
                break;
            }
        }

        output.append("\n");
        for (IFieldDescriptor field : typeDescriptor.getDeclaredFields().values()) {
            renderField(output, field);
        }
        output.append("\n");
    }

    private Set<ITypeDescriptor> sortTypes() throws StoreException {
        return new SortContext().sort();
    }

    private static String encode(String name) {
        int colon = name.indexOf(':');
        if (colon > 0) {
            return name.substring(0, colon + 1) + ISO9075Helper.encodeLocalName(name.substring(colon + 1));
        }
        return name;
    }
}
