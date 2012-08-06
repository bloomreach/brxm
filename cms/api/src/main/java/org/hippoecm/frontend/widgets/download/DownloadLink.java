/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.widgets.download;

import java.io.InputStream;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.time.Time;

public abstract class DownloadLink<T> extends Link<T> {

    private static final long serialVersionUID = 1L;

    public DownloadLink(String id) {
        super(id);
    }

    public DownloadLink(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    public void onClick() {
        DownloadRequestTarget requestTarget = createDownloadRequestTarget();
        RequestCycle.get().setRequestTarget(requestTarget);
    }
    
    protected DownloadRequestTarget createDownloadRequestTarget() {
        return new DownloadRequestTarget();
    }

    protected abstract String getFilename();
    protected abstract InputStream getContent();

    protected void onDownloadTargetDetach() {}

    protected class DownloadRequestTarget implements IRequestTarget {

        /**
         * @see org.apache.wicket.IRequestTarget#respond(org.apache.wicket.RequestCycle)
         */
        public void respond(RequestCycle requestCycle) {

            // Set content type based on markup type for page
            final WebResponse response = (WebResponse) requestCycle.getResponse();

            // Make sure it is not cached by a client
            response.setHeader("Expires", Time.now().subtract(Duration.minutes(1)).toDateString());
            response.setHeader("Cache-Control", "no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setLastModifiedTime(Time.now());

            // set filename
            final String filename = getFilename();
            if (filename != null) {
                final String mimeType = WebApplication.get().getServletContext().getMimeType(filename);
                if (mimeType != null) {
                    response.setContentType(mimeType);
                }
                response.setAttachmentHeader(filename);
                final InputStream content = getContent();
                if (content != null) {
                    try {
                        response.write(content);
                    } finally {
                        IOUtils.closeQuietly(content);
                    }
                }
            }
        }

        public void detach(RequestCycle requestCycle) {
            onDownloadTargetDetach();
        }

    }
}
