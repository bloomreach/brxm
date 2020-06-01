/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.translation;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;

/**
 * Workflow on translated documents. (hippotranslation:translated)
 * <p>
 * The {@link Workflow#hints()} method returns the following information:
 * <ul>
 *   <li><b>locale</b>: the locale of the document
 *   <li><b>locales</b>: a Set<String> of locale names in which
 *      the document or folder is available
 *   <li><b>available</b>: the Set<String> of locale names to
 *      which this document can be translated.
 */
public interface TranslationWorkflow extends Workflow {

    /**
     * Create a new translation of the document.  Only valid when the containing folder
     * of the current document has been translated.
     * 
     * @param language
     * @param name the translated name of the document
     * @return the newly created document
     * 
     * @throws WorkflowException when a translation already exists or no containing folder was found. 
     */
    Document addTranslation(String language, String name)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Adds the provided document as a translation of the current document or folder.
     * 
     * @param language
     * @param document
     * 
     * @throws WorkflowException when a translation already exists. 
     */
    void addTranslation(String language, Document document)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

}
