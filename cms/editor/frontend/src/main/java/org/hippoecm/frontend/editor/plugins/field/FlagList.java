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

public final class FlagList {

    private final BitSet flags = new BitSet();

    public boolean get(int i) {
        return flags.get(i);
    }

    public void set(int i, boolean value) {
        flags.set(i, value);
    }

    public void moveTo(int source, int target) {
        moveBy(source, target - source);
    }

    public void moveUp(int i) {
        moveBy(i, -1);
    }

    public void moveDown(int i) {
        moveBy(i, 1);
    }

    public void remove(int i) {
        final int lastSetBitPlusOne = flags.stream().max().orElse(i) + 1;
        // moves bit i past the last set one
        moveTo(i, lastSetBitPlusOne);
        //  and then clears it
        flags.clear(lastSetBitPlusOne);
    }

    public void moveBy(int i, int n) {
        // Precondition: if n < 0 then i + n > 0
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
