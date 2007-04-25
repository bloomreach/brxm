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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;

import nl.hippo.webdav.batchprocessor.Configuration;
import nl.hippo.webdav.batchprocessor.Plugin;
import nl.hippo.webdav.batchprocessor.PluginConfiguration;
import nl.hippo.webdav.batchprocessor.WebdavBatchProcessor;
import nl.hippo.webdav.batchprocessor.WebdavBatchProcessorException;

import org.apache.commons.httpclient.util.DateParseException;
import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.apache.webdav.lib.Property;
import org.apache.webdav.lib.PropertyName;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;

public class Webdav2JCRDumper implements Plugin {

    // ---------------------------------------------------- Constants
    private static final String JCR_RMI_HOST = "jcr.rmi.host";
    private static final String JCR_RMI_PORT = "jcr.rmi.port";

    private static final String JCR_REPOSITORY = "jcr.repository";
    private static final String JCR_WORKSPACE = "jcr.workspace";
    private static final String JCR_USER = "jcr.rmi.user";
    private static final String JCR_PASS = "jcr.rmi.pass";
    private static final String JCR_PATH = "jcr.path";

    /* DAV::creationate = 2006-12-18T14:24:53Z */
    private static final SimpleDateFormat CREATIONDATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /* hippo::publicationDate = 20040124 */
    private static final SimpleDateFormat PUBLICATIONDATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    /* hippo::documentdate = 20040124 */
    private static final SimpleDateFormat DOCUMENTDATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    
    // ---------------------------------------------------- Instance variables
    // rmi
    private String rmiHost;
    private String rmiPort;

    // jcr
    private String jcrRepository;
    private String jcrWorkspace;
    private String jcrUser;
    private String jcrPass;
    private String jcrPath;

    private Session session;
    private ValueFactory valueFactory;
    private String webdavRootUri;

    // backup & embedded server
    //private String dumpFile;
    //private String workingDir;
    //private Server server;

    public void configure(WebdavBatchProcessor processor, Configuration config, PluginConfiguration pluginConfig) {

        // Fetch properties
        webdavRootUri = config.getRootUri();
        //workingDir = pluginConfig.getValue("jcr.workingdir");
        //dumpFile = pluginConfig.getValue("dumpfile");

        // rmi connection
        rmiHost = pluginConfig.getValue(JCR_RMI_HOST);
        rmiPort = pluginConfig.getValue(JCR_RMI_PORT);

        // jcr settings
        jcrRepository = pluginConfig.getValue(JCR_REPOSITORY);
        jcrWorkspace = pluginConfig.getValue(JCR_WORKSPACE);
        jcrUser = pluginConfig.getValue(JCR_USER);
        jcrPass = pluginConfig.getValue(JCR_PASS);
        jcrPath = pluginConfig.getValue(JCR_PATH);

        // remove trainling slash
        if (jcrPath.endsWith("/")) {
            jcrPath = jcrPath.substring(0, jcrPath.length() - 1);
        }

        // show config
        printConfig(config);

        // test and setup connection and login       
        ClientRepositoryFactory factory = new ClientRepositoryFactory();
        Repository repository;
        try {
            // get the repository
            repository = factory.getRepository(getRmiUrl());

            // login and get session
            session = repository.login(new SimpleCredentials(jcrUser, jcrPass.toCharArray()));

            // todo, add option to set and create workspace
            Workspace ws = session.getWorkspace();
            if (!ws.getName().equals(jcrWorkspace)) {
                throw new RuntimeException("Workspaces not equal. Found: " + ws.getName() + " requested: "
                        + jcrWorkspace);
            }

            // test and create target path
            checkPath();

            valueFactory = session.getValueFactory();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void process(nl.hippo.webdav.batchprocessor.Node webdavNode) throws WebdavBatchProcessorException {
        String jcrParentPath = jcrPath + parentPath(webdavNode.getUri());
        String nodeName = nodeName(webdavNode.getUri());

        if (webdavNode.getUri().equals(webdavRootUri)) {
            return;
        }
        
        try {
            //System.out.println("Converting WebDAV node: " + webdavNode.getUri() + " => " + jcrParentPath + "/" + nodeName);
            System.out.append('.');
            
            javax.jcr.Node parent = (javax.jcr.Node) session.getItem(jcrParentPath);

            // Add content property
            if (!webdavNode.isCollection()) {

                // Overwrite existing nodes
                if (parent.hasNode(nodeName)) {
                    parent.getNode(nodeName).remove();
                }

                javax.jcr.Node current = parent.addNode(nodeName);
                

                current.addMixin("mix:referenceable");
                current.setProperty("published", false);

                String contentLengthAsString = webdavNode.getProperty("DAV:", "getcontentlength").getPropertyAsString();
                int contentLength = Integer.parseInt(contentLengthAsString);

                if (contentLength > 0) {
                    byte[] content = webdavNode.getContents();
                    current.setProperty("content", valueFactory.createValue(new ByteArrayInputStream(content)));
                }

                // Add metadata properties
                Iterator webdavPropertyNames = webdavNode.propertyNamesIterator();
                while (webdavPropertyNames.hasNext()) {
                    PropertyName webdavPropertyName = (PropertyName) webdavPropertyNames.next();
                    String webdavPropertyNamespace = webdavPropertyName.getNamespaceURI();
                    if (!webdavPropertyNamespace.equals("DAV:")) {
                        String name = webdavPropertyName.getLocalName();
                        Property webdavProperty = webdavNode.getProperty(webdavPropertyNamespace, name);
                        if (name.equals("publicationDate")) {
                            current.setProperty("published", true);
                            try {
                                Date d = new Date();
                                d = PUBLICATIONDATE_FORMAT.parse(webdavProperty.getPropertyAsString());
                                Calendar c = Calendar.getInstance();
                                c.setTime(d);
                                current.setProperty("publicationdate", c);
                            } catch (java.text.ParseException e) {
                            }
                            
                        } else if (name.equals("documentdate")) { 
                            try {
                                Date d = new Date();
                                d = DOCUMENTDATE_FORMAT.parse(webdavProperty.getPropertyAsString());
                                Calendar c = Calendar.getInstance();
                                c.setTime(d);
                                current.setProperty("documentdate", c);
                                current.setProperty("year", c.get(Calendar.YEAR));
                                current.setProperty("month", 1 + c.get(Calendar.MONTH));
                                current.setProperty("day", c.get(Calendar.DAY_OF_MONTH));
                            } catch (java.text.ParseException e) {
                            }
                        } else {
                            Value value = valueFactory.createValue(webdavProperty.getPropertyAsString());
                            current.setProperty(name, value);
                        }
                    } else {
                        /*
                        String name = webdavPropertyName.getLocalName();
                        if (name.equals("creationdate")) {
                            Property webdavProperty = webdavNode.getProperty(webdavPropertyNamespace, name);
                            Date d = new Date();
                            try
                            {
                                d = CREATIONDATE_FORMAT.parse(webdavProperty.getPropertyAsString());
                                Calendar c = Calendar.getInstance();
                                c.setTime(d);
                                current.setProperty("year", c.get(Calendar.YEAR));
                                current.setProperty("month", 1 + c.get(Calendar.MONTH));
                                current.setProperty("day", c.get(Calendar.DAY_OF_MONTH));
                            } catch (java.text.ParseException e) {
                            }
                            
                        }
                        */
                        
                    }
                }
            } else {
                // create 'collection' nodes if they don't exist
                if (!parent.hasNode(nodeName)) {
                    parent.addNode(nodeName);
                }
            }
            session.save();

        } catch (PathNotFoundException e) {
            System.err.println("");
            System.err.println("JCR Path does not exist: " + e.getMessage());
            System.err.println("Unable to create: " + nodeName);
            throw new RuntimeException("Path does not exist: " + e.getMessage());
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
        session.logout();
        //        try {
        //            //TODO: Implement createBackup(file) and restoreBackup(file) in Hippo repo2 server.
        //            //server.createBackup(dumpFile);
        //            
        //            //For the time being just dump the repo contents to the console to verify that it works.
        //            //Node root = session.getRootNode();
        //            //server.dump(root);
        //            
        //            
        //            //server.close();
        //        } catch (RepositoryException e) {
        //            e.printStackTrace();
        //        }
    }

    private String parentPath(String uri) {
        String result = uri.substring(webdavRootUri.length());

        int i = result.lastIndexOf("/");
        result = i == -1 ? result : result.substring(0, i);

        return result;
    }

    private String nodeName(String uri) {
        return uri.substring(uri.lastIndexOf("/") + 1);
    }

    private String getRmiUrl() {
        return "rmi://" + rmiHost + ":" + rmiPort + "/" + jcrRepository;
    }

    private void checkPath() {
        try {
            javax.jcr.Node node = (javax.jcr.Node) session.getRootNode();
            String currentPath = "";

            StringTokenizer st = new StringTokenizer(jcrPath, "/"); 
            
            while (st.hasMoreTokens()) {
                
                String nodeName = st.nextToken();
                
                if (nodeName == null || "".endsWith(nodeName)) {
                    continue;
                }
                System.out.println("Checking for: " + currentPath + "/" + nodeName);

                // add node if it doesn't exist
                if (!node.hasNode(nodeName)) {
                    node.addNode(nodeName);
                    System.out.println("Added node for jcrPath: " + currentPath);
                }
                currentPath += "/" + nodeName;
                
                // shift to child node 
                node = node.getNode(nodeName);
            }
            session.save();

        } catch (RepositoryException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void printConfig(Configuration config) {
        System.out.println("******************************************************");
        System.out.println("******************************************************");
        System.out.println("Exporting from WebDAV: ");
        System.out.println("  Url         : http://" + config.getLocationHost() + ":" + config.getLocationPort());
        System.out.println("  Repository  : " + config.getLocationRootPath());
        System.out.println("  Path        : " + config.getFullPath());
        System.out.println("Importing in JCR:");
        System.out.println("  RMI Url     : " + getRmiUrl());
        System.out.println("  Repository  : " + jcrRepository);
        System.out.println("  Workspace   : " + jcrWorkspace);
        System.out.println("  Path        : " + jcrPath);
        System.out.println("******************************************************");
        System.out.println("******************************************************");
    }
}
