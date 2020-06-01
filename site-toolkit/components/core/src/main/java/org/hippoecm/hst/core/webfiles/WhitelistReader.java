/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.webfiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhitelistReader {

    private static final Logger log = LoggerFactory.getLogger(WhitelistReader.class);

    private final Set<String> whitelist = new HashSet<>();

    public WhitelistReader(final InputStream is) {
        try {
            final List<String> list = IOUtils.readLines(is, "UTF-8");
            for (String line : list) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                final int hashIndex = line.indexOf('#');
                if (hashIndex == 0) {
                    // comment
                    continue;
                } else if (hashIndex > 0) {
                    final String beforeComment = line.substring(0, hashIndex);
                    if (StringUtils.isBlank(beforeComment)) {
                        continue;
                    }
                    whitelist.add(beforeComment);
                } else {
                    whitelist.add(line);
                }
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Could not read InputStream due to wrong encoding. Return empty whitelist", e);
        } catch (IOException e) {
            log.error("Error during reading InputStream. Return empty whitelist", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public Set<String> getWhitelist() {
        return whitelist;
    }


}
