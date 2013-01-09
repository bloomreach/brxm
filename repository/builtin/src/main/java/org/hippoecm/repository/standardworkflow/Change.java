/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.standardworkflow;

/**
 * A map from node type name to a list of changes to be performed on that node type
 */
public class Change {

    private static final long serialVersionUID = 1L;

    /**
     * The type of change requested.
     */
    public ChangeType type;

    /**
     * The node or property that is indicated by this change.
     */
    public String relPath;

    /**
     * When the change is a RENAME, the newName denotes the new name (not
     * relative path) of the item.
     */
    public String newName;

    /**
     * When the change is an ADDITION, then the absPath references the
     * property or node to copy the default value from.
     */
    public String absPath;

    /**
     * Constructs a change object given the indicated change type on the item
     * indicated by the relPath.  The meaning of the last parameter is
     * dependent on the change type.
     * @param type indicates a field was added, removed or renamed
     * @param relPath the original JCR name used in the document type, or the new name in case of a field being added.
     * @param parameter in case of a rename action the new JCR name, an absolute JCR path to the default value in case of an
     *   addition of a field or null in case it is not relevant.
     */
    public Change(ChangeType type, String relPath, String parameter) {
        this.type = type;
        this.relPath = relPath;
        switch(type) {
        case ADDITION:
            absPath = parameter;
            break;
        case DROPPED:
            break;
        case RENAMED:
            newName = parameter;
            break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(type);
        sb.append(",");
        sb.append(relPath);
        switch(type) {
        case RENAMED:
            sb.append(",");
            sb.append(newName);
            break;
        case ADDITION:
            sb.append(",");
            sb.append(absPath);
            break;
        }
        return new String(sb);
    }
}
