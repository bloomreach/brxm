package org.hippoecm.frontend.plugins.cms.dev.updater

import org.onehippo.repository.update.BaseNodeUpdateVisitor
import javax.jcr.Node

public class UpdaterTemplate extends BaseNodeUpdateVisitor {

  public boolean doUpdate(Node node) {
    log.debug("Updating node " + node.getPath())
    throw new UnsupportedOperationException("Updater doesn't implement doUpdate method")
  }

  public boolean undoUpdate(Node node) {
    throw new UnsupportedOperationException("Updater doesn't implement undoUpdate method")
  }

}
