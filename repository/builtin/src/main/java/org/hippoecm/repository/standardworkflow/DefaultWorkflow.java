/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.standardworkflow;

import java.rmi.RemoteException;
import java.util.Locale;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Localized;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;

/**
 * The interface DefaultWorkflow implements a number of work-flow steps that are common to many type of documents.
 * Most calls, like delete actually defer to the #FolderWorkflow of the container document directly above this document
 * to implement the functionality.
 * A class implementing DefaultWorkflow is returned by one of the calls in #WorkflowManager returning a #Workflow.
 */
public interface DefaultWorkflow extends CopyWorkflow {

    /**
     * Permanently removes the document from the repository.  Unlike the #archive method, this makes it impossible
     * to resurrect the document from version history, even though the document may occupy resources in the version
     * history.
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public void delete()
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Archives a document, which makes it impossible to view or edit a document from the CMS, nor will it be accessible to
     * other programs or web-sites.  However its history MAY still be available through an archive location and the document can
     * be resurrected using that location.
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public void archive()
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Renames the named document to the given new name.  Documents with no localized name will show up with this name in the
     * CMS and site.  However the localized name may be used in case there is a more appropriate name available (e.g. due to
     * specific language locale).  The name of the document is however always the same for each locale and is used in programmatic
     * environments and may also be used in URLs.
     * @param newName the new name the document should get
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public void rename(String newName)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Gives the document a specific new name for the specific location.  A location may be a language, region or other
     * factor that would require a different name (like live or preview site), see #Localized.  A document can only have a
     * single normal name (see #rename), but multiple localized names.
     * @param locale a specification is which case to use the document name
     * @param newName the new name to be used for the specification
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public void localizeName(Localized locale, String newName)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
    
    /**
     * Gives the document a specific new name for the specific location.  This is a convenience method for
     * <code>localizeName(Localized.getInstance(locale), newName)</code>.
     * @param locale the language locale for which to give the document a new name
     * @param newName the new name to be used for the locale
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public void localizeName(Locale locale, String newName)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Gives the document a specific new name for a generic localized specification.  This is not the current default locale,
     * but a fall-back locale, which is active when no other localized specification matches.  So if there is a English
     * name specified, and only a name set by this localizeName method, then when retrieving a Dutch name, the name set by
     * this localizeName method is retrieved.  If this method was not used to set a different name, the actual JCR node name
     * is returned, which has some limitations.
     * @param newName the new name to be used
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public void localizeName(String newName)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Identical to {@link #localizeName(String)}, except that all existing localized names will be removed.
     * @param newName the new name
     * @throws WorkflowException indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public void replaceAllLocalizedNames(String newName)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Moved this document from it current container document, to a new location.  The target container document should also
     * implement a work-flow suitable to contain documents, typically both source and destination container documents implement
     * the #FolderWorkflow interface.  In the target location, the document will contain the new name as specific.  This method
     * cannot be used to rename documents, the document must actually change location.
     * @param target the destination folder or directory
     * @param newName the new name to be used for this document.
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public void move(Document target, String newName)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
