/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.migration;

import java.io.IOException;

import org.junit.Test;

public class TestEsv2Yaml {

    @Test
    public void main() throws IOException, EsvParseException {
        String init = System.getProperty("init");
        String src = System.getProperty("src");
        String target = System.getProperty("target");
        if (src != null && target != null) {
            if (init != null) {
                Esv2Yaml.main(new String[]{init, src, target});
            } else {
                Esv2Yaml.main(new String[]{src, target});
            }
        }
    }
}
