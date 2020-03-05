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

import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.service.IEditor;

/**
 * Model for the {@link HippostdPublishableEditor} view.
 *
 */
public class HippoStdPublishableEditorModel {

    private IEditor.Mode mode;
    private String base;
    private String editor;



    HippoStdPublishableEditorModel() {
        this.mode = IEditor.Mode.VIEW;
        this.base = StringUtils.EMPTY;
        this.editor = StringUtils.EMPTY;
    }



    public void setMode(IEditor.Mode mode){
        this.mode = mode;
    }

    public void setBase(String identifier){
        this.base = identifier;
    }

    public void setEditor(String identifier){
        this.editor = identifier;
    }

    public IEditor.Mode getMode() {
        return mode;
    }

    public String getBase() {
        return base;
    }

    public String getEditor() {
        return editor;
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HippoStdPublishableEditorModel)) {
            return false;
        }
        final HippoStdPublishableEditorModel that = (HippoStdPublishableEditorModel) o;
        return mode == that.mode &&
                Objects.equals(base, that.base) &&
                Objects.equals(editor, that.editor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, base, editor);
    }


    @Override
    public String toString() {
        return "HippoStdPublishableEditorModel{" +
                "mode=" + mode +
                ", base='" + base + '\'' +
                ", editor='" + editor + '\'' +
                '}';
    }
}
