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
package org.onehippo.cm.impl.model.builder.sorting;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;

import static java.util.Collections.unmodifiableMap;

public class SortedModule implements Module {
    private Module delegatee;
    private Project project;
    private final Map<String, Source> sortedSources = new LinkedHashMap<>();

    public SortedModule(final Module delegatee, final Project project) {
        this.delegatee = delegatee;
        this.project = project;
        SortedSet<Source> sorted = new SourceSorter().sort(delegatee.getSources());
        for (Source sortedSource : sorted) {
            sortedSources.put(sortedSource.getPath(), sortedSource);
        }
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public Map<String, Source> getSources() {
        return unmodifiableMap(sortedSources);
    }

    @Override
    public String getName() {
        return delegatee.getName();
    }

    @Override
    public List<String> getAfter() {
        return delegatee.getAfter();
    }

    static class SourceSorter {

        public SortedSet<Source> sort(final Map<String, Source> sources) {
            SortedSet<Source> sortedSources = new TreeSet<>(new Comparator<Source>() {
                @Override
                public int compare(final Source source1, final Source source2) {
                    String[] source1Segments = source1.getPath().split("/");
                    String[] source2Segments = source2.getPath().split("/");
                    if (source1Segments.length == source2Segments.length) {
                        return source1.getPath().compareTo(source2.getPath());
                    }

                    if (source1Segments.length < source2Segments.length) {
                        return -1;
                    }
                    return 1;
                }
            });
            sortedSources.addAll(sources.values());
            return sortedSources;
        }
    }
}
