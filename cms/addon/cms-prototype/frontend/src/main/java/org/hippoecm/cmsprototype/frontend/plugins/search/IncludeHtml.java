/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.search;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class IncludeHtml extends WebComponent
{
    private static final long serialVersionUID = 1L;

    private String htmlExcerpt;
   
    public IncludeHtml(final String id)
    {
        super(id);
    }

    public IncludeHtml(String id, IModel model)
    {
        super(id, model);
    }

    public IncludeHtml(String id, String htmlExcerpt)
    {
        super(id, new Model(htmlExcerpt));
        this.htmlExcerpt = htmlExcerpt;
    }

    protected String importAsString()
    {
        return htmlExcerpt;
    }

    /**
     * @see org.apache.wicket.Component#onComponentTagBody(org.apache.wicket.markup.MarkupStream,
     *      org.apache.wicket.markup.ComponentTag)
     */
    protected void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag)
    {
        String content = importAsString();
        replaceComponentTagBody(markupStream, openTag, content);
    }

   
  
}
