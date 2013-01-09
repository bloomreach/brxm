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
package org.hippoecm.hst.mock.content.beans.standard;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;

/**
 * Mock implementation of {@link org.hippoecm.hst.content.beans.standard.HippoBeanIterator} for testing purposes.
 */
public class MockHippoBeanIterator implements HippoBeanIterator {

    private final List<HippoBean> hippoDocuments;

    private final Iterator<HippoBean> iterator;

    private int position;

    public MockHippoBeanIterator() {
        this(Collections.<HippoBean>emptyList());
    }

    public MockHippoBeanIterator(List<HippoBean> hippoDocuments) {
        if (hippoDocuments == null) {
            throw new IllegalArgumentException("HippoDocuments cannot be null");
        }
        
        this.hippoDocuments = hippoDocuments;
        this.iterator = this.hippoDocuments.iterator();
    }

    public HippoBean nextHippoBean() {
        position ++;
        return iterator.next();
    }

    public void skip(int skipNum) {
        for (int i = 0; i < skipNum; i++) {
            iterator.next();
        }
        position += skipNum;
    }

    public long getSize() {
        return hippoDocuments.size();
    }

    public long getPosition() {
        return position;
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public HippoBean next() {
        return nextHippoBean();
    }

    public void remove() {
        iterator.remove();
    }
}
