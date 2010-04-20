/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.editor.compare;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

public class LCS {

    private LCS() {
    }

    static class Sequence {

        static Sequence NULL = new Sequence(-1, null);

        Sequence predecessor;
        int position;
        int length;

        Sequence(int position, Sequence predecessor) {
            this.predecessor = predecessor;
            this.length = predecessor == null ? 0 : predecessor.length + 1;
            this.position = position;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
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

        Map<T, List<Integer>> index = new HashMap<T, List<Integer>>();
        for (int i = 0; i < a.length; i++) {
            List<Integer> positions = index.get(a[i]);
            if (positions == null) {
                positions = new Vector<Integer>();
                index.put(a[i], positions);
            }
            positions.add(i);
        }

        Sequence[] sequences = new Sequence[a.length + 1];
        for (int i = 0; i <= a.length; i++) {
            sequences[i] = Sequence.NULL;
        }

        // main algorithm

        for (int j = 0; j < b.length; j++) {
            List<Integer> positions = index.get(b[j]);
            if (positions == null) {
                continue;
            }
            ListIterator<Integer> positionIter = positions.listIterator(positions.size());
            while (positionIter.hasPrevious()) {
                int position = positionIter.previous();
                Sequence replacement = new Sequence(position, sequences[position]);
                while (position < a.length && replacement.length >= sequences[position + 1].length) {
                    sequences[position + 1] = replacement;
                    position++;
                }
            }
        }

        // read out

        Sequence sequence = sequences[a.length];
        LinkedList<T> result = new LinkedList<T>();
        while (sequence != Sequence.NULL) {
            result.addFirst(a[sequence.position]);
            sequence = sequence.predecessor;
        }
        return result;
    }

}
