/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.hst.content.beans.standard;

import static junit.framework.Assert.*;
import org.junit.Test;
import static org.easymock.EasyMock.*;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;

/**
 * Tests {@link org.hippoecm.hst.content.beans.standard.HippoItem HippoItem}.
 *
 */
public class HippoItemTest {

    @Test
    public void returnsFalseWhenBothPathsAreNull() {
        HippoItem item1 = new HippoItem();
        HippoItem item2 = new HippoItem();

        assertFalse(item1.equals(item2));
        assertFalse("Multiple calls to equals() are consistent", item1.equals(item2));
    }

    @Test
    public void returnsTrueWhenComparedToSelf() {
        HippoItem item = new HippoItem();

        assertTrue("equals() is reflexive", item.equals(item));
    }

    @Test
    public void returnsFalseWhenComparedToNull() {
        HippoItem item1 = new HippoItem();
        HippoItem item2 = null;

        assertFalse(item1.equals(item2));
    }

    @Test
    public void returnsFalseWhenNotComparedToHippoBean() {
        HippoItem item1 = new HippoItem();
        Object item2 = new Object();

        assertFalse(item1.equals(item2));
    }

    @Test
    public void returnsFalseWhenPathsAreDifferent() {
        JCRValueProvider mockValueProvider1 = createMock(JCRValueProvider.class);
        JCRValueProvider mockValueProvider2 = createMock(JCRValueProvider.class);

        HippoItem item1 = new HippoItem();
        item1.valueProvider = mockValueProvider1;

        HippoItem item2 = new HippoItem();
        item2.valueProvider = mockValueProvider2;

        expect(mockValueProvider1.getPath()).andReturn("/content/documents/1");
        expect(mockValueProvider2.getPath()).andReturn("/content/documents/2");

        replay(mockValueProvider1, mockValueProvider2);

        assertFalse(item1.equals(item2));

        verify(mockValueProvider1, mockValueProvider2);
    }

    @Test
    public void returnsTrueWhenPathsAreTheSame() {
        JCRValueProvider mockValueProvider1 = createMock(JCRValueProvider.class);
        JCRValueProvider mockValueProvider2 = createMock(JCRValueProvider.class);

        HippoItem item1 = new HippoItem();
        item1.valueProvider = mockValueProvider1;

        HippoItem item2 = new HippoItem();
        item2.valueProvider = mockValueProvider2;

        expect(mockValueProvider1.getPath()).andReturn("/content/documents");
        expect(mockValueProvider2.getPath()).andReturn("/content/documents");

        replay(mockValueProvider1, mockValueProvider2);

        assertTrue(item1.equals(item2));
        assertTrue("equals() is symmetric", item2.equals(item1));

        verify(mockValueProvider1, mockValueProvider2);
    }

    @Test
    public void returnsFalseWhenOneOfThePathsIsNull() {
        JCRValueProvider mockValueProvider = createMock(JCRValueProvider.class);

        HippoItem item1 = new HippoItem();

        HippoItem item2 = new HippoItem();
        item2.valueProvider = mockValueProvider;

        replay(mockValueProvider);

        assertFalse(item1.equals(item2));

        verify(mockValueProvider);
    }

    
}
