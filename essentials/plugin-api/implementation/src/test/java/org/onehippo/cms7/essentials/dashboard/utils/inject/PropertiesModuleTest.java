/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils.inject;

import org.apache.wicket.util.string.Strings;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class PropertiesModuleTest extends BaseTest{

    private static Logger log = LoggerFactory.getLogger(PropertiesModuleTest.class);

    @Inject
    PropertyTestObj instance;

    @Test
    public void testConfigure() throws Exception {
        assertTrue(!Strings.isEmpty(instance.getValue()));
        assertTrue(!Strings.isEmpty(instance.getName()));
        log.info("instance {}", instance);

    }

    /**
     * @version "$Id$"
     */
    public static class PropertyTestObj {



        @Inject
        @Named("instruction.message.file.delete")
        private String value;

        @Inject
        @Named("instruction.message.file.copy")
        private String name;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("PropertyTestObj{");
            sb.append("value='").append(value).append('\'');
            sb.append(", name='").append(name).append('\'');
            sb.append('}');
            return sb.toString();
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }
}
