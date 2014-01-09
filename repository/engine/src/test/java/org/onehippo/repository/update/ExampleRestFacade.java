/**
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.update;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Example facade class supposed to invoke REST service.
 */
public class ExampleRestFacade {

    public ExampleRestFacade() {
    }

    public String getSomeData() throws MalformedURLException {
        // try to use File and URL which are blacklisted by GroovyUpdaterClassLoader.
        File dir = new File(System.getProperty("user.home"));
        URL url = new URL("http://www.onehippo.org");
        // get something from url
        return "something";
    }

}
