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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Esv2Yaml {

    static final Logger log = LoggerFactory.getLogger(Esv2Yaml.class);

    public static void main(String[] args) throws IOException {

        if (args.length == 0 || args[0] == null) {
            System.out.println("usage: <basedir> # directory of a hippoecm-extension.xml file");
            return;
        }
        try {
            new Esv2Yaml(new File(args[0])).convert();
        } catch (IOException e) {
            log.error("Esv2Yaml.convert() failed. ", e);
        }
    }

    private File baseDir;
    private EsvParser esvParser;
    private File extensionFile;

    public Esv2Yaml(File baseDir) throws IOException {
        this.baseDir = baseDir;
        extensionFile = new File(baseDir, "hippoecm-extension.xml");
        if (!extensionFile.exists() || !extensionFile.isFile()) {
            throw new IOException("File not found: "+extensionFile.getPath());
        }
        esvParser = new EsvParser(baseDir);
    }

    public void convert() throws IOException {
        EsvNode rootNode = esvParser.parse(createReader(extensionFile), extensionFile.getCanonicalPath());
        if (rootNode != null) {
            return;
        }
    }

    public static Reader createReader(File file) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
    }
}
