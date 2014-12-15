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

import java.util.List;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;

/**
 * [robotstxt:robotstxt] > hippo:document, hippostd:publishable, hippostd:publishableSummary
 *   - robotstxt:disallowfacnav (boolean)
 *   + robotstxt:section (robotstxt:section) multiple
 */
@Node(jcrType="robotstxt:robotstxt")
public class Robotstxt extends HippoDocument {

    public List<Section> getSections() {
        return this.getChildBeans("robotstxt:section");
    }

    public boolean isDisallowFacNav() {
        return getProperty("robotstxt:disallowfacnav", true);
    }
}
