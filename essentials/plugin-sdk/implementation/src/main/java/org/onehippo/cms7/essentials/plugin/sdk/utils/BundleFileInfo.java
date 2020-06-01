/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.essentials.plugin.sdk.utils;

import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.LocaleUtils;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class BundleFileInfo {

    private final Collection<BundleInfo> bundleInfos;

    private BundleFileInfo(final Collection<BundleInfo> bundleInfos) {
        this.bundleInfos = bundleInfos;
    }

    public Collection<BundleInfo> getBundleInfos() {
        return bundleInfos;
    }

    private static Collection<BundleInfo> parse(final JSONObject json, final Stack<String> path)
            throws IOException, RepositoryException {
        Collection<BundleInfo> result = new ArrayList<>();
        for (Object o : json.keySet()) {
            final String key = o.toString();
            if (!(json.get(key) instanceof JSONObject)) {
                throw new RepositoryException("Invalid translations import file: expected json object");
            }
            final JSONObject value = (JSONObject) json.get(key);
            if (value.isEmpty()) {
                continue;
            }
            if (!isBundle(value)) {
                path.push(key);
                result.addAll(parse(value, path));
                path.pop();
            } else {
                try {
                    final Locale locale = LocaleUtils.toLocale(key);
                    result.add(new BundleInfo(toBundleName(path), locale, jsonObjectToMap(value)));
                } catch (IllegalArgumentException e) {
                    throw new IOException(toBundleName(path) + " seems to be a bundle but " + key + " is not a locale");
                }
            }
        }
        return result;
    }

    private static String toBundleName(final Stack<String> path) {
        final StringBuilder sb = new StringBuilder();
        final Iterator<String> iterator = path.iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next());
            if (iterator.hasNext()) {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    private static Map<String, String> jsonObjectToMap(final JSONObject json) {
        final Map<String, String> map = new HashMap<>();
        for (Object key : json.keySet()) {
            map.put(key.toString(), json.getString(key.toString()));
        }
        return map;
    }

    public static BundleFileInfo readInfo(final InputStream in) throws RepositoryException, IOException {
        final JSONObject json = JSONObject.fromObject(IOUtils.toString(in, "UTF-8"));
        return new BundleFileInfo(parse(json, new Stack<>()));
    }

    private static boolean isBundle(JSONObject o) {
        if (!o.isEmpty()) {
            final Object val = o.get(o.keys().next());
            if (val instanceof String) {
                return true;
            }
        }
        return false;
    }

}
