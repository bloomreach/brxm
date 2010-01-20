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

    /**
     * @return the list of <code>HippoFolderBean</code> below this <code>HippoFolderBean</code> and an empty list of no folders present
     */
    
    List<HippoFolderBean> getFolders();
    
    /**
     * 
     * @param sorted if true, the list of folder will be returned sorted by their default compareTo(HippoBean) 
     * @return the sorted list of <code>HippoFolderBean</code> below this <code>HippoFolderBean</code> and an empty list if no folders present
     *
     */
    List<HippoFolderBean> getFolders(boolean sorted);
    
    /**
     * @return the number of documents in this folder
     */
    int getDocumentSize();
    
    /**
     * <b>note</b> when only a subset of the documents is needed, and the total number is large, better use {@link #getDocumentIterator(Class)} as this is a lazy proxied iterator
     * @return the list of <code>HippoDocumentBean</code> below this <code>HippoFolderBean</code> and an empty list if no documents present
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
     * 
     * <b>note</b> when only a subset of unsorted documents is needed, and the total number is large, better use {@link #getDocumentIterator(Class)} as this is a lazy proxied iterator
     * @param sorted
     * @return the (if (sorted) sorted) list of <code>HippoDocumentBean</code> below this <code>HippoFolderBean</code> and an empty list if no documents present
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
