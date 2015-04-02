/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class FileSystemWatcherIT extends AbstractFileSystemObserverIT {

    private FileSystemWatcher fileSystemWatcher;

    @Override
    public FileSystemObserver createFileSystemObserver() throws IOException {
        fileSystemWatcher = new FileSystemWatcher(fileNameMatcher);
        return fileSystemWatcher;
    }

    @Test(timeout = 60000)
    public void deleted_directories_get_removed_from_watchedFiles_eventually() throws IOException, InterruptedException {
        observeTestBundle();

        final int startSize = fileSystemWatcher.watchedPaths.size();

        for( int i = 0; i < 10; i++) {
            final File newDir = new File(testBundleDir, "newDir");
            assertTrue(newDir.mkdir());
            changesListener.awaitChanges();
            changesListener.assertCreated(newDir);
            assertTrue(fileSystemWatcher.watchedPaths.size() > startSize);
            newDir.delete();
            changesListener.awaitChanges();
            changesListener.assertCreated(newDir);
        }

        // after enough time and GC, the watchedPaths WatchKey that belong to deleted directories should be removed
        // if after 60 seconds the fileSystemWatcher.watchedPaths.size is not back to the value before adding and
        // removing the newDir, the test fails
        while (fileSystemWatcher.watchedPaths.size() != startSize) {
            System.gc();
            Thread.sleep(10);
            System.gc();
        }
    }

}
