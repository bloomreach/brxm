/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.standard;

/**
 * The interface which all hippo gallery image implementations should implement
 *
 */
public interface HippoGalleryImageBean extends HippoResourceBean {
    /** 
     * @return the height of the image: if their is no height property available, -1 is returned
     */
    int getHeight();
    
    /**
     * @return the width of the image: if their is no widht property available, -1 is returned
     */
    int getWidth(); 
}
