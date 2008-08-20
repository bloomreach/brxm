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

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.configuration.Configuration;

/**
 * Interface for the content importers.
 */
public interface ContentImporter {
    
    final static String SVN_ID = "$Id$";

    /**
     * Setup and configure the converter. The import.properties file is 
     * passed as config.
     * @param config
     * @throws RepositoryException
     */
    void setup(Configuration config) throws RepositoryException;

    /**
     * The content conversion method. This method is called for each 
     * file and the method should parse the (xml) doucment and create 
     * a repository document as child of parent.
     * @param parent
     * @param fileName
     * @param content
     * @throws RepositoryException
     * @throws IOException
     */
    void convertDocToJCR(Node parent, String fileName, InputStream content)
            throws RepositoryException, IOException;
    

    /**
     * Create a document as child of the parent (folder) of type with name. 
     * The method should handle encoding and create the handle as well as 
     * setting the initial properties for the workflow.
     * @param parent
     * @param name
     * @param type
     * @return
     * @throws RepositoryException
     */
    public Node createDocument(Node parent, String name, String type) throws RepositoryException;
    
    /**
     * Create a repository folder.
     * @param parent
     * @param name
     * @return
     * @throws RepositoryException
     */
    Node createFolder(Node parent, String name) throws  RepositoryException ;

    /** 
     * Creates the full path as folders
     */
    Node createPath(Node rootNode, String path) throws  RepositoryException ;
    
    /** 
     * getNodeType for some name name
     */
    String getNodeType(String name);

    /**
     * Helper method to translate (node)names e.g. Frankrijk -> France
     * @param name the string to translate
     * @return the translated string
     */
    String nameTranslate(String name);

    /**
     * Skip source paths.
     * @param path the absolute file path
     * @return
     */
    boolean skipPath(String path);
}
