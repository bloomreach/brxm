/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.deriveddata;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.slf4j.Logger;

class PropertyUpdateLogger {

    private final Logger logger;
    private boolean changed = false;
    private StringBuffer sb = null;
    
    PropertyUpdateLogger(String propertyPath, String propName, Node modified, Logger logger) throws RepositoryException {
        this.logger = logger;
        if (logger.isDebugEnabled()) {
            sb = new StringBuffer();
            sb.append("derived property ");
            sb.append(propertyPath);
            sb.append(" in ");
            sb.append(modified.getPath());
            sb.append(" derived using ");
            sb.append(propName);
            sb.append(" valued ");
        }
    }

    boolean isChanged() {
        return changed;
    }

    public void flush() {
        if (logger.isDebugEnabled()) {
            logger.debug(new String(sb));
        }
    }

    public void created(final Value[] values) throws RepositoryException {
        addValues(values);
        if (logger.isDebugEnabled()) {
            sb.append(" created");
        }
        changed = true;
    }

    public void created(final Value value) throws RepositoryException {
        if (logger.isDebugEnabled()) {
            sb.append(value.getString());
            sb.append(" created");
        }
        changed = true;
    }

    public void overwritten(final Value value) throws RepositoryException {
        if (logger.isDebugEnabled()) {
            sb.append(value.getString());
            sb.append(" overwritten");
        }
        changed = true;
    }

    public void overwritten(final Value[] values) throws RepositoryException {
        addValues(values);
        if (logger.isDebugEnabled()) {
            sb.append(" overwritten");
        }
        changed = true;
    }

    public void unchanged(Value value) throws RepositoryException {
        if (logger.isDebugEnabled()) {
            sb.append(value.getString());
            sb.append(" unchanged");
        }
    }

    public void unchanged(Value[] values) throws RepositoryException {
        addValues(values);
        if (logger.isDebugEnabled()) {
            sb.append(" unchanged");
        }
    }

    private void addValues(Value[] values) throws RepositoryException {
        if (logger.isDebugEnabled()) {
            sb.append("{");
            for (int i = 0; i < values.length; i++) {
                sb.append(i == 0 ? " " : ", ");
                sb.append(values[i].getString());
            }
            sb.append(" }");
        }
    }

    public void failed() {
        if (logger.isDebugEnabled()) {
            sb.append(" failed");
        }
    }

    public void removed() {
        if (logger.isDebugEnabled()) {
            sb.append(" removed");
        }
        changed = true;
    }

    public void skipped() {
        if (logger.isDebugEnabled()) {
            sb.append(" skipped");
        }
    }

}
