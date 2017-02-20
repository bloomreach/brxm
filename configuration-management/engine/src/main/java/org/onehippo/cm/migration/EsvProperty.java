/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.migration;

import java.util.ArrayList;
import java.util.List;

import org.onehippo.cm.impl.model.SourceLocationImpl;

public class EsvProperty {

    private final String name;
    private int type;
    private final SourceLocationImpl location;
    private Boolean multiple;
    private EsvMerge merge;
    private String mergeLocation;
    private List<EsvValue> values = new ArrayList<>();

    public EsvProperty(final String name, final int type, final SourceLocationImpl location) {
        this.name = name;
        this.type = type;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public SourceLocationImpl getSourceLocation() {
        return location;
    }

    public Boolean getMultiple() {
        return multiple;
    }

    public boolean isMultiple() {
        return (multiple != null && multiple) || getValues().size() > 1;
    }

    public boolean isSingle() {
        return (multiple == null || !multiple) && getValues().size() == 1;
    }

    public void setMultiple(final Boolean multiple) {
        this.multiple = multiple;
    }

    public EsvMerge getMerge() {
        return merge;
    }

    public void setMerge(final EsvMerge merge) {
        this.merge = merge;
    }

    public boolean isMergeSkip() {
        return EsvMerge.SKIP == merge;
    }

    public boolean isMergeOverride() {
        return EsvMerge.OVERRIDE == merge;
    }

    public boolean isMergeAppend() {
        return EsvMerge.APPEND == merge;
    }

    public String getMergeLocation() {
        return mergeLocation;
    }

    public void setMergeLocation(final String mergeLocation) {
        this.mergeLocation = mergeLocation;
    }

    public List<EsvValue> getValues() {
        return values;
    }

    public String getValue() {
        return getValues().size() == 1 ? getValues().get(0).getString() : null;
    }
}
