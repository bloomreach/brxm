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
package org.onehippo.repository.tika;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.Parser;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;

public final class TikaFactory {

    private static final String TIKA_CONFIG_RESOURCE_NAME = "tika-config.xml";
    private static final String TIKA_CONFIG_RESOURCE_PATH =
            TikaFactory.class.getPackage().getName().replace('.', '/') + "/" + TIKA_CONFIG_RESOURCE_NAME;

    private TikaConfig tikaConfig;

    private TikaFactory() {
        try {
            tikaConfig = new TikaConfig(getTikaConfigURL());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load "+TIKA_CONFIG_RESOURCE_PATH, e);
        } catch (SAXException e) {
            throw new RuntimeException("Failed to parse "+TIKA_CONFIG_RESOURCE_PATH, e);
        } catch (TikaException e) {
            throw new RuntimeException("Failed to instantiate Tika", e);
        }
    }

    private static class SingletonHelper {
        private static final TikaFactory INSTANCE = new TikaFactory();
    }

    public static String getTikaConfigPath() {
        return TIKA_CONFIG_RESOURCE_PATH;
    }

    public static URL getTikaConfigURL() {
        return TikaFactory.class.getResource(TIKA_CONFIG_RESOURCE_NAME);
    }

    public static TikaConfig getTikaConfig() {
        return SingletonHelper.INSTANCE.tikaConfig;
    }

    public static Tika newTika() {
        return new Tika(getTikaConfig());
    }
    public static Tika newTika(final Detector detector, final Parser parser) {
        return new Tika(detector, parser, getTikaConfig().getTranslator());
    }
}
