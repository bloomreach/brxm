/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
public interface HippoFolderBean extends HippoBean, HippoTranslated {

    /**
     * @return the list of <code>HippoFolderBean</code> below this <code>HippoFolderBean</code> and an empty list of no folders present
     */
    
    List<HippoFolderBean> getFolders();
    
    /**
     * 
     * @param sorted if true, the list of folder will be returned sorted by their default {@link HippoBean#compareTo(HippoBean)}
     * @return the sorted list of <code>HippoFolderBean</code> below this <code>HippoFolderBean</code> and an empty list if no folders present
     *
     */
    List<HippoFolderBean> getFolders(boolean sorted);
    
    /**
     * @return the number of documents in this folder
     */
    int getDocumentSize();
    
    /**
     * This method returns the {@link List} of {@link HippoDocumentBean}s in the order they are in the repository. If you need the {@link List} to be sorted according 
     * the {@link HippoDocumentBean#compareTo(HippoBean)}, use {@link #getDocuments(boolean))} with arg <code>true</code>
     * <b>note</b> when only a subset of the documents is needed, and the total number is large, better use {@link #getDocumentIterator(Class)} as this is a lazy proxied iterator
     * @return the list of {@link HippoDocumentBean}s below this {@link HippoFolderBean} and an empty list if no documents present
     */
    List<HippoDocumentBean> getDocuments();
    
    /**
     * Returns a view of the portion of the list of HippoDocumentBean between the specified <code>from</code>, inclusive, and <code>to<code>, exclusive. (If from and to are equal, the returned list is empty.)
     * <b>note</b> when only a subset of the documents is needed, and the total number is large, better use {@link #getDocumentIterator(Class)} as this is a lazy proxied iterator 
     * @param from (inclusive)
     * @param to (exclusive)
     * @return the sublist of <code>HippoDocumentBean</code> below this <code>HippoFolderBean</code> and an empty list when original list is empty or invalid range
     */
    List<HippoDocumentBean> getDocuments(int from, int to);
    
    /**
     * Returns a view of the portion of the list of HippoDocumentBean between the specified <code>from</code>, inclusive, and <code>to<code>, exclusive. (If from and to are equal, the returned list is empty.)
     * <b>note</b> when only a subset of unsorted documents is needed, and the total number is large, better use {@link #getDocumentIterator(Class)} as this is a lazy proxied iterator 
     * @param from (inclusive)
     * @param to (exclusive)
     * @param sorted boolean whether list to get sublist from needs to be sorted
     * @return the sublist of possibly sorted of <code>HippoDocumentBean</code> below this <code>HippoFolderBean</code> and an empty list when original list is empty or invalid range
     */
    List<HippoDocumentBean> getDocuments(int from, int to, boolean sorted);
    
    /**
     * This method enables to get the {@link List} of {@link HippoDocumentBean}s according their {@link HippoBean#compareTo(HippoBean)}. If you need the {@link List} to be ordered the way the documents are ordered in the repository, use {@link #getDocuments()}.
     * <b>note</b> when only a subset of unsorted documents is needed, and the total number is large, better use {@link #getDocumentIterator(Class)} as this is a lazy proxied iterator
     * @param sorted indicates whether the List should be sorted
     * @return if <code>sorted</code> is true, the sorted list of {@link HippoDocumentBean}s below this {@link HippoFolderBean}, where the sorting is according {@link HippoBean#compareTo(HippoBean)},  If <code>sorted</code> is false, the same list as {@link #getDocuments()} is returned. An empty list is returned if no documents are present. 
     */
    List<HippoDocumentBean> getDocuments(boolean sorted);
    
    /**
     * Facility method to get all documents directly below this folder that result in a HippoBean of class or subclass clazz. 
     * <b>note</b> when only a subset of the documents is needed, and the total number is large, better use {@link #getDocumentIterator(Class)} as this is a lazy proxied iterator
     * @param <T> Any Object that implements a HippoDocumentBean
     * @param beanMappingClass a class implementing <code>{@link HippoDocumentBean}<code>. This functions as a filter
     * @return the list documents in this folder of type <T>
     */
    <T> List<T> getDocuments(Class<T> beanMappingClass);
    
    /**
     * Lazy loading iterator that fetches Documents only when asked for it. This is much more efficient then all the{@link #getDocuments()}
     * methods (also with arguments) as they fetch <b>all</b> HippoDocumentBeans directly.
     * 
     * @param <T> Any Object that implements a HippoDocumentBean
     * @param beanMappingClass a class implementing <code>{@link HippoDocumentBean}<code>. This functions as a filter
     * @return A lazy loading iterator returning documents in this folder of type <T>
     */
    <T> HippoDocumentIterator<T> getDocumentIterator(Class<T> beanMappingClass);
    
      
}
