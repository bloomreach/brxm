/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.autoreload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the auto-reload JavaScript snippet.
 */
class AutoReloadScriptLoader {

    private static final String AUTO_RELOAD_SCRIPT = "autoreload.js";
    private static final int READ_BUF_SIZE = 1024;

    private static final Logger log = LoggerFactory.getLogger(AutoReloadScriptLoader.class);

    /**
     * @return the auto-reload JavaScript snippet, or null if the snippet could not be loaded.
     */
    String getJavaScript() {
        InputStream input = null;
        try {
            input = AutoReloadScriptLoader.class.getResourceAsStream(AUTO_RELOAD_SCRIPT);
            if (input == null) {
                log.error("Could not locate " + AUTO_RELOAD_SCRIPT);
            } else {
                return readString(input, "UTF-8");
            }
        } catch (IOException e) {
            log.warn("Error while loading " + AUTO_RELOAD_SCRIPT, e);
        } finally {
            closeQuietly(input);
        }
        log.warn("Script {} could not be loaded", AUTO_RELOAD_SCRIPT);
        return null;
    }

    private String readString(final InputStream in, final String encoding) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[READ_BUF_SIZE];
        int length;
        while ((length = in.read(buffer)) != -1) {
            out.write(buffer, 0, length);
        }
        return new String(out.toByteArray(), encoding);
    }

    private void closeQuietly(final InputStream input) {
        try {
            input.close();
        } catch (IOException e) {
            log.debug("Ignored exception while closing input stream", e);
        }
    }

}
