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

/**
 * ESIPageRenderer
 */
public class ESIPageRenderer {

    public void render(Writer writer, ESIPageInfo pageInfo) throws IOException {
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
            }
        }

        writer.write(bodyContent.substring(fragmentInfos.get(fragmentInfos.size() - 1).getEndIndex()));
    }

    protected void writeElementFragment(Writer writer, ESIElementFragment fragment) {
        ESIFragmentType type = fragment.getType();

        if (type == ESIFragmentType.INCLUDE_TAG) {
            writeIncludeElementFragment(writer, fragment);
        }
    }

    protected void writeIncludeElementFragment(Writer writer, ESIElementFragment fragment) {
        // TODO
    }
}
