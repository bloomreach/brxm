package org.hippoecm.frontend.plugins.cms.dev.updater

import org.onehippo.repository.update.BaseUpdater

import javax.jcr.Node

public class UpdaterTemplate extends BaseUpdater {

  public boolean update(Node node) {
    throw new UnsupportedOperationException("Updater doesn't implement update method")
  }

  public boolean revert(Node node) {
    throw new UnsupportedOperationException("Updater doesn't implement revert method")
  }

}
