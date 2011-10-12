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
package org.hippoecm.checker;

import org.slf4j.Logger;

public class Progress {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private int maximum;
    private int current;
    long timestamp;
    long updateDelay  = 5000;
    long initialDelay = 10000;
    long currentDelay;
    Logger logger = null;

    public Progress() {
        this.maximum = -1;
        timestamp = System.currentTimeMillis();
        currentDelay = initialDelay;
    }

    public Progress(int maximum) {
        this.maximum = maximum;
        timestamp = System.currentTimeMillis();
        currentDelay = initialDelay;
    }
    
    public Progress(Progress parent) {
        this.maximum = -1;
        this.logger = parent.logger;
        timestamp = System.currentTimeMillis();
        currentDelay = initialDelay;
    }

    public Progress(Progress parent, int maximum) {
        this.maximum = maximum;
        this.logger = parent.logger;
        timestamp = System.currentTimeMillis();
        currentDelay = initialDelay;
    }
    
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    
    public void setInitialDelay(long millis) {
        initialDelay = millis;
    }

    public void setUpdateDelay(long millis) {
        updateDelay = millis;
    }

    public void setProgress(int current) {
        this.current = current;
        updateIfNeeded();
    }

    private void updateIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime < timestamp || currentTime - timestamp >= currentDelay) {
            timestamp = currentTime;
            update();
        }
    }

    public boolean needsUpdate() {
        long currentTime = System.currentTimeMillis();
        return currentTime < timestamp || currentTime - timestamp >= currentDelay;
    }

    protected void update() {
        if (maximum >= 0) {
            if (logger == null) {
                System.err.println(current + "/" + maximum);
            } else {
                logger.info(current + "/" + maximum);
            }
        }
    }

    public void close() {
    }
}
