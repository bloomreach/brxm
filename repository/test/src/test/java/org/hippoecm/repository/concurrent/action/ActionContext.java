/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.repository.concurrent.action;

public final class ActionContext {

    private final String documentBasePath;
    private final String assetBasePath;
    private final String imageBasePath;
    
    public ActionContext(String documentBasePath, String assetBasePath, String imageBasePath) {
        this.documentBasePath = documentBasePath;
        this.assetBasePath = assetBasePath;
        this.imageBasePath = imageBasePath;
    }
    
    public String getDocumentBasePath() {
        return documentBasePath;
    }
    
    public String getAssetBasePath() {
        return assetBasePath;
    }
    
    public String getImageBasePath() {
        return imageBasePath;
    }
    
    public boolean isBasePath(String path) {
        return path.equals(documentBasePath) || path.equals(assetBasePath) || path.equals(imageBasePath);
    }

}
