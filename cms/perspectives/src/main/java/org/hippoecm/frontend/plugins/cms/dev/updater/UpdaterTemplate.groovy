package org.hippoecm.frontend.plugins.cms.dev.updater

import org.onehippo.repository.update.BaseNodeUpdateVisitor
import javax.jcr.Node

class UpdaterTemplate extends BaseNodeUpdateVisitor {

  boolean doUpdate(Node node) {
    log.debug "Updating node ${node.path}"
    return false
  }

  boolean undoUpdate(Node node) {
    throw new UnsupportedOperationException('Updater does not implement undoUpdate method')
  }

}
