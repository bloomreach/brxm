/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.services.webfiles.watch;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Test defaults to 'official' default values and that the lists returned by different instances are properly
 * independent.
 */
public class WebFilesWatcherJavaConfigTest {

    public static final List<String> WATCHED = Arrays.asList(WebFilesWatcherConfig.DEFAULT_WATCHED_MODULES);
    public static final List<String> INCLUDED = Arrays.asList(WebFilesWatcherConfig.DEFAULT_INCLUDED_FILES);
    public static final List<String> EXCLUDED = Arrays.asList(WebFilesWatcherConfig.DEFAULT_EXCLUDED_DIRECTORIES);
    public static final List<String> WATCH_SERVICE = Arrays.asList(WebFilesWatcherConfig.DEFAULT_USE_WATCH_SERVICE_ON_OS_NAMES);
    public static final Long WATCH_DELAY = WebFilesWatcherConfig.DEFAULT_WATCH_DELAY_MILLIS;

    private WebFilesWatcherJavaConfig config1, config2;

    @Before
    public void init() {
        config1 = new WebFilesWatcherJavaConfig();
        config2 = new WebFilesWatcherJavaConfig();
    }

    @Test
    public void testGetWatchedModulesDefault() throws Exception {
        assertThat(config1.getWatchedModules(), is(WATCHED));
    }

    @Test
    public void testAddWatchedModule() throws Exception {
        config1.addWatchedModule("hippo");
        assertThat(config1.getWatchedModules(), contains("hippo"));
        assertThat(config2.getWatchedModules(), is(WATCHED));
    }

    @Test
    public void testGetIncludedFilesDefault() throws Exception {
        assertThat(config1.getIncludedFiles(), is(INCLUDED));

    }

    @Test
    public void testIncludeFiles() throws Exception {
        config1.includeFiles("hippo");
        assertThat(config1.getIncludedFiles(), contains("hippo"));
        assertThat(config2.getIncludedFiles(), is(INCLUDED));

    }

    @Test
    public void testGetExcludedDirectoriesDefault() throws Exception {
        assertThat(config1.getExcludedDirectories(), is(EXCLUDED));

    }

    @Test
    public void testExcludeDirs() throws Exception {
        config1.excludeDirs("hippo");
        assertThat(config1.getExcludedDirectories(), contains("hippo"));
        assertThat(config2.getExcludedDirectories(), is(EXCLUDED));

    }

    @Test
    public void testGetUseWatchServiceOnOsNamesDefault() throws Exception {
        assertThat(config1.getUseWatchServiceOnOsNames(), is(WATCH_SERVICE));

    }

    @Test
    public void testUseWatchServiceOnOsNames() throws Exception {
        config1.useWatchServiceOnOsNames("hippo");
        assertThat(config1.getUseWatchServiceOnOsNames(), contains("hippo"));
        assertThat(config2.getUseWatchServiceOnOsNames(), is(WATCH_SERVICE));

    }

    @Test
    public void testGetWatchDelayMillisDefault() throws Exception {
        assertThat(config1.getWatchDelayMillis(), is(WATCH_DELAY));

    }

    @Test
    public void testSetWatchDelayMillis() throws Exception {
        config1.setWatchDelayMillis(WATCH_DELAY + 42);
        assertThat(config1.getWatchDelayMillis(), is(WATCH_DELAY + 42));
        assertThat(config2.getWatchDelayMillis(), is(WATCH_DELAY));

    }
}
