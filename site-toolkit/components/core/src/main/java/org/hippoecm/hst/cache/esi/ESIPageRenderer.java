/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.cache.esi;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.util.PropertyParser;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;

/**
 * ESIPageRenderer
 */
public class ESIPageRenderer {

    private static final Pattern COOKIE_VAR_PATTERN = Pattern.compile("^HTTP_COOKIE\\{([^\\{\\}]+)\\}$");

    private PropertyParser propertyParser;

    public void render(Writer writer, HttpServletRequest request, ESIPageInfo pageInfo) throws IOException {
        propertyParser = new PropertyParser(null, "$(", ")", null, true);
        propertyParser.setPlaceholderResolver(new ESIVarsPlaceholderResolver(request));

        String bodyContent = pageInfo.getBodyContent();
        List<ESIFragmentInfo> fragmentInfos = pageInfo.getFragmentInfos();

        if (fragmentInfos.isEmpty()) {
            writer.write(bodyContent);
            return;
        }

        int beginIndex = 0;

        for (ESIFragmentInfo fragmentInfo : fragmentInfos) {
            ESIFragment fragment = fragmentInfo.getFragment();
            ESIFragmentType type = fragment.getType();

            writer.write(bodyContent.substring(beginIndex, fragmentInfo.getBeginIndex()));
            beginIndex = fragmentInfo.getEndIndex();

            if (type == ESIFragmentType.COMMENT_BLOCK) {
                String uncommentedSource = fragment.getSource();

                if (!((ESICommentFragmentInfo) fragmentInfo).hasAnyFragmentInfo()) {
                    writer.write(uncommentedSource);
                } else {
                    List<ESIFragmentInfo> embeddedFragmentInfos = ((ESICommentFragmentInfo) fragmentInfo).getFragmentInfos();
                    int embeddedBeginIndex = 0;

                    for (ESIFragmentInfo embeddedFragmentInfo : embeddedFragmentInfos) {
                        ESIFragment embeddedFragment = embeddedFragmentInfo.getFragment();
                        ESIFragmentType embeddedFragmentType = embeddedFragment.getType();

                        writer.write(uncommentedSource.substring(embeddedBeginIndex, embeddedFragmentInfo.getBeginIndex()));
                        embeddedBeginIndex = embeddedFragmentInfo.getEndIndex();

                        if (embeddedFragmentType == ESIFragmentType.INCLUDE_TAG) {
                            writeElementFragment(writer, (ESIElementFragment) embeddedFragment);
                        }
                    }

                    writer.write(uncommentedSource.substring(embeddedFragmentInfos.get(embeddedFragmentInfos.size() - 1).getEndIndex()));
                }
            } else if (type == ESIFragmentType.INCLUDE_TAG) {
                writeElementFragment(writer, (ESIElementFragment) fragment);
            } else if (type == ESIFragmentType.VARS_TAG) {
                writeElementFragment(writer, (ESIElementFragment) fragment);
            }
        }

        writer.write(bodyContent.substring(fragmentInfos.get(fragmentInfos.size() - 1).getEndIndex()));
    }

    protected void writeElementFragment(Writer writer, ESIElementFragment fragment) throws IOException {
        ESIFragmentType type = fragment.getType();

        if (type == ESIFragmentType.INCLUDE_TAG) {
            writeIncludeElementFragment(writer, fragment);
        } else if (type == ESIFragmentType.VARS_TAG) {
            writeVarsElementFragment(writer, fragment);
        }
    }

    protected void writeIncludeElementFragment(Writer writer, ESIElementFragment fragment) throws IOException {
        String src = fragment.getElement().getAttribute("src");
        String alt = fragment.getElement().getAttribute("alt");
        String onerror = fragment.getElement().getAttribute("onerror");

        if (src != null) {
            src = (String) propertyParser.resolveProperty(getClass().getSimpleName(), src);
        }

        if (alt != null) {
            alt = (String) propertyParser.resolveProperty(getClass().getSimpleName(), alt);
        }

        // TODO ...
    }

    protected void writeVarsElementFragment(Writer writer, ESIElementFragment fragment) throws IOException {
        String source = fragment.getSource();

        if (source != null) {
            source = (String) propertyParser.resolveProperty(getClass().getSimpleName(), source);
            writer.write(source);
        }
    }

    protected PropertyParser createESIVariablesParser(final HttpServletRequest request) {
        final HstRequestContext requestContext = RequestContextProvider.get();

        PropertyParser parser = new PropertyParser(null, "$(", ")", null, true);

        parser.setPlaceholderResolver(new PlaceholderResolver() {
            @Override
            public String resolvePlaceholder(String name) {
                if ("HTTP_HOST".equals(name)) {
                    return requestContext.getResolvedMount().getResolvedVirtualHost().getResolvedHostName();
                }

                Matcher m = COOKIE_VAR_PATTERN.matcher(name);
                if (m.matches()) {
                    String cookieName = m.group(1);
                    Cookie [] cookies = request.getCookies();
                }

                return null;
            }
        });

        return parser;
    }
}
