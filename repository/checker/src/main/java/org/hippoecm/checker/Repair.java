package org.hippoecm.checker;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.persistence.pool.Access;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle;
import org.apache.jackrabbit.core.state.ItemStateException;

class Repair {
    enum RepairStatus {
        CLEAN, PENDING, RECHECK, FAILURE
    }
    private RepairStatus currentStatus;

    Repair(Access access) {
        currentStatus = RepairStatus.CLEAN;
    }

    boolean report(RepairStatus statusChange) {
        switch (currentStatus) {
            case CLEAN:
                switch (statusChange) {
                    case PENDING:
                    case RECHECK:
                    case FAILURE:
                        currentStatus = statusChange;
                        break;
                }
                return false;
            case PENDING:
                switch (statusChange) {
                    case RECHECK:
                    case FAILURE:
                        currentStatus = statusChange;
                        break;
                }
                return false;
            case RECHECK:
                switch (statusChange) {
                    case RECHECK:
                        return false;
                    case FAILURE:
                        currentStatus = statusChange;
                        break;
                }
                return true;
            case FAILURE:
                return true;
            default:
                return true;
        }
    }
    Collection<Action> actions = new LinkedList<Action>();

    void perform(DatabaseDelegate storage) {
        for (Action action : actions) {
            try {
                action.perform(storage);
            } catch (SQLException ex) {
                ex.printStackTrace(System.err);
            }
        }
    }

    void unlistChild(RepairStatus statusChange, UUID parent, UUID child) {
        if (report(statusChange))
            return;
        actions.add(new UnlistChild(parent, child));
    }

    void fixParent(RepairStatus statusChange, UUID child, UUID parent) {
        if (report(statusChange))
            return;
    }

    void removeNode(RepairStatus statusChange, UUID node) {
        if (report(statusChange))
            return;
        actions.add(new RemoveNode(node));
    }

    void removeReference(RepairStatus statusChange, UUID source, UUID target) {
        if (report(statusChange))
            return;
    }

    private abstract class Action {
        abstract void perform(DatabaseDelegate storage) throws SQLException;
    }

    private class RemoveNode extends Action {
        UUID node;

        RemoveNode(UUID node) {
            this.node = node;
        }

        @Override
        void perform(DatabaseDelegate storage) throws SQLException {
            storage.access.destroyBundle(node);
        }
    }
    
    private class UnlistChild extends Action {
        UUID node;
        UUID child;
        UnlistChild(UUID node, UUID child) {
            this.node = node;
            this.child = child;
        }

        @Override
        void perform(DatabaseDelegate storage) throws SQLException {
            try {
                NodePropBundle bundle = storage.access.loadBundle(new NodeId(node.toString()));
                bundle.markOld();
                for (Iterator<NodePropBundle.ChildNodeEntry> iter = bundle.getChildNodeEntries().iterator(); iter.hasNext();) {
                    NodePropBundle.ChildNodeEntry childEntry = iter.next();
                    if (DatabaseDelegate.create(childEntry.getId()).equals(child)) {
                        iter.remove();
                        break;
                    }
                }
                storage.access.storeBundle(bundle);
            } catch (ItemStateException ex) {
                ex.printStackTrace();
            }
        }
    }
}
