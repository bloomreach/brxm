package org.hippoecm.repository.jackrabbit;

import org.apache.jackrabbit.core.id.NodeId;

public interface HandleListener {

    void handleModified(NodeId handleId);
}
