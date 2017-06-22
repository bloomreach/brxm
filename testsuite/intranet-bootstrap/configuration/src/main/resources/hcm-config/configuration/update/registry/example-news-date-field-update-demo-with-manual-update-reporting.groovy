/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

/**
 * ExampleNewsDocumentDateFieldUpdateDemoVisitor is a script that does manual node iteration
 * in an original iteration cycle and reports updated node manually in order to be aligned
 * with the built-in batch commit/revert feature of the groovy updater engine
 * for demonstration purpose.
 */
package org.hippoecm.frontend.plugins.cms.admin.updater

import org.onehippo.repository.update.BaseNodeUpdateVisitor
import java.util.*
import javax.jcr.*
import javax.jcr.query.*

class ExampleNewsDocumentDateFieldUpdateDemoVisitor extends BaseNodeUpdateVisitor {

  boolean doUpdate(Node node) {
    log.debug "Visiting node at ${node.path} just as an entry point in this demo."
    
    // new date field value from the current time
    def now = Calendar.getInstance()
    
    // do manual query and node iteration
    def query = node.session.workspace.queryManager.createQuery("//element(*,demosite:newsdocument)", "xpath")
    def result = query.execute()
    
    for (NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext(); ) {
      def newsNode = nodeIt.nextNode()
      newsNode.setProperty("demosite:date", now)
      // report updated to the engine manually here.
      visitorContext.reportUpdated(newsNode.path)
    }
    
    return false
  }

  boolean undoUpdate(Node node) {
    throw new UnsupportedOperationException('Updater does not implement undoUpdate method')
  }

}