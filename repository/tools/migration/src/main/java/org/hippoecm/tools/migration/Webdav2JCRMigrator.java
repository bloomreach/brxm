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
package org.hippoecm.tools.migration;

import java.util.StringTokenizer;

import javax.jcr.PathNotFoundException;
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
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.tools.migration.jcr.JCRHelper;
import org.hippoecm.tools.migration.webdav.WebdavHelper;


/**
 * The plugin to the wdbp.
 * Parses the configuration for the plugin and initilizes the converter plugin.
 * Contains the main process loop which loops over all WebDAV nodes.
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
    private static final String DEFAULT_DOCUMENT_CONVERTER = "org.hippoecm.tools.migration.SimpleDocumentConverter";

    private static final String BATCH_SIZE_CONFIG = "batchsize";
    private static final String VERBOSE_CONFIG = "verbose";
    private static final int DEFAULT_BATCH_SIZE = 1;



    // ---------------------------------------------------- Instance variables
    private int batchCount = 0;
    private int batchSize = DEFAULT_BATCH_SIZE;

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

    private long startTime;
    private long timeSpendConverting = 0;
    private long timeSpendSaving = 0;
    private boolean verbose;

    //----------------------- webdav
    private HttpState httpState;
    private HttpClient httpClient;

    public void configure(WebdavBatchProcessor processor, Configuration config, PluginConfiguration pluginConfig) {
        startTime = System.currentTimeMillis();
        // Fetch properties
        webdavRootUri = config.getRootUri();

        // Batch size
        String size = pluginConfig.getValue(BATCH_SIZE_CONFIG);
        try {
            batchSize = Integer.parseInt(size);
        } catch (NumberFormatException e) {}

        if(pluginConfig.getValue(VERBOSE_CONFIG).equals("true")) {
            verbose = true;
        }
         
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
        HippoRepository repository;
        try {
            // get the repository
            repository = HippoRepositoryFactory.getHippoRepository(getRmiUrl());

            // login and get session
            session = repository.login(new SimpleCredentials(jcrUser, jcrPass.toCharArray()));

            // todo, add option to set and create workspace
            Workspace ws = session.getWorkspace();
            if (!ws.getName().equals(jcrWorkspace)) {
                throw new RuntimeException("Workspaces not equal. Found: " + ws.getName() + " requested: "
                        + jcrWorkspace);
            }

            // test and create target path
            JCRHelper.checkAndCreatePath(session, jcrPath);

            // webdav
            createHttpClient(config);

            // setup converter
            documentConverter.setup(pluginConfig, session, httpClient);

        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

    }

    public void process(nl.hippo.webdav.batchprocessor.Node webdavNode) throws WebdavBatchProcessorException {
        if (webdavNode.getUri().equals(webdavRootUri)) {
            return;
        }

        // get paths and names
        String jcrParentPath = jcrPath + WebdavHelper.parentPath(webdavNode.getUri().substring(webdavRootUri.length()));
        String nodeName = documentConverter.nameTranslate(WebdavHelper.nodeName(webdavNode.getUri()));
        StringTokenizer t = new StringTokenizer(jcrParentPath, "/");
        StringBuffer path = new StringBuffer();
        while (t.hasMoreTokens()) {
            path.append("/");
            path.append(documentConverter.nameTranslate(t.nextToken()));
        }
        jcrParentPath = path.toString();
        
        
        if (documentConverter.skipJcrPath(jcrParentPath + "/" + nodeName)) {
            return;
        }
        
        try {
            
            javax.jcr.Node parent = (javax.jcr.Node) session.getItem(jcrParentPath);

            if (!webdavNode.isCollection()) {
                // Add content property
                try {
                    long start = System.currentTimeMillis();
                    if(!parent.hasNode(nodeName)) {
                        documentConverter.convertNodeToJCR(webdavNode, nodeName, parent);
                    }
                    timeSpendConverting += (System.currentTimeMillis() - start);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // create nt:unstructured nodes if they don't exist
                if (!parent.hasNode(nodeName)) {
                    javax.jcr.Node n = parent.addNode(nodeName);
                    n.addMixin("mix:referenceable");                
                    }
            }

            batchCount++;
            if ((batchCount % batchSize) == 0 ) {
                long start = System.currentTimeMillis();
                session.save();
                timeSpendSaving += (System.currentTimeMillis() - start);
                System.out.println(batchCount + ". Converting WebDAV node: " + webdavNode.getUri() + " => " + jcrParentPath + "/" + nodeName);
                if(verbose) {
                    long totalTime = (System.currentTimeMillis() - startTime);
                    System.out.println("total time running : \t" + totalTime/1000 + " sec \t" + "100%");
                    System.out.println("average time per/doc : \t" + totalTime/batchCount + " ms ");
                    System.out.println("total time saving : \t" + (timeSpendSaving)/1000 + " sec \t" + (int)(100*(timeSpendSaving+0.1)/totalTime) + "%");
                    System.out.println("total time convert : \t" + (timeSpendConverting)/1000 + " sec \t" + (int)(100*(timeSpendConverting+0.1)/totalTime) + "%");
                }
            }
            
        } catch (PathNotFoundException e) {
            System.err.println("");
            System.err.println("JCR Path does not exist: " + e.getMessage());
            System.err.println("Unable to create: " + nodeName);
            throw new RuntimeException("Path does not exist: " + e.getMessage());
        } catch (RepositoryException e) {
            e.printStackTrace();
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
