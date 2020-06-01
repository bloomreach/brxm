/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: TestMockBean.java 167907 2013-06-17 08:34:55Z mmilicevic $"
 */
public class TestMockBean {

    private static Logger log = LoggerFactory.getLogger(TestMockBean.class);


    public static String existingStaticMethod() {
        return "";
    }


    public String existingMethod() {
        return "";
    }

}
