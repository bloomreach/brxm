/*
 * Copyright 2007 Hippo (www.hippo.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.jr;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import nl.hippo.webdav.batchprocessor.Configuration;
import nl.hippo.webdav.batchprocessor.Plugin;
import nl.hippo.webdav.batchprocessor.PluginConfiguration;
import nl.hippo.webdav.batchprocessor.WebdavBatchProcessor;
import nl.hippo.webdav.batchprocessor.WebdavBatchProcessorException;

import org.apache.webdav.lib.Property;
import org.apache.webdav.lib.PropertyName;
import org.hippocms.repository.jr.embedded.Server;

public class Webdav2JCRDumper implements Plugin {

    private String webdavRootUri;
    private String dumpFile;
    private String workingDir;

    private Server server;
    private Session session;
    private ValueFactory valueFactory;

    public void configure(WebdavBatchProcessor processor, Configuration config, PluginConfiguration pluginConfig) {
        webdavRootUri = config.getRootUri();
        workingDir = pluginConfig.getValue("jcr.workingdir");
        dumpFile = pluginConfig.getValue("dumpfile");
        try {
            server = new Server(workingDir);
            session = server.login();
            valueFactory = session.getValueFactory();
        } catch (RepositoryException e) {
            throw new IllegalStateException("Failed to start JCR server.", e);
        }
    }

    public void process(nl.hippo.webdav.batchprocessor.Node webdavNode) throws WebdavBatchProcessorException {
        String parentPath = parentPath(webdavNode.getUri());
        String nodeName = nodeName(webdavNode.getUri());

        try {
            javax.jcr.Node parent = (javax.jcr.Node) session.getItem(parentPath);
            javax.jcr.Node current = parent.addNode(nodeName);

            // Add content property
            if (!webdavNode.isCollection()) {
                String contentLengthAsString = webdavNode.getProperty("DAV:", "getcontentlength").getPropertyAsString();
                int contentLength = Integer.parseInt(contentLengthAsString);

                if (contentLength > 0) {
                    byte[] content = webdavNode.getContents();
                    Value value = valueFactory.createValue(new ByteArrayInputStream(content));
                    current.setProperty("content", value);
                }
            }

            // Add metadata properties
            Iterator webdavPropertyNames = webdavNode.propertyNamesIterator();
            while (webdavPropertyNames.hasNext()) {
                PropertyName webdavPropertyName = (PropertyName) webdavPropertyNames.next();
                String webdavPropertyNamespace = webdavPropertyName.getNamespaceURI();
                if (!webdavPropertyNamespace.equals("DAV:")) {
                    String name = webdavPropertyName.getLocalName();
                    Property webdavProperty = webdavNode.getProperty(webdavPropertyNamespace, name);
                    Value value = valueFactory.createValue(webdavProperty.getPropertyAsString());
                    current.setProperty(name, value);
                }
            }
            session.save();

        } catch (RepositoryException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public boolean requiresNodeOrderPreservation() {
        return true;
    }

    public void postprocess() {
        try {
            //TODO: Implement createBackup(file) and restoreBackup(file) in Hippo repo2 server.
            //server.createBackup(dumpFile);
            
            //For the time being just dump the repo contents to the console to verify that it works.
            Node root = session.getRootNode();
            server.dump(root);
            
            
            session.logout();
            server.close();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    private String parentPath(String uri) {
        String result = uri.substring(webdavRootUri.length());
        int i = result.lastIndexOf("/");
        result = i == -1 ? result : result.substring(0, i);

        return "".equals(result) ? "/" : result;
    }

    private String nodeName(String uri) {
        return uri.substring(uri.lastIndexOf("/") + 1);
    }

}
