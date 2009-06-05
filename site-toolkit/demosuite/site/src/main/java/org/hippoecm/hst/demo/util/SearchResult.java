/*
 * Copyright 2009 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.demo.util;

import java.io.Serializable;
import java.util.Calendar;

public class SearchResult<T> implements Serializable {
    private static final long serialVersionUID = -5158089708675950372L;

    private T item;
    private String title;
    private String text;
    private Calendar date;


    public SearchResult(T item, String title, String text, Calendar date) {
        this.item = item;
        this.title = title;
        this.text = text;
        this.date = date;
    }


    public T getItem() {
        return item;
    }

    public void setItem(T item) {
        this.item = item;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
    
    public void setDate(Calendar date) {
        this.date = date;
    }
    
    public Calendar getDate(){
        return this.date;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SearchResult");
        sb.append("{item=").append(item);
        sb.append(", title='").append(title).append('\'');
        sb.append(", text='").append(text).append('\'');
        sb.append(", date='").append(date.getTime()).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

