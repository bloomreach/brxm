/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.linking;

import org.hippoecm.hst.configuration.hosting.Mount;

public class RewriteContext {

    private final String path;
    private final Mount mount;
    private final boolean canonical;
    private final boolean navigationStateful;

    /**
     * @param path the path to use for link creation, not allowed to be <code>null</code>
     * @param mount the {@link org.hippoecm.hst.configuration.hosting.Mount} to use for link creation,
     *              not allowed to be <code>null</code>
     * @param canonical <code>true</code> when the link should be canonical
     * @param navigationStateful  <code>true</code> when the link should be navigationStateful
     * @throws org.hippoecm.hst.core.linking.RewriteContextException if <code>path</code> or <code>mount</code> is null
     */
    public RewriteContext(final String path,
                          final Mount mount,
                          final boolean canonical,
                          final boolean navigationStateful) throws RewriteContextException {
        if (path == null || mount == null) {
            throw new RewriteContextException("path and mount are not allowed to be null.");
        }
        this.path = path;
        this.mount = mount;
        this.canonical = canonical;
        this.navigationStateful = navigationStateful;
    }

    /**
     * @return the path to try the link creation for
     */
    public String getPath() {
        return path;
    }

    /**
     * @return the mount to try the link creation for
     */
    public Mount getMount() {
        return mount;
    }

    /**
     * @return <code>true</code> when a canonical link should be created
     */
    public boolean isCanonical() {
        return canonical;
    }


    /**
     * @return <code>true</code> when the link must be navigationStateful
     */
    public boolean isNavigationStateful() {
        return navigationStateful;
    }

    @Override
    public String toString() {
        return "RewriteContext{" +
                "path='" + path + '\'' +
                ", mount=" + mount +
                ", canonical=" + canonical +
                ", navigationStateful=" + navigationStateful +
                '}';
    }
}
