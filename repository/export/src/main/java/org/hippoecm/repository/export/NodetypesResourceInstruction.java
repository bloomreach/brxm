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
package org.hippoecm.repository.export;

import static org.hippoecm.repository.export.Constants.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.JcrCompactNodeTypeDefWriter;

class NodetypesResourceInstruction extends ResourceInstruction implements NamespaceInstruction {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id: ";

    private final String nodetypesresource;
    private String internalPrefix;
    private final String prefix;
    private String namespace;
    private final String namespaceroot;
    private Element namespacePropertyValue;

    NodetypesResourceInstruction(String name, Double sequence, File basedir, String nodetypesresource, String namespace, Element namespacePropertyValue, String internalPrefix) {
        super(name, sequence, new File(basedir, nodetypesresource));
        if (!file.exists()) {
            changed = true;
        }
        this.nodetypesresource = nodetypesresource;
        this.internalPrefix = internalPrefix;
        int indexOfUnderscore = internalPrefix.indexOf('_');
        this.prefix = (indexOfUnderscore == -1) ? internalPrefix : internalPrefix.substring(0, indexOfUnderscore);
        if (namespace != null) {
            this.namespace = namespace;
            int lastIndexOfPathSeparator = namespace.lastIndexOf('/');
            namespaceroot = (lastIndexOfPathSeparator == -1) ? namespace : namespace.substring(0, lastIndexOfPathSeparator);
        } else {
            this.namespace = null;
            namespaceroot = null;
        }
        this.namespacePropertyValue = namespacePropertyValue;
    }

    @Override
    void export(Session session) {
        log.info("Exporting " + nodetypesresource);
        try {
            if (!file.exists()) {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            }
            Writer out = new FileWriter(file);
            try {
                String cnd = null;
                try {
                    log.debug("Trying to export cnd for internal prefix " + internalPrefix);
                    cnd = JcrCompactNodeTypeDefWriter.compactNodeTypeDef(session.getWorkspace(), internalPrefix);
                    // HACK: we only get events for /jcr:system/jcr:nodeTypes/example_1_1 instead
                    // of for /jcr:system/jcr:nodeTypes/example
                    // here we fix that prefix
                    cnd = cnd.replaceAll(internalPrefix, prefix);
                } catch (NamespaceException e) {
                    log.debug("Failed. Now trying regular prefix " + prefix);
                    // update all content was already finished, we can use regular prefix
                    // but we need to first get a fresh session because the old session
                    // does not seem to pick up the last step in update all content
                    session = ((HippoSession)session).impersonate(new SimpleCredentials("system", new char[] {}));
                    try {
                        cnd = JcrCompactNodeTypeDefWriter.compactNodeTypeDef(session.getWorkspace(), prefix);
                    } finally {
                        session.logout();
                    }
                }
                out.write(cnd);
                out.flush();
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        } catch (IOException e) {
            log.error("Exporting " + nodetypesresource + " failed.", e);
        } catch (RepositoryException e) {
            log.error("Exporting " + nodetypesresource + " failed.", e);
        }
        changed = false;
    }

    @Override
    public Element createInstructionElement() {
        Element element = createBaseInstructionElement();
        // create element:
        // <sv:property sv:name="hippo:nodetypesresource" sv:type="String">
        //   <sv:value>{this.m_file.getName()}</sv:value>
        // </sv:property>
        Element cndProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
        cndProperty.add(DocumentFactory.getInstance().createAttribute(cndProperty, NAME_QNAME, "hippo:nodetypesresource"));
        cndProperty.add(DocumentFactory.getInstance().createAttribute(cndProperty, TYPE_QNAME, "String"));
        Element cndPropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
        cndPropertyValue.setText(nodetypesresource);
        cndProperty.add(cndPropertyValue);
        element.add(cndProperty);

        if (namespace != null) {
            // create element:
            // <sv:property sv:name="hippo:namespace" sv:type="String">
            //   <sv:value>{this.m_namespace}</sv:value>
            // </sv:property>
            Element namespaceProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
            namespaceProperty.add(DocumentFactory.getInstance().createAttribute(namespaceProperty, NAME_QNAME, "hippo:namespace"));
            namespaceProperty.add(DocumentFactory.getInstance().createAttribute(namespaceProperty, TYPE_QNAME, "String"));
            Element namespacePropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
            namespacePropertyValue.setText(namespace);
            namespaceProperty.add(namespacePropertyValue);
            this.namespacePropertyValue = namespacePropertyValue;
            element.add(namespaceProperty);
        }

        return element;
    }

    @Override
    void nodeAdded(String path) {
        setInternalPrefixFromPath(path);
        changed = true;
    }

    @Override
    boolean nodeRemoved(String path) {
        setInternalPrefixFromPath(path);
        changed = true;
        // TODO: should determine whether or not context was removed
        return false;
    }

    /* Don't think this can happen on a node type node */
    @Override
    void propertyAdded(String path) {
        setInternalPrefixFromPath(path);
        changed = true;
    }

    /* Don't think this can happen on a node type node */
    @Override
    void propertyChanged(String path) {
        setInternalPrefixFromPath(path);
        changed = true;
    }

    /* Don't think this can happen on a node type node */
    @Override
    void propertyRemoved(String path) {
        setInternalPrefixFromPath(path);
        changed = true;
    }

    private void setInternalPrefixFromPath(String path) {
        // path = /jcr:system/jcr:nodeTypes/example_1_2:doctype/jcr:propertyDefinition
        // relPath = example_1_2:doctype/jcr:propertyDefinition
        String relPath = path.substring("/jcr:system/jcr:nodeTypes/".length());
        // nodeTypeRoot = example_1_2:doctype
        int indexOfPathSeparator = relPath.indexOf('/');
        String nodeTypeRoot = (indexOfPathSeparator == -1) ? relPath : relPath.substring(0, indexOfPathSeparator);
        // internalPrefix = example_1_2
        int indexOfColon = nodeTypeRoot.indexOf(':');
        internalPrefix = (indexOfColon == -1) ? nodeTypeRoot : nodeTypeRoot.substring(0, indexOfColon);
    }

    boolean matchesPrefix(String _internalPrefix) {
        // internalPrefix = example_1_2
        // prefix = example
        int indexOfUnderscore = _internalPrefix.indexOf('_');
        String _prefix = (indexOfUnderscore == -1) ? _internalPrefix : _internalPrefix.substring(0, indexOfUnderscore);
        return _prefix.equals(prefix);
    }

    @Override
    public String toString() {
        return "NodetypesResourceInstruction[prefix=" + prefix + "]";
    }

    @Override
    public boolean matchesNamespace(String _namespace) {
        if (namespace == null)
            return false;
        int lastIndexOfPathSeparator = _namespace.lastIndexOf('/');
        String _namespaceroot = (lastIndexOfPathSeparator == -1) ? _namespace : _namespace.substring(0, lastIndexOfPathSeparator);
        return _namespaceroot.equals(namespaceroot);
    }

    public void updateNamespace(String namespace) {
        this.namespace = namespace;
        namespacePropertyValue.setText(namespace);
    }
}
