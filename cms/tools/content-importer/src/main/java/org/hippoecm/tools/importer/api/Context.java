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
package org.hippoecm.tools.importer.api;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Interface for the content importers.
 */
public interface Context {

    final static String SVN_ID = "$Id$";

    /**
     * The content conversion method. This method is called for each 
     * file and the method should parse the (xml) document and create 
     * a repository document as child of parent.
     * @param context
     * @param resource
     * @throws RepositoryException
     * @throws IOException
     */
    Node createDocument(Content source) throws IOException, RepositoryException;

    Node createFolder(Content source) throws RepositoryException;

    Node createPath(String path) throws RepositoryException;

    String buildPath(String name) throws RepositoryException;

}
