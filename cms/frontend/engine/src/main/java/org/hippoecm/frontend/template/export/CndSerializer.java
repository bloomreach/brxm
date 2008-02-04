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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.template.FieldDescriptor;
import org.hippoecm.frontend.template.TemplateDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CndSerializer implements IClusterable {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(CndSerializer.class);

    private JcrSessionModel jcrSession;
    private HashMap<String, String> namespaces;
    private LinkedHashSet<TemplateDescriptor> templates;

    public CndSerializer(JcrSessionModel session) {
        this.jcrSession = session;
        namespaces = new HashMap<String, String>();
        templates = new LinkedHashSet<TemplateDescriptor>();
    }

    public String getOutput() {
        StringBuffer output = new StringBuffer();
        for (Map.Entry<String, String> entry : getNamespaces().entrySet()) {
            output.append("<" + entry.getKey() + "='" + entry.getValue() + "'>\n");
        }
        output.append("\n");

        sortTemplates();

        for (TemplateDescriptor template : templates) {
            renderTemplate(output, template);
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

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public void addTemplate(TemplateDescriptor template) {
        String type = template.getType();
        if (type.indexOf(':') > 0) {
            addNamespace(type.substring(0, type.indexOf(':')));
            templates.add(template);
        }
    }

    private void renderField(StringBuffer output, FieldDescriptor field) {
        if (field.getTemplate() != null) {
            if (field.getTemplate().isNode()) {
                output.append("+");
            } else {
                output.append("-");
            }

            if (field.getPath() != null) {
                output.append(" " + field.getPath());
            } else {
                output.append(" *");
            }

            String type = field.getTemplate().getType();
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
            if (field.isOrdered()) {
                output.append(" ordered");
            }
            output.append("\n");
        } else {
            renderField(output, field.getField());
        }
    }

    private void renderTemplate(StringBuffer output, TemplateDescriptor template) {
        String type = template.getType();
        output.append("[" + type + "]\n");
        for (FieldDescriptor field : template.getFields()) {
            renderField(output, field);
        }
        output.append("\n");
    }

    private void sortTemplates() {
        templates = new SortContext(templates).sort();
    }

    class SortContext {
        HashSet<TemplateDescriptor> visited;
        LinkedHashSet<TemplateDescriptor> result;
        LinkedHashSet<TemplateDescriptor> set;

        SortContext(LinkedHashSet set) {
            this.set = set;
            visited = new HashSet<TemplateDescriptor>();
            result = new LinkedHashSet<TemplateDescriptor>();
        }

        void visit(TemplateDescriptor descriptor) {
            if (visited.contains(descriptor) || !templates.contains(descriptor)) {
                return;
            }

            visited.add(descriptor);
            List<FieldDescriptor> fields = descriptor.getFields();
            for (FieldDescriptor field : fields) {
                TemplateDescriptor sub;
                if (field.getTemplate() != null) {
                    sub = field.getTemplate();
                } else {
                    sub = field.getField().getTemplate();
                }
                visit(sub);
            }
            result.add(descriptor);
        }

        LinkedHashSet<TemplateDescriptor> sort() {
            for (TemplateDescriptor template : set) {
                visit(template);
            }
            return result;
        }
    }
}
