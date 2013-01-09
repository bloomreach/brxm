/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.development.content.names;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamesFactory {

    static final Logger log = LoggerFactory.getLogger(NamesFactory.class);

    /** Reads the list of names from the given source into the specified set. */
    public static Names newNames() {
        // read names from source
        InputStream in = NamesFactory.class.getResourceAsStream("dist-all-last.properties");
        BufferedReader fin = new BufferedReader(new InputStreamReader(in));
        HashSet<String> names = new HashSet<String>();
        String line;
        try {
            while (true) {
                // read name entry
                line = fin.readLine();
                if (line == null) {
                    break;
                }
                StringTokenizer st = new StringTokenizer(line);
                while (st.hasMoreTokens()) {
                    String token = st.nextToken().toLowerCase();

                    // ignore entries with invalid characters
                    int len = token.length();
                    boolean valid = true;
                    for (int i = 0; i < len; i++) {
                        char c = token.charAt(i);
                        if (c < 'a' || c > 'z') {
                            valid = false;
                            break;
                        }
                    }
                    if (!valid) {
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Parsed name: " + token);
                    }
                    names.add(token);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                fin.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return new Names(names);
    }
}
