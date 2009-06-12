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
package org.hippoecm.hst.jackrabbit.ocm;

import java.util.List;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

@Node(jcrType="testproject:textpage", discriminator=false)
public class TextPage2 extends HippoStdDocument {

    protected String title;
    protected String uuid;
    
    // Currently, JackRabbit OCM Collection annotation is working 
    // for field declaration only, not working for getter.
    // Furthermore, this kind of collection mapping assumes 
    // the following node structure
    //
    // - gettingstarted:textpage
    //   - gettingstarted:comments
    //     - gettingstarted:comment
    //     - gettingstarted:comment
    //     - ...
    //
    @Collection(jcrName="testproject:comments")
    protected List<TextPageComment> comments;

    @Field(jcrName="testproject:title")
    public String getTitle() {
        return this.title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }

    @Field(uuid=true)
    public String getUuid() {
        return this.uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public List<TextPageComment> getComments() {
        return this.comments;
    }
    
    public void setComments(List<TextPageComment> comments) {
        this.comments = comments;
    }
}
