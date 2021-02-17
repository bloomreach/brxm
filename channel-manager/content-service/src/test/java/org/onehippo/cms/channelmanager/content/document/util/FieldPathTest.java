/*
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class FieldPathTest {

    private FieldPath empty;
    private FieldPath one;
    private FieldPath one2;
    private FieldPath oneTwo;
    private FieldPath one2Two;
    private FieldPath one2Two2;
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
        one2 = new FieldPath("one[2]");
        oneTwo = new FieldPath("one/two");
        one2Two = new FieldPath("one[2]/two");
        one2Two2 = new FieldPath("one[2]/two[2]");
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
        assertThat(one2.isEmpty(), is(false));
        assertThat(one2Two.isEmpty(), is(false));
    }

    @Test
    public void testIs() {
        assertThat(one.is("one"), is(true));
        assertThat(one.is(""), is(false));
        assertThat(nul.is(""), is(false));
        assertThat(one.is("oneTwo"), is(false));
        assertThat(oneTwo.is("one"), is(false));
        assertThat(one2.is("one[2]"), is(true));
        assertThat(one2.is("one"), is(false));
        assertThat(one2Two.is("one"), is(false));
        assertThat(one2Two.is("one[2]"), is(false));
    }

    @Test
    public void startsWith() {
        assertThat(one.startsWith("one"), is(true));
        assertThat(one.startsWith("on"), is(false));
        assertThat(one.startsWith("blablabla"), is(false));
        assertThat(oneTwo.startsWith("one"), is(true));
        assertThat(oneTwo.startsWith("two"), is(false));
        assertThat(oneTwoThree.startsWith("one"), is(true));
        assertThat(oneTwoThree.startsWith(""), is(false));
        assertThat(empty.startsWith(""), is(false));
        assertThat(empty.startsWith(null), is(false));
        assertThat(blank.startsWith(" "), is(true));
        assertThat(oneTwoThree.startsWith(null), is(false));
        assertThat(one2.startsWith("one[2]"), is(true));
        assertThat(one2.startsWith("one[3]"), is(false));
        assertThat(one2.startsWith("one"), is(true));
        assertThat(one2.startsWith("on"), is(false));
        assertThat(one2.startsWith("oneone"), is(false));
        assertThat(one2Two.startsWith("one[2]"), is(true));
        assertThat(one2Two.startsWith("one"), is(true));
    }

    @Test
    public void getFirstSegment() {
        assertThat(one.getFirstSegment(), is("one"));
        assertThat(oneTwo.getFirstSegment(), is("one"));
        assertThat(oneTwoThree.getFirstSegment(), is("one"));
        assertThat(empty.getFirstSegment(), is(nullValue()));
        assertThat(blank.getFirstSegment(), is(" "));
        assertThat(nul.getFirstSegment(), is(nullValue()));
        assertThat(one2.getFirstSegment(), is("one[2]"));
        assertThat(one2Two.getFirstSegment(), is("one[2]"));
    }

    @Test
    public void getFirstSegmentName() {
        assertThat(one.getFirstSegmentName(), is("one"));
        assertThat(oneTwo.getFirstSegmentName(), is("one"));
        assertThat(oneTwoThree.getFirstSegmentName(), is("one"));
        assertThat(blank.getFirstSegmentName(), is(" "));
        assertThat(one2.getFirstSegmentName(), is("one"));
        assertThat(one2Two.getFirstSegmentName(), is("one"));
    }

    @Test
    public void getFirstSegmentIndex() {
        assertThat(one.getFirstSegmentIndex(), is(1));
        assertThat(oneTwo.getFirstSegmentIndex(), is(1));
        assertThat(oneTwoThree.getFirstSegmentIndex(), is(1));
        assertThat(blank.getFirstSegmentIndex(), is(1));
        assertThat(one2.getFirstSegmentIndex(), is(2));
        assertThat(one2Two.getFirstSegmentIndex(), is(2));
    }

    @Test
    public void getRemainingSegments() {
        assertThat(one.getRemainingSegments(), equalTo(empty));
        assertThat(one2.getRemainingSegments(), equalTo(empty));
        assertThat(oneTwo.getRemainingSegments(), equalTo(two));
        assertThat(oneTwoThree.getRemainingSegments(), equalTo(twoThree));
        assertThat(one2Two.getRemainingSegments(), equalTo(two));
        assertThat(empty.getRemainingSegments(), equalTo(empty));
        assertThat(blank.getRemainingSegments(), equalTo(empty));
        assertThat(nul.getRemainingSegments(), equalTo(empty));
    }

    @Test
    public void getLastSegment() {
        assertThat(one.getLastSegment(), is("one"));
        assertThat(oneTwo.getLastSegment(), is("two"));
        assertThat(oneTwoThree.getLastSegment(), is("three"));
        assertThat(empty.getLastSegment(), is(nullValue()));
        assertThat(blank.getLastSegment(), is(" "));
        assertThat(nul.getLastSegment(), is(nullValue()));
        assertThat(one2.getLastSegment(), is("one[2]"));
        assertThat(one2Two.getLastSegment(), is("two"));
        assertThat(one2Two2.getLastSegment(), is("two[2]"));
    }

    @Test
    public void getLastSegmentName() {
        assertThat(one.getLastSegmentName(), is("one"));
        assertThat(oneTwo.getLastSegmentName(), is("two"));
        assertThat(oneTwoThree.getLastSegmentName(), is("three"));
        assertThat(blank.getLastSegmentName(), is(" "));
        assertThat(one2.getLastSegmentName(), is("one"));
        assertThat(one2Two.getLastSegmentName(), is("two"));
        assertThat(one2Two2.getLastSegmentName(), is("two"));
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
        assertThat(one2.equals(one2), is(true));
        assertThat(one2Two.equals(one2Two), is(true));
        assertThat(one.equals(one2), is(false));
        assertThat(one2Two.equals(oneTwo), is(false));
    }

    @Test
    public void testHashCode() {
        assertThat(one.hashCode(), equalTo(one.hashCode()));
        assertThat(oneTwo.hashCode(), equalTo(oneTwo.hashCode()));
        assertThat(one.hashCode(), not(equalTo(two.hashCode())));
        assertThat(empty.hashCode(), not(equalTo(blank.hashCode())));
        assertThat(empty.hashCode(), equalTo(nul.hashCode()));
        assertThat(one2.hashCode(), equalTo(one2.hashCode()));
        assertThat(one.hashCode(), not(equalTo(one2.hashCode())));
    }

    @Test
    public void testToString() {
        assertThat(one.toString(), equalTo("one"));
        assertThat(oneTwo.toString(), equalTo("one/two"));
        assertThat(oneTwoThree.toString(), equalTo("one/two/three"));
        assertThat(empty.toString(), equalTo(""));
        assertThat(nul.toString(), equalTo(""));
        assertThat(blank.toString(), equalTo(" "));
        assertThat(one2.toString(), equalTo("one[2]"));
        assertThat(one2Two.toString(), equalTo("one[2]/two"));
    }
}
