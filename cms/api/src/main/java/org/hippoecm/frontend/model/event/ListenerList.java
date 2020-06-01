/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.model.event;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class ListenerList<T extends EventListener> extends LinkedList<T> {

    private static final long serialVersionUID = 1L;

    private class SafeIterator implements Iterator<T> {
        
        Iterator<T> upstream;
        T next;
        
        SafeIterator() {
            upstream = new ArrayList<T>(ListenerList.this).iterator();
            next = null;
        }

        void load() {
            while (next == null && upstream.hasNext()) {
                T candidate = upstream.next();
                if (ListenerList.this.contains(candidate)) {
                    next = candidate;
                }
            }
        }

        public boolean hasNext() {
            load();
            return next != null;
        }

        public T next() {
            load();
            if (next == null) {
                throw new NoSuchElementException();
            }
            T result = next;
            next = null;
            return result;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    @Override
    public Iterator<T> iterator() {
        return new SafeIterator();
    }

}
