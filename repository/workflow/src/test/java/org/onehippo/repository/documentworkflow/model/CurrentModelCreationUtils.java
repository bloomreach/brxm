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
package org.onehippo.repository.documentworkflow.model;

import org.apache.commons.lang.StringUtils;

/**
 * CurrentModelCreationUtils
 */
public class CurrentModelCreationUtils {

    public static final String VARIANT_KEY_DRAFT = "draft";
    public static final String VARIANT_KEY_UNPUBLISHED = "unpublished";
    public static final String VARIANT_KEY_PUBLISHED = "published";

    public static final String SHORT_NOTATION_VARIANT_DRAFT = "D";
    public static final String SHORT_NOTATION_VARIANT_DRAFT_HOLDER_EDITOR = "e";
    public static final String SHORT_NOTATION_VARIANT_DRAFT_HOLDER_EDITOR_DEFAULT_VALUE = "editor";
    public static final String SHORT_NOTATION_VARIANT_DRAFT_NEW = "n";
    public static final String SHORT_NOTATION_VARIANT_DRAFT_LIVE = "l";
    public static final String SHORT_NOTATION_VARIANT_DRAFT_CHANGED = "c";

    public static final String SHORT_NOTATION_VARIANT_UNPUBLISHED = "U";
    public static final String SHORT_NOTATION_VARIANT_UNPUBLISHED_NEW = "n";
    public static final String SHORT_NOTATION_VARIANT_UNPUBLISHED_LIVE = "l";
    public static final String SHORT_NOTATION_VARIANT_UNPUBLISHED_CHANGED = "c";

    public static final String SHORT_NOTATION_VARIANT_PUBLISHED = "P";
    public static final String SHORT_NOTATION_VARIANT_PUBLISHED_PREVIEW = "p";
    public static final String SHORT_NOTATION_VARIANT_PUBLISHED_LIVE = "l";
    public static final String SHORT_NOTATION_VARIANT_PUBLISHED_CHANGED = "c";
    public static final String SHORT_NOTATION_VARIANT_PUBLISHED_NEW = "n";

    public static final String SHORT_NOTATION_REQUEST = "R";
    public static final String SHORT_NOTATION_REQUEST_PUBLISH = "p";
    public static final String SHORT_NOTATION_REQUEST_SCHEDULE_PUBLISH = "q";
    public static final String SHORT_NOTATION_REQUEST_DEPUBLISH = "u";
    public static final String SHORT_NOTATION_REQUEST_SCHEDULE_DEPUBLISH = "v";

    public static final String STATE_SUMMARY_NEW = "new";
    public static final String STATE_SUMMARY_LIVE = "live";
    public static final String STATE_SUMMARY_CHANGED = "changed";

    public static final String AVAILABILITY_LIVE = "live";
    public static final String AVAILABILITY_PREVIEW = "preview";

    private CurrentModelCreationUtils() {
    }

    public static Handle createHandleByVariantStateNotations(String draftStates, String unpublishedStates, String publishedStates, String requestStates) {
        Handle handle = new Handle();

        if (StringUtils.contains(draftStates, SHORT_NOTATION_VARIANT_DRAFT)) {
            Variant draft = new Variant();
            handle.getVariants().put(VARIANT_KEY_DRAFT, draft);

            if (StringUtils.contains(draftStates, SHORT_NOTATION_VARIANT_DRAFT_HOLDER_EDITOR)) {
                draft.setHolder(SHORT_NOTATION_VARIANT_DRAFT_HOLDER_EDITOR_DEFAULT_VALUE);
            }

            if (StringUtils.contains(draftStates, SHORT_NOTATION_VARIANT_DRAFT_NEW)) {
                draft.setStateSummary(STATE_SUMMARY_NEW);
            }

            if (StringUtils.contains(draftStates, SHORT_NOTATION_VARIANT_DRAFT_LIVE)) {
                draft.setStateSummary(STATE_SUMMARY_LIVE);
            }

            if (StringUtils.contains(draftStates, SHORT_NOTATION_VARIANT_DRAFT_CHANGED)) {
                draft.setStateSummary(STATE_SUMMARY_CHANGED);
            }
        }

        if (StringUtils.contains(unpublishedStates, SHORT_NOTATION_VARIANT_UNPUBLISHED)) {
            Variant unpublished = new Variant();
            unpublished.addAvailabilities(AVAILABILITY_PREVIEW);

            handle.getVariants().put(VARIANT_KEY_UNPUBLISHED, unpublished);

            if (StringUtils.contains(unpublishedStates, SHORT_NOTATION_VARIANT_UNPUBLISHED_LIVE)) {
                unpublished.setStateSummary(STATE_SUMMARY_LIVE);
            }

            if (StringUtils.contains(unpublishedStates, SHORT_NOTATION_VARIANT_UNPUBLISHED_NEW)) {
                unpublished.setStateSummary(STATE_SUMMARY_NEW);
                unpublished.addAvailabilities(AVAILABILITY_PREVIEW);
            }

            if (StringUtils.contains(unpublishedStates, SHORT_NOTATION_VARIANT_UNPUBLISHED_CHANGED)) {
                unpublished.setStateSummary(STATE_SUMMARY_CHANGED);
                unpublished.addAvailabilities(AVAILABILITY_PREVIEW);
            }
        }

        if (StringUtils.contains(publishedStates, SHORT_NOTATION_VARIANT_PUBLISHED)) {
            Variant published = new Variant();

            handle.getVariants().put(VARIANT_KEY_PUBLISHED, published);

            if (StringUtils.contains(publishedStates, SHORT_NOTATION_VARIANT_PUBLISHED_LIVE)) {
                published.setStateSummary(STATE_SUMMARY_LIVE);
                published.addAvailabilities(AVAILABILITY_PREVIEW, AVAILABILITY_LIVE);
            }

            if (StringUtils.contains(publishedStates, SHORT_NOTATION_VARIANT_PUBLISHED_CHANGED)) {
                published.setStateSummary(STATE_SUMMARY_CHANGED);
                published.addAvailabilities(AVAILABILITY_LIVE);
            }

            if (StringUtils.contains(publishedStates, SHORT_NOTATION_VARIANT_PUBLISHED_NEW)) {
                published.setStateSummary(STATE_SUMMARY_NEW);
            }
        }

        if (StringUtils.contains(requestStates, SHORT_NOTATION_REQUEST)) {
            if (StringUtils.contains(requestStates, SHORT_NOTATION_REQUEST_PUBLISH)) {
                handle.getRequest().setType(Request.TYPE_PUBLISH);
            } else if (StringUtils.contains(requestStates, SHORT_NOTATION_REQUEST_SCHEDULE_PUBLISH)) {
                handle.getRequest().setType(Request.TYPE_SCHEDULE_PUBLISH);
            } else if (StringUtils.contains(requestStates, SHORT_NOTATION_REQUEST_DEPUBLISH)) {
                handle.getRequest().setType(Request.TYPE_DEPUBLISH);
            } else if (StringUtils.contains(requestStates, SHORT_NOTATION_REQUEST_SCHEDULE_DEPUBLISH)) {
                handle.getRequest().setType(Request.TYPE_SCHEDULE_DEPUBLISH);
            }
        }

        return handle;
    }

}
