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
package org.hippoecm.frontend.template.export;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.template.FieldDescriptor;
import org.hippoecm.frontend.template.TypeDescriptor;
import org.hippoecm.frontend.template.config.TypeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CndSerializer implements IClusterable {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(CndSerializer.class);

    private JcrSessionModel jcrSession;
    private HashMap<String, String> namespaces;
    private LinkedHashSet<TypeDescriptor> types;
    private TypeConfig typeConfig;

    public CndSerializer(JcrSessionModel session, TypeConfig config, String namespace) {
        this.jcrSession = session;
        namespaces = new HashMap<String, String>();
        types = new LinkedHashSet<TypeDescriptor>();
        typeConfig = config;

        List<TypeDescriptor> list = config.getTypes(namespace);
        for (TypeDescriptor descriptor : list) {
            if (descriptor.isNode()) {
                String type = descriptor.getType();
                if (type.indexOf(':') > 0) {
                    String prefix = type.substring(0, type.indexOf(':'));
                    if (namespace.equals(prefix)) {
                        addType(descriptor);
                    }
                }
            }
        }
    }

    public String getOutput() {
        StringBuffer output = new StringBuffer();
        for (Map.Entry<String, String> entry : getNamespaces().entrySet()) {
            output.append("<" + entry.getKey() + "='" + entry.getValue() + "'>\n");
        }
        output.append("\n");

        sortTypes();

        for (TypeDescriptor descriptor : types) {
            renderType(output, descriptor);
        }
        return output.toString();
    }

    public void addNamespace(String prefix) {
        try {
            Session session = jcrSession.getSession();
            if (!namespaces.containsKey(prefix)) {
                namespaces.put(prefix, session.getNamespaceURI(prefix));
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public void versionNamespace(String prefix) {
        if (namespaces.containsKey(prefix)) {
            String namespace = namespaces.get(prefix);
            int pos = namespace.lastIndexOf('/');
            int minorPos = namespace.lastIndexOf('.');
            if (minorPos > pos) {
                int minor = Integer.parseInt(namespace.substring(minorPos + 1));
                namespace = namespace.substring(0, minorPos + 1) + new Integer(minor + 1).toString();
                namespaces.put(prefix, namespace);
            } else {
                log.warn("namespace for " + prefix + " does not conform to versionable format");
            }
        } else {
            log.warn("namespace for " + prefix + " was not found");
        }
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public void addType(TypeDescriptor typeDescriptor) {
        String type = typeDescriptor.getType();
        if (type.indexOf(':') > 0) {
            if (!types.contains(typeDescriptor)) {
                for (FieldDescriptor field : typeDescriptor.getFields()) {
                    String subType = field.getType();
                    TypeDescriptor sub = typeConfig.getTypeDescriptor(subType);
                    if (sub.isNode()) {
                        addNamespace(subType.substring(0, subType.indexOf(':')));

                        String superType = sub.getSuperType();
                        addNamespace(superType.substring(0, superType.indexOf(':')));

                        Iterator<String> mixins = sub.getMixinTypes().iterator();
                        while (mixins.hasNext()) {
                            String mixin = mixins.next();
                            addNamespace(mixin.substring(0, mixin.indexOf(':')));
                        }
                    } else if (field.getPath().indexOf(':') > 0) {
                        addNamespace(field.getPath().substring(0, field.getPath().indexOf(':')));
                    }
                }
                types.add(typeDescriptor);
                addNamespace(type.substring(0, type.indexOf(':')));
            }
        }
    }

    private void renderField(StringBuffer output, FieldDescriptor field) {
        String subType = field.getType();
        TypeDescriptor sub = typeConfig.getTypeDescriptor(subType);
        if (sub.isNode()) {
            output.append("+");
        } else {
            output.append("-");
        }

        if (field.getPath() != null) {
            output.append(" " + field.getPath());
        } else {
            output.append(" *");
        }

        String type = field.getType();
        if (type.indexOf(':') > 0) {
            addNamespace(type.substring(0, type.indexOf(':')));
        } else {
            type = type.toLowerCase();
        }
        output.append(" (" + type + ")");
        if (field.isMultiple()) {
            output.append(" multiple");
        }
        if (field.isMandatory()) {
            output.append(" mandatory");
        }
        output.append("\n");
    }

    private void renderType(StringBuffer output, TypeDescriptor template) {
        String type = template.getType();
        output.append("[" + type + "]");

        if (template.getSuperType() != null) {
            output.append(" > " + template.getSuperType());
        }

        List<String> mixinFields = new LinkedList<String>();
        Iterator<String> mixins = template.getMixinTypes().iterator();
        while (mixins.hasNext()) {
            String mixin = mixins.next();
            TypeDescriptor mixinDescriptor = typeConfig.getTypeDescriptor(mixin);
            if (mixinDescriptor != null) {
                Iterator<FieldDescriptor> fields = mixinDescriptor.getFields().iterator();
                while (fields.hasNext()) {
                    FieldDescriptor field = fields.next();
                    if (!mixinFields.contains(field.getPath())) {
                        mixinFields.add(field.getPath());
                    }
                }
            }
            output.append(", " + mixin);
        }

        for (FieldDescriptor field : template.getFields()) {
            if (field.isOrdered()) {
                output.append(" orderable");
                break;
            }
        }

        output.append("\n");
        for (FieldDescriptor field : template.getFields()) {
            if (!mixinFields.contains(field.getPath())) {
                renderField(output, field);
            }
        }
        output.append("\n");
    }

    private void sortTypes() {
        types = new SortContext(types).sort();
    }

    class SortContext {
        HashSet<TypeDescriptor> visited;
        LinkedHashSet<TypeDescriptor> result;
        LinkedHashSet<TypeDescriptor> set;

        SortContext(LinkedHashSet set) {
            this.set = set;
            visited = new HashSet<TypeDescriptor>();
            result = new LinkedHashSet<TypeDescriptor>();
        }

        void visit(TypeDescriptor descriptor) {
            if (visited.contains(descriptor) || !types.contains(descriptor)) {
                return;
            }

            visited.add(descriptor);
            List<FieldDescriptor> fields = descriptor.getFields();
            for (FieldDescriptor field : fields) {
                TypeDescriptor type = typeConfig.getTypeDescriptor(field.getType());
                visit(type);
            }
            result.add(descriptor);
        }

        LinkedHashSet<TypeDescriptor> sort() {
            for (TypeDescriptor template : set) {
                visit(template);
            }
            return result;
        }
    }
}
