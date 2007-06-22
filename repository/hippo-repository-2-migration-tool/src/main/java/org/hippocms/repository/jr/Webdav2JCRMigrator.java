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

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;

import nl.hippo.webdav.batchprocessor.Configuration;
import nl.hippo.webdav.batchprocessor.Plugin;
import nl.hippo.webdav.batchprocessor.PluginConfiguration;
import nl.hippo.webdav.batchprocessor.WebdavBatchProcessor;
import nl.hippo.webdav.batchprocessor.WebdavBatchProcessorException;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;

/*
 * TODO:
 * - extenalize strings for properties and nodetypes
 * - rename 2 => To
 * - refactor long methods
 * - commit!
 * - optimize document parsing (parse only once)
 */


public class Webdav2JCRMigrator implements Plugin {

    // ---------------------------------------------------- Constants
    private static final String JCR_RMI_HOST = "jcr.rmi.host";
    private static final String JCR_RMI_PORT = "jcr.rmi.port";

    private static final String JCR_REPOSITORY = "jcr.repository";
    private static final String JCR_WORKSPACE = "jcr.workspace";
    private static final String JCR_USER = "jcr.rmi.user";
    private static final String JCR_PASS = "jcr.rmi.pass";
    private static final String JCR_PATH = "jcr.path";

    private static final String DOCUMENT_CONVERTER_CONFIG = "documentconverter";
    private static final String DEFAULT_DOCUMENT_CONVERTER = "org.hippocms.repository.jr.SimpleDocumentConverter";
    


    // ---------------------------------------------------- Instance variables
    //----------------------- rmi
    private String rmiHost;
    private String rmiPort;

    //----------------------- jcr
    private String jcrRepository;
    private String jcrWorkspace;
    private String jcrUser;
    private String jcrPass;
    private String jcrPath;

    private Session session;
    private String webdavRootUri;
    
    private DocumentConverter documentConverter;
    

    //----------------------- webdav
    private HttpState httpState;
    private HttpClient httpClient;

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

        // document converter
        String converter = pluginConfig.getValue(DOCUMENT_CONVERTER_CONFIG);
        if (converter == null || "".equals(converter)) {
            converter = DEFAULT_DOCUMENT_CONVERTER;
        }
        
        try {
            documentConverter = (DocumentConverter) Class.forName(converter).newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("DocumentConverter class not found: " + e.getMessage());
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("DocumentConverter class not instantiated: " + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("DocumentConverter class access exception: " + e.getMessage());
            }
        
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


        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (NotBoundException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        // webdav
        createHttpClient(config);

        // setup converter
        documentConverter.setup(pluginConfig, session, httpClient);
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
                try {
                    documentConverter.convertNodeToJCR(webdavNode, nodeName, parent);
                } catch (Exception e) {
                    e.printStackTrace();
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
        //} catch (IOException e) {
        //    e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }



    /**
     * Inhereted
     */
    public boolean requiresNodeOrderPreservation() {
        return true;
    }

    //-------------------------------------------------- JCR Related Methods

    /**
     * Logout from the JCR session
     */
    public void postprocess() {
        session.logout();
    }

    /**
     * Contruct the rmi url
     * @return the rmi url
     */
    private String getRmiUrl() {
        return "rmi://" + rmiHost + ":" + rmiPort + "/" + jcrRepository;
    }

    /**
     * Check the JCR import root path and create the path if it doesn't exist
     */
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

    //-------------------------------------------------- WebDAV Related Methods
    
    /**
     * Create and setup the httpClient
     * @param config
     */
    private void createHttpClient(Configuration config) {
        httpState = new HttpState();
        httpState.setAuthenticationPreemptive(true);
        httpState.setCredentials(null, config.getLocationHost(), new UsernamePasswordCredentials(config
                .getAuthenticationUsername(), config.getAuthenticationPassword()));
        httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
        httpClient.setState(httpState);
        HostConfiguration hostConfiguration = new HostConfiguration();
        hostConfiguration.setHost(config.getLocationHost(), config.getLocationPort());
        httpClient.setHostConfiguration(hostConfiguration);   
    }
    
    /**
     * Get the parent path of a uri
     * @param uri
     * @return the parent path
     */
    private String parentPath(String uri) {
        String result = uri.substring(webdavRootUri.length());

        int i = result.lastIndexOf("/");
        result = i == -1 ? result : result.substring(0, i);

        return result;
    }

    /**
     * Find the nodeName from a specific uri
     * @param uri
     * @return the nodeName
     */
    private String nodeName(String uri) {
        return uri.substring(uri.lastIndexOf("/") + 1);
    }
    
    /**
     * Print the configuration to the screnen
     * @param config
     */
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
        System.out.println("Converter:");
        System.out.println("  Plugin      : " + documentConverter.getClass().getSimpleName());
        System.out.println("******************************************************");
        System.out.println("******************************************************");
    }
}
