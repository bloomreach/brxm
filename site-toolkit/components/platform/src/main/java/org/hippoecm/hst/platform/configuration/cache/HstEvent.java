/*
 * Copyright 2013-2023 Bloomreach
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
package org.hippoecm.hst.platform.configuration.cache;

public class HstEvent {

    private final String nodePath;
    private final boolean propertyEvent;

    /**
     * @param nodePath the node nodePath of the event
     * @param propertyEvent whether the event was a property change or a node event
     */
    public HstEvent(final String nodePath, final boolean propertyEvent) {
        if (nodePath == null) {
            throw new IllegalArgumentException("nodePath is not allowed to be null");
        }
        this.nodePath = nodePath;
        this.propertyEvent = propertyEvent;
    }

    public String getNodePath() {
        return nodePath;
    }

    public boolean isPropertyEvent() {
        return propertyEvent;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HstEvent hstEvent = (HstEvent) o;

        if (propertyEvent != hstEvent.propertyEvent) {
            return false;
        }
        if (!nodePath.equals(hstEvent.nodePath)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = nodePath.hashCode();
        result = 31 * result + (propertyEvent ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "HstEvent{" +
                "nodePath='" + nodePath + '\'' +
                ", propertyEvent=" + propertyEvent +
                '}';
    }
}
