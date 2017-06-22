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
package org.hippoecm.frontend.plugins.cms.admin.updater

import org.onehippo.repository.update.BaseNodeUpdateVisitor
import javax.jcr.Node

class UpdaterTemplate extends BaseNodeUpdateVisitor {

  def session

  void initialize(Session session) {
    this.session = session
  }
  
  boolean doUpdate(Node node) {
    log.debug "Visiting translated node ${node.path}"

    def updated = false
    def relPath = node.path.substring("/content/documents/demosite/".length())
    def translationId = node.getProperty("hippotranslation:id").getString()

    if (translationId?.trim()) {
      [ "de", "fr", "it", "nl" ].each { locale ->
        def translatedNodePath = "/content/documents/demosite_" + locale + "/" + relPath
        log.debug "TranslatedNodePath: ${translatedNodePath}"

        if (this.session.nodeExists(translatedNodePath)) {
          def translatedNode = this.session.getNode(translatedNodePath)
          translatedNode.setProperty("hippotranslation:id", translationId)
          translatedNode.setProperty("hippotranslation:locale", locale)
          log.debug "Updated translated node ${translatedNode.path}"
          updated = true
        }
      }
    }
    
    return updated
  }

  boolean undoUpdate(Node node) {
    throw new UnsupportedOperationException('Updater does not implement undoUpdate method')
  }

}