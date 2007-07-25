/*
  THIS CODE IS UNDER CONSTRUCTION, please leave as is until
  work has proceeded to a stable level, at which time this comment
  will be removed.  -- Berry
*/

/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.workflow;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippocms.repository.jr.servicing.Document;
import org.hippocms.repository.jr.servicing.DocumentManager;
import org.hippocms.repository.jr.servicing.ServicingWorkspace;

public final class WorkflowContext {
  Session session;
  WorkflowContext(Session session) {
    this.session = session;
  }
  public Document getDocument(String category, String identifier) throws RepositoryException {
    DocumentManager documentManager = ((ServicingWorkspace)session.getWorkspace()).getDocumentManager();
    return documentManager.getDocument(category, identifier);
  }
}
