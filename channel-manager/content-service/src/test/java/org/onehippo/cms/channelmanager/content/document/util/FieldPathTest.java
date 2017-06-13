/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content.document.util;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class FieldPathTest {

    private FieldPath empty;
    private FieldPath one;
    private FieldPath oneTwo;
    private FieldPath oneTwoThree;
    private FieldPath two;
    private FieldPath nul;
    private FieldPath blank;
    private FieldPath twoThree;

    @Before
    public void setUp() {
        empty = new FieldPath("");
        nul = new FieldPath(null);
        blank = new FieldPath(" ");
        one = new FieldPath("one");
        oneTwo = new FieldPath("one/two");
        oneTwoThree = new FieldPath("one/two/three");
        two = new FieldPath("two");
        twoThree = new FieldPath("two/three");
    }

    @Test
    public void isEmpty() {
        assertThat(empty.isEmpty(), is(true));
        assertThat(nul.isEmpty(), is(true));
        assertThat(blank.isEmpty(), is(false));
        assertThat(one.isEmpty(), is(false));
        assertThat(oneTwo.isEmpty(), is(false));
    }

    @Test
    public void testIs() {
        assertThat(one.is("one"), is(true));
        assertThat(one.is(""), is(false));
        assertThat(nul.is(""), is(false));
        assertThat(one.is("oneTwo"), is(false));
        assertThat(oneTwo.is("one"), is(false));
    }

    @Test
    public void startsWith() {
        assertThat(one.startsWith("one"), is(true));
        assertThat(oneTwo.startsWith("one"), is(true));
        assertThat(oneTwo.startsWith("two"), is(false));
        assertThat(oneTwoThree.startsWith("one"), is(true));
        assertThat(oneTwoThree.startsWith(""), is(false));
        assertThat(empty.startsWith(""), is(false));
        assertThat(blank.startsWith(" "), is(true));
        assertThat(oneTwoThree.startsWith(null), is(false));
    }

    @Test
    public void getFirstId() {
        assertThat(one.getFirstSegment(), is("one"));
        assertThat(oneTwo.getFirstSegment(), is("one"));
        assertThat(oneTwoThree.getFirstSegment(), is("one"));
        assertThat(empty.getFirstSegment(), is(nullValue()));
        assertThat(blank.getFirstSegment(), is(" "));
        assertThat(nul.getFirstSegment(), is(nullValue()));
    }

    @Test
    public void getRemainingIds() {
        assertThat(one.getRemainingSegments(), equalTo(empty));
        assertThat(oneTwo.getRemainingSegments(), equalTo(two));
        assertThat(oneTwoThree.getRemainingSegments(), equalTo(twoThree));
        assertThat(empty.getRemainingSegments(), equalTo(empty));
        assertThat(blank.getRemainingSegments(), equalTo(empty));
        assertThat(nul.getRemainingSegments(), equalTo(empty));
    }

    @Test
    public void equals() {
        assertThat(one.equals(one), is(true));
        assertThat(oneTwo.equals(oneTwo), is(true));
        assertThat(oneTwoThree.equals(oneTwoThree), is(true));
        assertThat(empty.equals(empty), is(true));
        assertThat(blank.equals(blank), is(true));
        assertThat(nul.equals(nul), is(true));
        assertThat(empty.equals(nul), is(true));
        assertThat(empty.equals(blank), is(false));
    }

    @Test
    public void testHashCode() {
        assertThat(one.hashCode(), equalTo(one.hashCode()));
        assertThat(oneTwo.hashCode(), equalTo(oneTwo.hashCode()));
        assertThat(one.hashCode(), not(equalTo(two.hashCode())));
        assertThat(empty.hashCode(), not(equalTo(blank.hashCode())));
        assertThat(empty.hashCode(), equalTo(nul.hashCode()));
    }

    @Test
    public void testToString() {
        assertThat(one.toString(), equalTo("one"));
        assertThat(oneTwo.toString(), equalTo("one/two"));
        assertThat(oneTwoThree.toString(), equalTo("one/two/three"));
        assertThat(empty.toString(), equalTo(""));
        assertThat(nul.toString(), equalTo(""));
        assertThat(blank.toString(), equalTo(" "));
    }
}