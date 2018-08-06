/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.diff;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class LCS {

    private LCS() {
    }

    static class Sequence {

        static Sequence NULL = new Sequence(-1, null);

        final Sequence predecessor;
        final int position;
        final int length;

        Sequence(int position, Sequence predecessor) {
            this.predecessor = predecessor;
            this.length = predecessor == null ? 0 : predecessor.length + 1;
            this.position = position;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append('(');
            if (predecessor != null) {
                sb.append(predecessor);
                sb.append(", ");
            }
            sb.append(length);
            sb.append(" [");
            sb.append(position);
            sb.append("]");
            sb.append(')');
            return sb.toString();
        }
    }

    public enum ChangeType {
        ADDED, REMOVED, INVARIANT
    }

    /**
     * Elemental change in an array of values.
     * Serializable if T is serializable.
     *
     * @param <T>
     */
    static public class Change<T> implements Serializable {

        private final ChangeType type;
        private final T value;

        public Change(T value, ChangeType type) {
            this.value = value;
            this.type = type;
        }

        public T getValue() {
            return value;
        }

        public ChangeType getType() {
            return type;
        }
    }

    /**
     * Constructs the minimal changeset to turn the first array into the second one.
     * The returned changeset is guaranteed to be serializable.
     *
     * @param <T> the type of the array elements
     * @param a the first array
     * @param b the second array
     * @return the minimal changeset
     */
    public static <T extends Serializable> List<Change<T>> getSerializableChangeSet(T[] a, T[] b) {
        return getChangeSet(a, b);
    }

    /**
     * Constructs the minimal changeset to turn the first array into the second one.
     * The returned changeset is only serializable when T is serializable.
     *
     * @param <T> the type of the array elements
     * @param a the first array
     * @param b the second array
     * @return the minimal changeset
     */
    public static <T> List<Change<T>> getChangeSet(T[] a, T[] b) {
        final List<T> lcs = getLongestCommonSubsequence(a, b);

        final LinkedList<Change<T>> operations = new LinkedList<>();

        final Iterator<T> commonIter = lcs.iterator();
        final Iterator<T> oldValueIter = Arrays.asList(a).iterator();
        final Iterator<T> newValueIter = Arrays.asList(b).iterator();
        T nextNewValue = null;
        if (newValueIter.hasNext()) {
            nextNewValue = newValueIter.next();
        }
        while (commonIter.hasNext()) {
            T nextValue = commonIter.next();
            while (oldValueIter.hasNext()) {
                T oldValue = oldValueIter.next();
                if (oldValue.equals(nextValue)) {
                    break;
                } else {
                    operations.add(new Change<>(oldValue, ChangeType.REMOVED));
                }
            }
            while (nextNewValue != null && !nextNewValue.equals(nextValue)) {
                operations.add(new Change<>(nextNewValue, ChangeType.ADDED));
                if (newValueIter.hasNext()) {
                    nextNewValue = newValueIter.next();
                } else {
                    nextNewValue = null;
                }
            }
            operations.add(new Change<>(nextValue, ChangeType.INVARIANT));
            nextNewValue = null;
            if (newValueIter.hasNext()) {
                nextNewValue = newValueIter.next();
            }
        }
        while (oldValueIter.hasNext()) {
            T oldValue = oldValueIter.next();
            operations.add(new Change<>(oldValue, ChangeType.REMOVED));
        }
        while (nextNewValue != null) {
            operations.add(new Change<>(nextNewValue, ChangeType.ADDED));
            if (newValueIter.hasNext()) {
                nextNewValue = newValueIter.next();
            } else {
                nextNewValue = null;
            }
        }
        return operations;
    }

    /**
     * Find the longest common subsequence between arrays a and b.
     * The algorithm that's implemented creates an index of the items in the smallest array
     * and iterates over the largest one.  During this iteration, for each sub-array consisting of
     * the elements 0..j of the small array (j < |smallest|), the sequence with most common
     * elements is maintained.
     * <p>
     * The execution time and memory usage are linear for typical inputs, but will be quadratic in
     * the worst case.
     */
    public static <T> List<T> getLongestCommonSubsequence(T[] a, T[] b) {
        if (b.length < a.length) {
            return getLongestCommonSubsequence(b, a);
        }

        // prepare index and list of sequences

        final Map<T, List<Integer>> index = new HashMap<>();
        for (int i = 0; i < a.length; i++) {
            List<Integer> positions = index.computeIfAbsent(a[i], k -> new ArrayList<>());
            positions.add(i);
        }

        final Sequence[] sequences = new Sequence[a.length + 1];
        for (int i = 0; i <= a.length; i++) {
            sequences[i] = Sequence.NULL;
        }

        // main algorithmCMS-11290

        for (final T item : b) {
            final List<Integer> positions = index.get(item);
            if (positions == null) {
                continue;
            }
            final ListIterator<Integer> positionIter = positions.listIterator(positions.size());
            while (positionIter.hasPrevious()) {
                int position = positionIter.previous();
                final Sequence replacement = new Sequence(position, sequences[position]);
                while (position < a.length && replacement.length >= sequences[position + 1].length) {
                    sequences[position + 1] = replacement;
                    position++;
                }
            }
        }

        // read out

        Sequence sequence = sequences[a.length];
        final LinkedList<T> result = new LinkedList<>();
        while (sequence != Sequence.NULL) {
            result.addFirst(a[sequence.position]);
            sequence = sequence.predecessor;
        }
        return result;
    }

}
