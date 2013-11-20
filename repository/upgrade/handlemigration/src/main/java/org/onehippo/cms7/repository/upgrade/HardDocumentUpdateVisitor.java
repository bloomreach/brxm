package org.onehippo.cms7.repository.upgrade;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.core.version.VersionHistoryRemover;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;

public class HardDocumentUpdateVisitor extends BaseContentUpdateVisitor {

    @Override
    public boolean doUpdate(final Node node) throws RepositoryException {
        log.debug("Migrating {}", node.getPath());
        try {
            final VersionHistory versionHistory = getVersionHistory(node);
            JcrUtils.ensureIsCheckedOut(node, true);
            removeHippoPaths(node);
            removeMixin(node, HippoNodeType.NT_HARDDOCUMENT);
            session.save();
            VersionHistoryRemover.removeVersionHistory(versionHistory);
        } finally {
            session.refresh(false);
        }
        return true;
    }

    /**
     * In case the node is not of type hippo:document the hippo:paths property has
     * no property definition after loading of the new CND. To fix this, we first
     * put the hippostd:relaxed mixin on the node so that the property has a definition.
     * Then we can remove the property.
     */
    private void removeHippoPaths(final Node node) throws RepositoryException {
        boolean removeRelaxed = false;
        if (!node.isNodeType(HippoNodeType.NT_DOCUMENT) && !node.isNodeType(HippoStdNodeType.NT_RELAXED)) {
            node.addMixin(HippoStdNodeType.NT_RELAXED);
            removeRelaxed = true;
        }
        final Property paths = JcrUtils.getPropertyIfExists(node, HippoNodeType.HIPPO_PATHS);
        if (paths != null) {
            paths.remove();
        }
        if (removeRelaxed) {
            node.removeMixin(HippoStdNodeType.NT_RELAXED);
        }
    }

    private VersionHistory getVersionHistory(final Node node) throws RepositoryException {
        final VersionManager versionManager = session.getWorkspace().getVersionManager();
        return versionManager.getVersionHistory(node.getPath());
    }

}
