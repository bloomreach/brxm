/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.branch;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.HstNodeTypes;


public interface NodeHasher {

    /**
     * <p>
     *  The {@code node} that will get hashed : The {@code node} and all its descendants will get
     *  <ul>
     *      <li>The property {@link HstNodeTypes#HASHABLE_PROPERTY_HASH} property set property set if {@code setHash}
     *      is {@code true}</li>
     *      <li>The property {@link HstNodeTypes#HASHABLE_PROPERTY_UPSTREAM_HASH} property set if {@code setUpstreamHash}
     *      is {@code true}</li>
     *  </ul>
     *  The changes <strong>won't</strong> be persisted
     * </p>
     * @param node the {@code node} that will get hashed including all its descendants
     * @param setHash If {@code setHash} is {@code true} {@link HstNodeTypes#HASHABLE_PROPERTY_HASH} will be set
     * @param setUpstreamHash If {@code setUpstreamHash} is {@code true} {@link HstNodeTypes#HASHABLE_PROPERTY_UPSTREAM_HASH} will be set
     * @return The {@code String} representing the hash
     * @throws BranchException in case a repository or some other exception happens
     */
    String hash(final Node node, final boolean setHash, final boolean setUpstreamHash) throws BranchException;

}
