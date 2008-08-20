/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.tools.importer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;

public class ExternalContentImport {

    final static String SVN_ID = "$Id$";
    
    private static final String JCR_RMI_URL = "repository.rmiurl";
    private static final String JCR_USER = "repository.username";
    private static final String JCR_PASS = "repository.password";
    private static final String JCR_PATH = "repository.path";
    private static final String FILE_PATH = "filesystem.path";
    private static final String DOCUMENT_CONVERTER = "contentimporter";
    
    private static final String DEFAULT_DOCUMENT_CONVERTER = "org.hippoecm.tools.importer.SimpleXmlImporter";


    private final String rmiurl;
    private final String username;
    private final String password;
    private String repopath;
    private final String filepath;

    private final Session session;
    private ContentImporter contentImporter;
    
    private final Node baseNode;
    private Node currentNode;

    private static Logger log = Logger.getLogger(ExternalContentImport.class);

    public ExternalContentImport(Configuration config) throws IOException, RepositoryException {
        rmiurl = config.getString(JCR_RMI_URL);
        username = config.getString(JCR_USER);
        password = config.getString(JCR_PASS);
        repopath = config.getString(JCR_PATH);
        filepath = config.getString(FILE_PATH);

        // document converter
        String converter = config.getString(DOCUMENT_CONVERTER);
        if (converter == null || "".equals(converter)) {
            converter = DEFAULT_DOCUMENT_CONVERTER;
        }

        try {
            contentImporter = (ContentImporter) Class.forName(converter).newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("ContentImporter class not found: " + e.getMessage());
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("ContentImporter class not instantiated: " + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("ContentImporter class access exception: " + e.getMessage());
        }

        // add trainling slash
        if (!repopath.endsWith("/")) {
            repopath = repopath + "/";
        }

        File file = new File(filepath);
        log.info("Repository : " + rmiurl);
        log.info("user       : " + username);
        log.info("File path  : " + file.getCanonicalPath());
        log.info("Repo path : " + repopath);
        

        // test and setup connection and login
        HippoRepository repository;
        // get the repository
        repository = HippoRepositoryFactory.getHippoRepository(rmiurl);

        // login and get session
        session = repository.login( new SimpleCredentials(username, password.toCharArray()));

        // setup converter
        contentImporter.setup(config);
        baseNode = contentImporter.createPath(session.getRootNode(), repopath);
        session.save();
     
        // start the import
        currentNode = baseNode;
        contentImport(file);
        session.save();

    }

    /**
     * Recursively import from the current file
     * @param file
     * @throws RepositoryException 
     */
    private void contentImport(File file) throws IOException, RepositoryException {
        if (file.isFile() && !file.getName().startsWith(".")) {
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            try {
                contentImporter.convertDocToJCR(currentNode, file.getName(), bis);
            } finally {
                try {
                    bis.close();
                    fis.close();
                } catch (IOException e) {
                    log.warn("Error while closing inputstream for " + file.getAbsolutePath());
                }
            }
        } else if (file.isDirectory() && !file.getName().startsWith(".")) {
            try {
                currentNode = contentImporter.createFolder(currentNode, file.getName());
            } catch (RepositoryException e) {
                log.error("Unable to create collection: " + file.getName());
            }
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (!contentImporter.skipPath(files[i].getAbsolutePath())) {
                    contentImport(files[i]);
                }
            }
            currentNode = currentNode.getParent();
            session.save();
        }
    }
}
