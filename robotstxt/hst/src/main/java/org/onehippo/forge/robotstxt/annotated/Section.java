/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.robotstxt.annotated;

import java.util.Arrays;
import java.util.List;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoItem;

/**
  * [robotstxt:section]
  *   - robotstxt:useragent (string) mandatory
  *   - robotstxt:disallow (string) multiple 
 */
@Node(jcrType = "robotstxt:section")
public class Section extends HippoItem {

    private final static String DISALLOW_PREFIX = "Disallow: ";
    
    public String getUserAgent() {
        return this.getProperty("robotstxt:useragent");
    }

    public List<String> getDisallows() {
        final String[] disallows = this.getProperty("robotstxt:disallow");
        List<String> disallowsList = Arrays.asList(disallows);
        return disallowsList;
    }

    /**
     * @deprecated This function implements part of the rendering. Rendering is supposed
     * to happen in the JSP, so it is *recommended* to use the section's "disallows"
     * attribute directly from the JSP. This function is left here for backwards
     * compatibility.
     * @return a pre-formatted string, containing all "Disallow:" lines for this section.
     */
    @Deprecated
    public String getDisallowFragment() {
        StringBuilder sb = new StringBuilder();
        for (String s : getDisallows()) {
            sb.append(DISALLOW_PREFIX).append(s).append(System.getProperty("line.separator"));
        }
        return sb.toString();
    }

}
