/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.jsonassert;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * <p>
 *     Tests for getting some fingerspitzengefuhl with JSONAssert. The result from these tests are that the
 *     {@link JSONCompareMode} we should use for the Page Model API is {@link JSONCompareMode#STRICT_ORDER} : with this
 *     mode, {@link JSONAssert#assertEquals} passes in case
 *     <ul>
 *         <li>
 *            extra objects or fields are added to the Page Model API
 *         </li>
 *         <li>
 *            objects and/or fields are reshuffled
 *         </li>
 *     </ul>
 *     and it fails in case
 *     <ul>
 *         <li>
 *            values change, for example links get rewritten to a different value
 *         </li>
 *         <li>
 *            a field gets removed
 *         </li>
 *         <li>
 *            array get changed (in value or order)
 *         </li>
 *     </ul>
 *     The above covers also the rules which can be seen as compatible Page Model API changes.
 * </p>
 */
public class ApiCompatibilityJsonAssertTest {

    @Test
    public void same_JSON_passes_all_JSONCompareModes() throws Exception {

        final Ferrari ferrari = new Ferrari("convertible", new Owner("john"));

        String actual = new ObjectMapper().writeValueAsString(ferrari);

        System.out.println(actual);

        final String expected = getSimpleJson("simple.json");

        JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT);
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
    }

    @Test
    public void different_values_JSON_passes_none_JSONCompareModes() throws Exception {

        final Ferrari ferrari = new Ferrari("convertible", new Owner("john"));

        String actual = new ObjectMapper().writeValueAsString(ferrari);

        System.out.println(actual);

        final String expected = getSimpleJson("simple_different_value.json");

        JSONAssert.assertNotEquals(expected, actual, JSONCompareMode.LENIENT);
        JSONAssert.assertNotEquals(expected, actual, JSONCompareMode.STRICT);
        JSONAssert.assertNotEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertNotEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
    }

    /**
     * Strict order is just arrays, not for reordered objects
     */
    @Test
    public void reordered_objects_JSON_passes_all_JSONCompareModes() throws Exception {

        final Ferrari ferrari = new Ferrari("convertible", new Owner("john"));

        String actual = new ObjectMapper().writeValueAsString(ferrari);

        final String expected = getSimpleJson("simple_reordered_objects.json");

        JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT);
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
    }

    @Test
    public void extra_field_in_actual_passes_LENIENT_and_STRICT_ORDER() throws Exception {

        final Ferrari ferrari = new Ferrari("convertible", new Owner("john"));

        String actual = new ObjectMapper().writeValueAsString(ferrari);

        {
            final String expected = getSimpleJson("simple_without_type.json");

            JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT);
            JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
            JSONAssert.assertNotEquals(expected, actual, JSONCompareMode.STRICT);
            JSONAssert.assertNotEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
        }
        {
            final String expected = getSimpleJson("simple_without_owner.json");

            JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT);
            JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
            JSONAssert.assertNotEquals(expected, actual, JSONCompareMode.STRICT);
            JSONAssert.assertNotEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
        }
    }

    @Test
    public void missing_field_in_actual_passes_none_JSONCompareMode() throws Exception {

        // car misses origin
        final Car car = new Car("convertible", "bmw", new Owner("john"));

        // now actual does not have field 'origin' which is present in simple.json
        String actual = new ObjectMapper().writeValueAsString(car);

        {
            final String expected = getSimpleJson("simple.json");

            JSONAssert.assertNotEquals(expected, actual, JSONCompareMode.LENIENT);
            JSONAssert.assertNotEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
            JSONAssert.assertNotEquals(expected, actual, JSONCompareMode.STRICT);
            JSONAssert.assertNotEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
        }

    }


    @Test
    public void reordered_array_colors_JSON_passes_LENIENT_and_NON_EXTENSIBLE() throws Exception {

        final Ferrari ferrari = new Ferrari("convertible", new Owner("john"));

        // now actual does not have field 'origin' which is present in simple.json
        String actual = new ObjectMapper().writeValueAsString(ferrari);

        {
            final String expected = getSimpleJson("simple_reorder_array_colors.json");

            JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT);
            JSONAssert.assertNotEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
            JSONAssert.assertNotEquals(expected, actual, JSONCompareMode.STRICT);
            JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
        }

    }

    @Test
    public void reordered_array_previous_owners_JSON_passes_LENIENT_and_NON_EXTENSIBLE() throws Exception {

        final Ferrari ferrari = new Ferrari("convertible", new Owner("john"));

        // now actual does not have field 'origin' which is present in simple.json
        String actual = new ObjectMapper().writeValueAsString(ferrari);

        {
            final String expected = getSimpleJson("simple_reorder_array_previous_owners.json");

            JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT);
            JSONAssert.assertNotEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
            JSONAssert.assertNotEquals(expected, actual, JSONCompareMode.STRICT);
            JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
        }

    }

    private String getSimpleJson(final String filename) throws IOException {
        InputStream inputStream = ApiCompatibilityJsonAssertTest.class.getResourceAsStream(filename);

        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

    public static class Ferrari extends Car {

        public Ferrari(final String type, final Owner owner) {
            super(type, "ferrari", owner);
        }

        public String getOrigin() {
            return "Italy";
        }
    }

    public static class Car {

        private final String type;
        private final String brand;
        private final Owner owner;
        private final String[] colors = new String[] {"blue", "red"};
        private final Owner[] previousOwners = new Owner[] {new Owner("Mark"), new Owner("Peter")};

        public Car(final String type, final String brand, final Owner owner){

            this.type = type;
            this.brand = brand;
            this.owner = owner;
        }

        public String getType() {
            return type;
        }

        public String getBrand() {
            return brand;
        }

        public Owner getOwner() {
            return owner;
        }

        public String[] getColors() {
            return colors;
        }

        public Owner[] getPreviousOwners() {
            return previousOwners;
        }
    }

    public static class Owner {

        private final String name;

        public Owner(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
