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
package org.hippoecm.repository.updater;

import java.util.Iterator;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

class SetNodeIterator implements NodeIterator {

    Iterator<UpdaterNode> iter;
    long position = 0;
    long size;

    public SetNodeIterator(Set<UpdaterNode> nodes) {
        iter = nodes.iterator();
        position = -1;
        size = nodes.size();
    }

    public Object next() {
        ++position;
        return iter.next();
    }

    public Node nextNode() {
        ++position;
        return iter.next();
    }

    public long getPosition() {
        return position;
    }

    public long getSize() {
        return size;
    }

    public void skip(long count) {
        while (count > 0)
            next();
    }

    public void remove() {
        iter.remove();
        --size;
    }

    public boolean hasNext() {
        return iter.hasNext();
    }
}
