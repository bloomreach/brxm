/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.ui.progress;

/**
 * @version "$Id$"
 */
public class Progression {
    private final int progress;
    private final String message;

    /**
     * Create a new Progression value object from a percentage progress value.
     *
     * @param progress The progress in percent from 0 to 100, where 100 means done
     */
    public Progression(int progress) {
        this.progress = progress;
        message = null;
    }

    /**
     * Create a new Progression value object from a percentage progress value and a message
     * describing the current task
     *
     * @param progress The progress in percent from 0 to 100, where 100 means done
     * @param message  The message we'd like to show to end users
     */
    public Progression(int progress, String message) {
        this.progress = progress;
        this.message = message;
    }

    /**
     * @return true iff the progress is done
     */
    public boolean isDone() {
        return progress >= 100;
    }

    public int getProgress() {
        return progress;
    }

    public String getProgressMessage() {
        return message;
    }
}
