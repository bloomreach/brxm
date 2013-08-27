/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.standard;

/**
 *  <p>
 *  The abstract base bean that can (not should) be used for all HippoCompound types. 
 *  </p>
 *  <p>
 *  By default there is a getHippoHtml method added. This one can be used for compounds that have an html field. If you have 
 *  a compound that does not contain a html field, you can choose to not extend this abstract class but extend from HippoItem but
 *  make sure you implement the marker {@link HippoCompoundBean} interface
 *  </p>
 */
public abstract class HippoCompound extends HippoItem implements HippoCompoundBean {

    /**
     * @param relPath
     * @return <code>HippoHtml</code> or <code>null</code> if no node exists as relPath or no node of type "hippostd:html"
     */
    public HippoHtml getHippoHtml(String relPath) {
        return getBean(relPath, HippoHtml.class);
    }

}
