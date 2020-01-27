/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.frontend.editor.plugins.field;

import java.util.BitSet;
import java.util.stream.IntStream;

/**
 * Represents a list of flags. Flags can be set to true or false, can be moved up and down and removed.
 */
public final class FlagList {

    private final BitSet flags = new BitSet();

    /**
     * Returns {@code true} if flag at index i is set and {@code false} otherwise.
     *
     * @param i index of flag
     * @return if flag i is set
     * @throws IndexOutOfBoundsException if i < 0
     */
    public boolean get(int i) {
        return flags.get(i);
    }

    /**
     * Sets flag at index i to the given value
     *
     * @param i     index of flag
     * @param value value for flag with index i
     * @throws IndexOutOfBoundsException if i < 0
     */
    public void set(int i, boolean value) {
        flags.set(i, value);
    }

    /**
     * Moves the flag with the source index to the target index.
     * Example:<br/>
     * [0: true, 1: false, 2: true] -> moveTo (1, 0) ->  [0: false, 1: true, 2: true]
     *
     * @param sourceIndex source index
     * @param targetIndex target index
     * @throws IndexOutOfBoundsException if the sourceIndex < 0 or targetIndex < 0
     */
    public void moveTo(int sourceIndex, int targetIndex) {
        moveBy(sourceIndex, targetIndex - sourceIndex);
    }

    /**
     * Moves flag with index i one up (i.e. to index i - 1).
     *
     * @param i index of flag to move
     * @throws IndexOutOfBoundsException if i <= 0
     */
    public void moveUp(int i) {
        moveBy(i, -1);
    }

    /**
     * Moves flag with index i one up (i.e. to index i + 1).
     *
     * @param i index of flag to move
     * @throws IndexOutOfBoundsException if i < 0
     */
    public void moveDown(int i) {
        moveBy(i, 1);
    }

    /**
     * Removes flag with index i from the list
     *
     * @param i index to remove
     * @throws IndexOutOfBoundsException if i < 0
     */
    public void remove(int i) {
        final int lastSetBitPlusOne = flags.stream().max().orElse(i) + 1;
        // moves bit i past the last set one
        moveTo(i, lastSetBitPlusOne);
        //  and then clears it
        flags.clear(lastSetBitPlusOne);
    }

    /**
     * Moves flag with index i by n positions, where n can be positive or negative.
     * If n is negative then i must be >= n.
     *
     * @param i index of flag to move
     * @param n nr of positions to move
     * @throws IndexOutOfBoundsException if i < 0 or if i + n < 0
     */
    public void moveBy(int i, int n) {
        IntStream.range(0, Math.abs(n))
                .map(n > 0
                        ? j -> i + j
                        : j -> i - j - 1
                )
                .forEach(k -> {
                    final boolean v = flags.get(k);
                    flags.set(k, flags.get(k + 1));
                    flags.set(k + 1, v);
                });
    }

}
