/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.monkey;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

class MoveNodeAction extends Action {

    private final String srcAbsPath;
    private final String destAbsPath;

    protected MoveNodeAction(final String srcRelPath, final String destRelPath) {
        super("nodeMove-" + srcRelPath + "=>" + destRelPath);
        this.srcAbsPath = "/test/" + srcRelPath;
        this.destAbsPath = "/test/" + destRelPath;
    }

    @Override
    boolean execute(final Session s) throws RepositoryException {
        if (!s.nodeExists(srcAbsPath)) {
            return false;
        }
        if (s.nodeExists(destAbsPath)) {
            return false;
        }
        if (!s.nodeExists(getParentPath(destAbsPath))) {
            return false;
        }
        s.move(srcAbsPath, destAbsPath);
        return true;
    }

    private String getParentPath(final String path) {
        int offset = path.lastIndexOf('/');
        return path.substring(0, offset);
    }

}
