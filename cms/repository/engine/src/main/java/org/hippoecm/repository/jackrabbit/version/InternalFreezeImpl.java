/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.jackrabbit.version;

import org.apache.jackrabbit.core.version.InternalFreeze;
import org.apache.jackrabbit.core.version.InternalVersionItem;

/**
 * Implements a <code>InternalFreeze</code>
 */
abstract class InternalFreezeImpl extends InternalVersionItemImpl implements InternalFreeze {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * The parent item
     */
    private final InternalVersionItem parent;

    /**
     * Creates a new <code>InternalFreezeImpl</code>
     *
     * @param vMgr
     * @param parent
     */
    protected InternalFreezeImpl(AbstractVersionManager vMgr, NodeStateEx node, InternalVersionItem parent) {
        super(vMgr, node);
        this.parent = parent;
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersionItem getParent() {
        return parent;
    }

}
