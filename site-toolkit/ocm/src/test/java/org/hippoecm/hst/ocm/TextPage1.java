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
package org.hippoecm.hst.ocm;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Bean;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdDocument;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdHtml;

@Node(jcrType="gettingstarted:textpage", discriminator=false)
public class TextPage1 extends HippoStdDocument {

    protected String title;
    protected HippoStdHtml html;

    @Field(jcrName="gettingstarted:title")
    public String getTitle() {
        return this.title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }

    @Bean(jcrName="gettingstarted:body")
    public HippoStdHtml getHtml() {
        return this.html;
    }
    
    public void setHtml(HippoStdHtml html) {
        this.html = html;
    }
}
