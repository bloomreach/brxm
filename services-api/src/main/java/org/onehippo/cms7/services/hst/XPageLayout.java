/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.onehippo.cms7.services.hst;

import java.util.Objects;

public class XPageLayout implements IXPageLayout {

    private final String label;
    private final String key;
    private final String subPrototypeUUID;

    public XPageLayout(final String key, final String label, final String subPrototypeUUID) {
        this.label = label;
        this.key = key;
        this.subPrototypeUUID = subPrototypeUUID;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getSubPrototypeUUID() {
        return subPrototypeUUID;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof XPageLayout)) {
            return false;
        }
        final XPageLayout that = (XPageLayout) o;
        return Objects.equals(getLabel(), that.getLabel()) &&
                Objects.equals(getKey(), that.getKey()) &&
                Objects.equals(getSubPrototypeUUID(), that.getSubPrototypeUUID());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLabel(), getKey(), getSubPrototypeUUID());
    }
}
