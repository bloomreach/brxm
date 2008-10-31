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
package org.hippoecm.repository.decorating.checked;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;

public class DocumentManagerDecorator extends AbstractDecorator implements DocumentManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    Session session;
    DocumentManager documentManager;

    protected DocumentManagerDecorator(DecoratorFactory factory, SessionDecorator session, DocumentManager documentManager) {
        super(factory, session);
        this.documentManager = documentManager;
    }

    @Override
    protected void repair(Session upstreamSession) throws RepositoryException {
        documentManager = ((HippoWorkspace)session.getWorkspace()).getDocumentManager();
    }

    public Session getSession() {
        return session;
    }

    public Document getDocument(String category, String identifier) throws MappingException, RepositoryException {
        check();
        return documentManager.getDocument(category, identifier);
    }
}
