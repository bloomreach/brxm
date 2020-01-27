/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.common.content.beans;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.hippoecm.hst.content.annotations.PageModelIgnore;
import org.hippoecm.hst.content.annotations.PageModelIgnoreType;
import org.hippoecm.hst.content.annotations.PageModelProperty;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HippoBeanApiObjectMapperTest {

    @Test
    public void testMapper() throws JsonProcessingException {

        ObjectMapper objectMapper = new PageModelObjectMapperFactory().createPageModelObjectMapper();

        CarImpl car = new CarImpl("name", "bar");

        String serialized = objectMapper.writeValueAsString(car);

        assertTrue(serialized.contains("\"barbar\":\"bar\""));

        assertFalse(serialized.contains("\"name\""));
        assertFalse(serialized.contains("\"type\""));
    }

    private static class CarImpl implements Car {

        private final String name;
        private final String bar;
        private final Type type = new Type();

        public CarImpl(final String name, final String bar) {

            this.name = name;
            this.bar = bar;
        }

        @PageModelIgnore
        @Override
        public String getName() {
            return name;
        }

        @PageModelProperty("barbar")
        public String getBar() {
            return bar;
        }

        public Type getType() {
            return type;
        }
    }

    private interface Car {
        String getName();
    }

    @PageModelIgnoreType
    private static class Type {

        final String type = "type";

        public String getType() {
            return type;
        }
    }

}
