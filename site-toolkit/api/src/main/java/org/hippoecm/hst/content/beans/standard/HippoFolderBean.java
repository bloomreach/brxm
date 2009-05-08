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
package org.hippoecm.hst.content.beans.standard;

import java.util.List;

/**
 * This is a base interface for all beans that represent a folder should implement. When developers implement their own bean which
 * does not extend the standard HippoFolder bean, they should implement this interface. 
 */
public interface HippoFolderBean extends HippoBean{

    List<HippoFolderBean> getFolders();
    
    List<HippoFolderBean> getFolders(boolean sorted);
    
    int getDocumentSize();
    
    List<HippoDocumentBean> getDocuments();
    
    List<HippoDocumentBean> getDocuments(int from, int to);
    
    List<HippoDocumentBean> getDocuments(int from, int to, boolean sorted);
    
    List<HippoDocumentBean> getDocuments(boolean sorted);
}
