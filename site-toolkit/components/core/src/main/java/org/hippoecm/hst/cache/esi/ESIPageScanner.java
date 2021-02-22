/**
 * Copyright 2013-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hippoecm.hst.core.component.SerializableElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * ESIPageScanner
 * <P>
 * Scanner implementation which parses the page body content and collects all the esi fragment information objects.
 * </P>
 */
public class ESIPageScanner {

    private static Logger log = LoggerFactory.getLogger(ESIPageScanner.class);

    private String ESI_COMMENT_START = "<!--esi";
    private String ESI_COMMENT_END = "-->";
    private String ESI_TAG_START_END = ">";
    private String ESI_TAG_END_EMPTY = "/>";
    private String ESI_INCLUDE_TAG_START = "<esi:include";
    private String ESI_INCLUDE_TAG_END = "</esi:include>";
    private String ESI_COMMENT_TAG_START = "<esi:comment";
    private String ESI_COMMENT_TAG_END = "</esi:comment>";
    private String ESI_REMOVE_TAG_START = "<esi:remove";
    private String ESI_REMOVE_TAG_END = "</esi:remove>";
    private String ESI_VARS_TAG_START = "<esi:vars";
    private String ESI_VARS_TAG_END = "</esi:vars>";

    private Pattern ESI_TAG_START_PATTERN = Pattern.compile("(<!--esi|<esi:include|<esi:comment|<esi:remove|<esi:vars)", Pattern.DOTALL);

    public ESIPageScanner() {
    }

    /**
     * Scans the body content and collects all the parsed esi fragment info objects.
     * @param bodyContent
     * @return
     * @throws IOException
     */
    public List<ESIFragmentInfo> scanFragmentInfos(String bodyContent) throws IOException {
        List<ESIFragmentInfo> fragmentInfos = new LinkedList<ESIFragmentInfo>();

        log.debug("[INFO] bodyContent: {}", bodyContent);

        int begin = 0;
        int end = 0;
        int offset = -1;

        Matcher m = ESI_TAG_START_PATTERN.matcher(bodyContent);

        while (m.find(end)) {
            MatchResult mr = m.toMatchResult();
            begin = mr.start();
            end = mr.end();

            String prefix = mr.group(1);
            ESIFragmentType fragmentType = getFragmentTypeByTagPrefix(prefix);

            if (fragmentType == ESIFragmentType.COMMENT_BLOCK) {
                offset = bodyContent.indexOf(ESI_COMMENT_END, end);

                if (offset == -1) {
                    log.warn("Invalid esi comment at index, {}. No comment ending: {}", begin, mr.group());
                } else {
                    end = offset + ESI_COMMENT_END.length();
                    String fragmentSource = bodyContent.substring(begin, end);
                    log.debug("fragmentSource: {}", fragmentSource);

                    try {
                        ESIFragmentInfo fragmentInfo = createCommentFragmentInfo(fragmentSource, begin, end);
                        fragmentInfos.add(fragmentInfo);
                    } catch (Exception e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Failed to create fragment info.", e);
                        } else {
                            log.warn("Failed to create fragment info. {}", e.toString());
                        }
                    }
                }
            } else {
                offset = bodyContent.indexOf(ESI_TAG_END_EMPTY, end);

                if (bodyContent.indexOf(ESI_TAG_START_END, end) < offset) {
                    offset = -1;
                }

                if (offset != -1) {
                    end = offset + ESI_TAG_END_EMPTY.length();
                    String fragmentSource = bodyContent.substring(begin, end);
                    log.debug("fragmentSource: {}", fragmentSource);

                    try {
                        ESIFragmentInfo fragmentInfo = createElementFragmentInfo(fragmentType, fragmentSource, begin, end);
                        fragmentInfos.add(fragmentInfo);
                    } catch (Exception e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Failed to create fragment info.", e);
                        } else {
                            log.warn("Failed to create fragment info. {}", e.toString());
                        }
                    }
                } else {
                    String fragmentEndTag = getEndTagStringByFragmentType(fragmentType);
                    offset = bodyContent.indexOf(fragmentEndTag, end);

                    if (offset == -1) {
                        log.warn("Invalid esi tag at index, {}. No element ending: {}", begin, mr.group());
                    } else {
                        end = offset + fragmentEndTag.length();
                        String fragmentSource = bodyContent.substring(begin, end);
                        log.debug("fragmentSource: {}", fragmentSource);

                        try {
                            ESIFragmentInfo fragmentInfo = createElementFragmentInfo(fragmentType, fragmentSource, begin, end);
                            fragmentInfos.add(fragmentInfo);
                        } catch (Exception e) {
                            if (log.isDebugEnabled()) {
                                log.warn("Failed to create fragment info.", e);
                            } else {
                                log.warn("Failed to create fragment info. {}", e.toString());
                            }
                        }
                    }
                }
            }
        }

        return fragmentInfos;
    }

    /**
     * Create esi comment fragment info from the uncommented source.
     * <P>
     * <EM>NOTE: the source must be uncommented. In other words, the source argument must not contain '<!--' and '-->'.</EM>
     * </P>
     * @param fragmentSource uncommented comment fragment source
     * @param begin the begin index of the original comment fragment source (including '<!--' and '-->') in the page
     * @param end the end index of the original comment fragment source (including '<!--' and '-->') in the page
     * @return
     * @throws Exception
     */
    private ESIFragmentInfo createCommentFragmentInfo(String fragmentSource, int begin, int end) throws Exception {
        String uncommentedFragmentSource = fragmentSource.substring(ESI_COMMENT_START.length(), fragmentSource.length() - ESI_COMMENT_END.length());
        ESICommentFragment fragment = new ESICommentFragment(ESIFragmentType.COMMENT_BLOCK, uncommentedFragmentSource);
        ESICommentFragmentInfo fragmentInfo = new ESICommentFragmentInfo(fragment, begin, end);

        List<ESIFragmentInfo> elementFragmentInfos = scanFragmentInfos(uncommentedFragmentSource);

        for (ESIFragmentInfo elementFragmentInfo : elementFragmentInfos) {
            if (elementFragmentInfo.getFragment().getType() == ESIFragmentType.COMMENT_BLOCK) {
                log.warn("Weird unlikely situation. Parsed ESI comment block must not have a child comment block.");
                continue;
            }

            fragmentInfo.addFragmentInfo(elementFragmentInfo);
        }

        return fragmentInfo;
    }

    /**
     * Create esi element such as comment, include, remove, vars, etc.
     * @param type
     * @param fragmentSource
     * @param begin
     * @param end
     * @return
     * @throws Exception
     */
    private ESIFragmentInfo createElementFragmentInfo(ESIFragmentType type, String fragmentSource, int begin, int end) throws Exception {
        if (type == ESIFragmentType.VARS_TAG) {
            int start = fragmentSource.indexOf(ESI_TAG_START_END);
            int stop = fragmentSource.indexOf(ESI_VARS_TAG_END);

            if (start != -1 && end != -1 && start < end) {
                fragmentSource = fragmentSource.substring(start + 1, stop);
            } else {
                log.warn("Invalid vars tag fragment: {}", fragmentSource);
                fragmentSource = "";
            }
        }

        ESIElementFragment fragment = new ESIElementFragment(type, fragmentSource);

        if (type == ESIFragmentType.INCLUDE_TAG) {
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            dbfac.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbfac.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbfac.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            Document doc = docBuilder.parse(new InputSource(new StringReader(fragmentSource)));
            Element element = doc.getDocumentElement();
            fragment.setElement(new SerializableElement(element));
        }

        ESIElementFragmentInfo fragmentInfo = new ESIElementFragmentInfo(fragment, begin, end);

        return fragmentInfo;
    }

    private ESIFragmentType getFragmentTypeByTagPrefix(String prefix) {
        if (ESI_COMMENT_START.equals(prefix)) {
            return ESIFragmentType.COMMENT_BLOCK;
        } else if (ESI_COMMENT_TAG_START.equals(prefix)) {
            return ESIFragmentType.COMMENT_TAG;
        } else if (ESI_INCLUDE_TAG_START.equals(prefix)) {
            return ESIFragmentType.INCLUDE_TAG;
        } else if (ESI_REMOVE_TAG_START.equals(prefix)) {
            return ESIFragmentType.REMOVE_TAG;
        } else if (ESI_VARS_TAG_START.equals(prefix)) {
            return ESIFragmentType.VARS_TAG;
        }

        throw new IllegalArgumentException("Cannot find fragment type by the prefix, '" + prefix + "'.");
    }

    private String getEndTagStringByFragmentType(ESIFragmentType type) {
        if (type == ESIFragmentType.COMMENT_TAG) {
            return ESI_COMMENT_TAG_END;
        } else if (type == ESIFragmentType.INCLUDE_TAG) {
            return ESI_INCLUDE_TAG_END;
        } else if (type == ESIFragmentType.REMOVE_TAG) {
            return ESI_REMOVE_TAG_END;
        } else if (type == ESIFragmentType.VARS_TAG) {
            return ESI_VARS_TAG_END;
        }

        throw new IllegalArgumentException("Cannot find fragment end tag string by the type, " + type + ".");
    }
}
