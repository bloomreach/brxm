/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Map;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Strings;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @version "$Id$"
 */
public final class TemplateUtils {

    private static Logger log = LoggerFactory.getLogger(TemplateUtils.class);

    private TemplateUtils() {
    }

    public static void replaceFileTemplateData(final Path path, final Map<String, Object> data) {
        GlobalUtils.writeToFile(replaceTemplateData(GlobalUtils.readTextFile(path).toString(), data), path);
    }

    public static String replaceTemplateData(final String content, final Map<String, Object> data) {
        if (Strings.isNullOrEmpty(content)) {
            return content;
        }

        try {
            final Writer writer = new StringWriter();
            final MustacheFactory mf = new DefaultMustacheFactory();

            final StringReader reader = new StringReader(content);
            final Mustache mustache = mf.compile(reader, content);

            mustache.execute(writer, data);
            writer.flush();
            return writer.toString();
        } catch (IOException e) {
            log.error("Error flushing template", e);
        }
        return content;
    }

    public static String injectTemplate(final String templateName, final Map<String, Object> data, final Class<?> clazz) {
        return injectTemplate(clazz.getClassLoader().getResourceAsStream(templateName), data);
    }

    public static String injectTemplate(final String templatePath, final Map<String, Object> data) {
        return injectTemplate(TemplateUtils.class.getResourceAsStream(templatePath), data);
    }

    private static String injectTemplate(final InputStream templateStream, final Map<String, Object> data) {
        if (templateStream == null) {
            return null;
        }
        try {
            final String content = GlobalUtils.readStreamAsText(templateStream);
            return replaceTemplateData(content, data);
        } catch (Exception e) {
            log.error("Error processing template", e);
        } finally {
            IOUtils.closeQuietly(templateStream);
        }
        return null;
    }
}
