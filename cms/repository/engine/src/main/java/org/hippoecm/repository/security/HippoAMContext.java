package org.hippoecm.repository.security;

import java.io.File;

import javax.security.auth.Subject;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

/**
 * An <code>AMContext</code> is used to provide context information for an
 * <code>AccessManager</code>.
 *
 * @see AccessManager#init(AMContext)
 */
public class HippoAMContext extends AMContext {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * NodeTypeRegistry for resolving superclass node types
     */
    private final NodeTypeRegistry ntReg;
    
    /**
     * SessionItemStateManager for fetching attic states
     */
    private final SessionItemStateManager itemMgr;
    
    /**
     * Creates a new <code>AMContext</code>.
     *
     * @param physicalHomeDir the physical home directory
     * @param fs              the virtual jackrabbit filesystem
     * @param subject         subject whose access rights should be reflected
     * @param hierMgr         hierarchy manager
     * @param nsResolver      namespace resolver
     * @param workspaceName   workspace name
     */
    public HippoAMContext(File physicalHomeDir,
                     FileSystem fs,
                     Subject subject,
                     HierarchyManager hierMgr,
                     SessionItemStateManager itemMgr,
                     NamespaceResolver nsResolver,
                     String workspaceName,
                     NodeTypeRegistry ntReg) {
        super(physicalHomeDir, fs, subject, hierMgr, nsResolver, workspaceName);
        this.ntReg = ntReg;
        this.itemMgr = itemMgr;
    }

    /**
     * Returns the NodeTypeRegistry
     *
     * @return the NodeTypeRegistry
     */
    public NodeTypeRegistry getNodeTypeRegistry() {
        return ntReg;
    }
    
    /**
     * Returns the SessionItemStateManager associated with the user session
     * @return the SessionItemStateManager
     */
    public SessionItemStateManager getSessionItemStateManager() {
        return itemMgr;
    }
}
