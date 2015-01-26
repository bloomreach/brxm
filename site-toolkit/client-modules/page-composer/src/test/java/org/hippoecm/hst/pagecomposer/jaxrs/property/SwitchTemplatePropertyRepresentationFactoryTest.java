/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.property;

import java.util.Arrays;
import java.util.Map;

import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentPropertyRepresentation;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class SwitchTemplatePropertyRepresentationFactoryTest {

    @Test
    public void test_asKeySortedMap() {
        final String[] unSortedKeys = new String[]{"a", "z", "1"};
        final String[] values = new String[]{"12", "y", "x"};
        final Map<String, String> sortedMap = SwitchTemplatePropertyRepresentationFactory.asKeySortedMap(unSortedKeys, values);
        final String[] sortedKeys = unSortedKeys.clone();
        Arrays.sort(sortedKeys);
        assertArrayEquals(sortedKeys, sortedMap.keySet().toArray(new String[0]));
    }

    /**
     * Plain alphabetical ordering of for example ftl template file names layout.ftl, layout-variant1.ftl and
     * layout-variant2.ftl results in
     * <ol>
     *     <li>layout-variant1.ftl</li>
     *     <li>layout-variant2.ftl</li>
     *     <li>layout.ftl</li>
     * </ol>
     * however, desired is (like linux FS / intellij)
     *
     * <ol>
     *     <li>layout.ftl</li>
     *     <li>layout-variant1.ftl</li>
     *     <li>layout-variant2.ftl</li>
     * </ol>
     */
    @Test
    public void test_asKeySortedMap_with_ftl_extensions() {
        final String[] unsortedKeys = new String[]{"layout-variant2.ftl", "layout-variant1.ftl", "layout.ftl"};
        final String[] values = new String[]{"12", "y", "x"};
        final Map<String, String> sortedMap = SwitchTemplatePropertyRepresentationFactory.asKeySortedMap(unsortedKeys, values);

        final String[] expectedArray = new String[]{"layout.ftl", "layout-variant1.ftl","layout-variant2.ftl"};

        assertArrayEquals(expectedArray, sortedMap.keySet().toArray(new String[0]));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_asKeySortedMap_incorrect_length() {
        final String[] keys = new String[]{"a", "z", "1"};
        final String[] values = new String[]{"12", "y"};
        SwitchTemplatePropertyRepresentationFactory.asKeySortedMap(keys, values);
    }

    //FREE_MARKER_DISPLAY_NAME_COMPARATOR

    @Test
    public void test_PropertyRepresentationSorted() {
        ContainerItemComponentPropertyRepresentation prop = new ContainerItemComponentPropertyRepresentation();

        prop.setDropDownListValues(new String[] {"val3", "val1", "val2"});
        prop.setDropDownListDisplayValues(new String[] {"display3", "display1", "display2"});

        SwitchTemplatePropertyRepresentationFactory.sortDropDownByDisplayValue(prop);

        final String[] sortedValues = prop.getDropDownListValues();
        assertArrayEquals(sortedValues, new String[]{"val1", "val2", "val3"});
        final String[] sortedDisplayValues = prop.getDropDownListDisplayValues();
        assertArrayEquals(sortedDisplayValues, new String[]{"display1", "display2", "display3"});

    }

}
