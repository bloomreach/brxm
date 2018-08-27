/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.autoexport;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ModuleInfo {
    private final String fixtureName;
    private final String moduleName;
    private final String inName;
    private final String outName;
    private Path workingDirectory = null;

    public ModuleInfo(final String fixtureName) {
        this(fixtureName, null, "in", "out");
    }

    public ModuleInfo(final String fixtureName, final String moduleName) {
        this(fixtureName, moduleName, "in", "out");
    }

    public ModuleInfo(final String fixtureName, final String moduleName, final String inName, final String outName) {
        this.fixtureName = fixtureName;
        this.moduleName = moduleName;
        this.inName = inName;
        this.outName = outName;
    }

    public String getFixtureName() {
        return fixtureName;
    }

    public String getEffectiveModuleName() {
        return moduleName == null ? "TestModuleFileSource" : moduleName;
    }

    public Path getInPath() {
        return getPath(inName);
    }

    public Path getOutPath() {
        return getPath(outName);
    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(final Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    protected Path getPath(final String lastSegment) {
        final Path subPath = Paths.get("src", "test", "resources", getFixtureRootFolder(), fixtureName);
        final Path intermediate = calculateBasePath().resolve(subPath);
        return moduleName == null
                ? intermediate.resolve(lastSegment)
                : intermediate.resolve(moduleName).resolve(lastSegment);
    }

    protected String getFixtureRootFolder() {
        return "AutoExportIntegrationTest";
    }

    /**
     * Utility method to calculate correct path in case when run under Intellij IDEA (Working directory should be set to
     * module's root, e.g. ../master/engine).
     *
     * @return base directory
     */
    private static Path calculateBasePath() {
        String basedir = System.getProperty("basedir");
        basedir = basedir != null ? basedir : System.getProperty("user.dir");
        return Paths.get(basedir);
    }
}
