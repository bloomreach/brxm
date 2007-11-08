/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.security;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.namespace.NamespaceResolver;

import javax.security.auth.Subject;
import java.io.File;

/**
 * An <code>AMContext</code> is used to provide context information for the
 * Hippo based <code>AccessManager</code>s.
 *
 * @see AccessManager#init(AMContext)
 */
public class AMContext extends org.apache.jackrabbit.core.security.AMContext {

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
    public AMContext(File physicalHomeDir,
                     FileSystem fs,
                     Subject subject,
                     HierarchyManager hierMgr,
                     NamespaceResolver nsResolver,
                     String workspaceName) {
        super(physicalHomeDir, fs, subject, hierMgr, nsResolver, workspaceName);
    }
}
