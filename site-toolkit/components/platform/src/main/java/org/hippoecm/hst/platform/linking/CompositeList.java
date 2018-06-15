/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.linking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class CompositeList<E> implements List<E> {
    
    private List<E> delegatee;
    
    // combining multiple lists, where the order of the elements is accoring the order in which the lists are added
    public CompositeList(List<List<E>> lists)  {
        delegatee = new ArrayList<E>();
        for(List<E> list : lists ){
            delegatee.addAll(list);
        }
    }

    public boolean add(E e) {
        return delegatee.add(e);
    }

    public void add(int index, E element) {
        delegatee.add(index, element);
    }

    public boolean addAll(Collection<? extends E> c) {
        return delegatee.addAll(c);
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        return delegatee.addAll(index, c);
    }

    public void clear() {
        delegatee.clear();
    }

    public boolean contains(Object o) {
        return delegatee.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return delegatee.containsAll(c);
    }

    public E get(int index) {
        return delegatee.get(index);
    }

    public int indexOf(Object o) {
        return delegatee.indexOf(o);
    }

    public boolean isEmpty() {
        return delegatee.isEmpty();
    }

    public Iterator<E> iterator() {
        return delegatee.iterator();
    }

    public int lastIndexOf(Object o) {
        return delegatee.lastIndexOf(o);
    }

    public ListIterator<E> listIterator() {
        return delegatee.listIterator();
    }

    public ListIterator<E> listIterator(int index) {
        return delegatee.listIterator(index);
    }

    public boolean remove(Object o) {
        return delegatee.remove(o);
    }

    public E remove(int index) {
        return delegatee.remove(index);
    }

    public boolean removeAll(Collection<?> c) {
        return delegatee.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return delegatee.retainAll(c);
    }

    public E set(int index, E element) {
        return delegatee.set(index, element);
    }

    public int size() {
        return delegatee.size();
    }

    public List<E> subList(int fromIndex, int toIndex) {
        return delegatee.subList(fromIndex, toIndex);
    }

    public Object[] toArray() {
        return delegatee.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return delegatee.toArray(a);
    }

    
    
}
