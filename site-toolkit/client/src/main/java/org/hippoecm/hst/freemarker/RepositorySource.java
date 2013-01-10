/*
 * Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.freemarker;

public class RepositorySource {
    
    public final static RepositorySource repositorySourceNotFound = new RepositorySource();
    private static final int NOTFOUND_HASHCODE = 1231; // completely arbitrary

  
    private String template;
    private long placeHolderLastModified;
    private boolean notFound = false;
    
    private RepositorySource(){
        this.notFound = true;
    }
    
    public RepositorySource(String template){
        this.template = template;
        this.placeHolderLastModified = System.currentTimeMillis();
        
    }

    public String getTemplate() {
        return template;
    }

    public long getPlaceHolderLastModified() {
        return placeHolderLastModified;
    }

    public boolean isNotFound() {
        return notFound;
    }
    
    @Override
    public boolean equals(Object anObject) {
        if(this == anObject) {
            return true;
        }
        if(anObject instanceof RepositorySource) {
            RepositorySource otherSource = (RepositorySource)anObject;
            if(this.notFound && otherSource.notFound) {
                return true;
            }
            if(this.template == null || otherSource.template == null) {
                return false;
            }
            return this.template.equals(otherSource.template) && this.placeHolderLastModified == otherSource.placeHolderLastModified;
        }
        
        return false;
    }
    
    @Override 
    public int hashCode(){
        if(this.notFound) {
            return NOTFOUND_HASHCODE;
        }
        return this.template.hashCode() ^  (int)this.placeHolderLastModified ;
    }
    
}
