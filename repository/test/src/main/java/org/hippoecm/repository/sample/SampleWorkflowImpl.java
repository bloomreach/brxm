/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.sample;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;

public class SampleWorkflowImpl extends WorkflowImpl implements SampleWorkflow {

    private ArticleDocument article;

    public SampleWorkflowImpl() throws RemoteException {
    }

    public void renameAuthor(String newName) throws WorkflowException, RepositoryException {
        final Session session = getWorkflowContext().getUserSession();
        final Node node = session.getNode("/files/myauthor");
        AuthorDocument author = new AuthorDocument(node);
        getArticle().setAuthorId(author.authorId);
    }

    public ArticleDocument getArticle() throws WorkflowException, RepositoryException {
        if (article == null) {
            article = new ArticleDocument(getNode());
        }
        return article;
    }

}
