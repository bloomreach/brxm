/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.core.mapping;

public class LinkImpl implements Link{
    private String uri;
    private boolean external;
    private boolean sameDomain = true;
    
    public LinkImpl(String uri, boolean external, boolean sameDomain) {
       this.uri = uri;
       this.external = external;
       this.sameDomain = sameDomain;    
    }
    
    /* (non-Javadoc)
     * @see org.hippoecm.hst.core.mapping.Link#getUri()
     */
    public String getUri() {
        return uri;
    }
    /* (non-Javadoc)
     * @see org.hippoecm.hst.core.mapping.Link#setUri(java.lang.String)
     */
    public void setUri(String uri) {
        this.uri = uri;
    }
    /* (non-Javadoc)
     * @see org.hippoecm.hst.core.mapping.Link#isExternal()
     */
    public boolean isExternal() {
        return external;
    }
    /* (non-Javadoc)
     * @see org.hippoecm.hst.core.mapping.Link#setExternal(boolean)
     */
    public void setExternal(boolean external) {
        this.external = external;
    }
    /* (non-Javadoc)
     * @see org.hippoecm.hst.core.mapping.Link#isSameDomain()
     */
    public boolean isSameDomain() {
        return sameDomain;
    }
    /* (non-Javadoc)
     * @see org.hippoecm.hst.core.mapping.Link#setSameDomain(boolean)
     */
    public void setSameDomain(boolean sameDomain) {
        this.sameDomain = sameDomain;
    }
}
