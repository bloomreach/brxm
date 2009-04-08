#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
#set($hyphen = '-')
#set($empty = '')
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
package ${package}.ocm;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdDocument;

public class GeneralPage extends HippoStdDocument {

    protected String title;
    protected String summary;

    @Field(jcrName="${rootArtifactId.replace($hyphen,$empty)}:title")
    public String getTitle() {
        return this.title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    @Field(jcrName="${rootArtifactId.replace($hyphen,$empty)}:summary")
    public String getSummary() {
        return this.summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    
}
