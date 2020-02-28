/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.editor;

import org.apache.commons.lang.StringUtils;

public class TestDocument implements Document{

    public static final String PUBLISHED = "published";
    public static final String UNPUBLISHED = "unpublished";
    public static final String DRAFT = "draft";
    public static final String REVISION = "revision";
    private String published;
    private String unpublished;
    private String revision;
    private String draft;
    private boolean transferable;
    private boolean holder;

    public TestDocument() {
        published = unpublished = draft = revision = StringUtils.EMPTY;

    }

    public static TestDocument create(){
        return new TestDocument();
    }

    public TestDocument published(){
        this.published = PUBLISHED;
        return this;
    }

    public TestDocument notPublished(){
        this.published = StringUtils.EMPTY;
        return this;
    }

    public TestDocument unpublished(){
        this.unpublished = UNPUBLISHED;
        return this;
    }

    public TestDocument notUnpublished(){
        this.unpublished = StringUtils.EMPTY;
        return this;
    }

    public TestDocument draft(){
        this.draft = DRAFT;
        return this;
    }
    public TestDocument notDraft(){
        this.draft = StringUtils.EMPTY;
        return this;
    }

    public TestDocument revision(){
        this.revision = REVISION;
        return this;
    }

    public TestDocument notRevision(){
        this.revision = StringUtils.EMPTY;
        return this;
    }

    public TestDocument holder(){
        this.holder = true;
        return this;
    }

    public TestDocument notHolder(){
        this.holder = false;
        return this;
    }

    public TestDocument transferable(){
        this.transferable = true;
        return this;
    }

    public TestDocument notTransferable(){
        this.transferable = false;
        return this;
    }




    @Override
    public String getUnpublished() {
        return unpublished;
    }

    @Override
    public String getPublished() {
        return published;
    }

    @Override
    public String getDraft() {
        return draft;
    }

    @Override
    public String getRevision() {
        return revision;
    }

    @Override
    public boolean isTransferable() {
        return transferable;
    }

    @Override
    public boolean isHolder() {
        return holder;
    }


    @Override
    public String toString() {
        return "TestDocument{" +
                "published='" + published + '\'' +
                ", unpublished='" + unpublished + '\'' +
                ", revision='" + revision + '\'' +
                ", draft='" + draft + '\'' +
                ", transferable=" + transferable +
                ", holder=" + holder +
                '}';
    }
}

