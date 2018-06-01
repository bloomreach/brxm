/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * VersionsCleaner is a script that visits document nodes and cleans up the version history.
 * It can retain retainCount number and daysToKeep days of latest versions.
 * For each node, it checks all its versions: if the age of the version is older
 * than the daysToKeep and there are more versions than retainCount, then it deletes the version.
 */

package org.hippoecm.frontend.plugins.cms.dev.updater

import org.onehippo.repository.update.BaseNodeUpdateVisitor

import javax.jcr.Node
import javax.jcr.version.Version
import javax.jcr.version.VersionHistory
import javax.jcr.version.VersionIterator
import javax.jcr.version.VersionManager

/**
 * Truncates the version history, retaining only the latest
 * ${retainCount} versions.
 */
class VersionsCleaner extends BaseNodeUpdateVisitor {

  /** The default number of versions to retain.*/
  int defaultRetainCount = 2;
  /** The default number of days to keep versions.*/
  int defaultDaysToKeep = 30;
  
  /** The number of versions to retain. Must be at least 1. */
  int retainCount;
  /** The number of days to keep versions. Must be at least 1 to keep history or 0 in which case only retainCount is used. */
  int daysToKeep;
  
  void initialize(Session session) {
    retainCount = parametersMap.get("retainCount", defaultRetainCount)
    daysToKeep = parametersMap.get("daysToKeep", defaultDaysToKeep)
    if (retainCount < 1) {
      retainCount = 1
    }
    if (daysToKeep < 0) {
      daysToKeep = 0
    }
    log.info "VersionsCleaner initialized with parameters: { retainCount: ${retainCount}, daysToKeep: ${daysToKeep} }"
    
  }

  boolean skipCheckoutNodes() {
    return true; // we're changing version history, not current content
  }
  
  boolean doUpdate(Node node) {
    if (node.getPrimaryNodeType().getName().startsWith("hst:")
            || node.getPath().startsWith("/hippo:configuration/")
            || node.getPath().equals("/hippo:namespaces")
            || node.getPath().startsWith("/hippo:namespaces/")) {
      log.debug "Skipping config node ${node.path}"
      return false
    }
    log.debug "Updating node ${node.path}"
    
    // gather versions
    List versions = new ArrayList()
    VersionManager versionManager = node.getSession().getWorkspace().getVersionManager()
    Version baseVersion = versionManager.getBaseVersion(node.getPath())
    VersionHistory versionHistory = versionManager.getVersionHistory(node.getPath())
    VersionIterator allVersions = versionHistory.getAllVersions()
    while (allVersions.hasNext()) {
      Version version = allVersions.nextVersion()
      Calendar created = version.getCreated();
      Calendar daysOld = Calendar.getInstance();
      daysOld.add(Calendar.DAY_OF_MONTH, -daysToKeep);
      if (created.before(daysOld)) {
        if (!version.getName().equals("jcr:rootVersion")) {
          versions.add(version);
        }
      }
    }
    // remove versions
    int removeCount = versions.size() - retainCount
    boolean remove = removeCount > 0
    if (remove) {
      log.info "Removing ${removeCount} versions of node ${node.path}"
    }
    while (removeCount > 0) {
      Version version = versions.remove(0);
      versionHistory.removeVersion(version.getName())
      removeCount--;
    }
    return remove
  }
  
  boolean undoUpdate(Node node) {
    throw new UnsupportedOperationException('Updater does not implement undoUpdate method')
  }

}