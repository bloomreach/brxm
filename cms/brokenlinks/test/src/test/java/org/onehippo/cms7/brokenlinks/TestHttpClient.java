/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.cms7.brokenlinks;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

public class TestHttpClient implements HttpClient {

    public static String OK_URL = "http://good/";
    public static String MOVED_URL = "http://moved/";
    public static String BAD_URL = "http://bad/";
    public static String NOT_FOUND_URL = "http://notfound/";

    private final ClientConnectionManager conman;
    private final HttpParams params;

    public TestHttpClient(ClientConnectionManager conman, HttpParams params) {
        this.conman = conman;
        this.params = params;
    }

    @Override
    public HttpParams getParams() {
        return params;
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return conman;
    }

    @Override
    public HttpResponse execute(final HttpUriRequest request) throws IOException, ClientProtocolException {
        final URI uri = request.getURI();
        final BasicStatusLine statusline;
        if (!"good".equals(uri.getHost())) {
            statusline = new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "Not Found");
        } else {
            statusline = new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
        }
        return new BasicHttpResponse(statusline);
    }

    private HttpResponse fail() throws IOException {
        throw new IOException("HttpClient does not support this method");
    }

    private <T> T failT() throws IOException {
        throw new IOException("HttpClient does not support this method");
    }

    @Override
    public HttpResponse execute(final HttpUriRequest request, final HttpContext context) throws IOException, ClientProtocolException {
        return execute(request);
    }

    @Override
    public HttpResponse execute(final HttpHost target, final HttpRequest request) throws IOException, ClientProtocolException {
        return fail();
    }

    @Override
    public HttpResponse execute(final HttpHost target, final HttpRequest request, final HttpContext context) throws IOException, ClientProtocolException {
        return fail();
    }

    @Override
    public <T> T execute(final HttpUriRequest request, final ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return failT();
    }

    @Override
    public <T> T execute(final HttpUriRequest request, final ResponseHandler<? extends T> responseHandler, final HttpContext context) throws IOException, ClientProtocolException {
        return failT();
    }

    @Override
    public <T> T execute(final HttpHost target, final HttpRequest request, final ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return failT();
    }

    @Override
    public <T> T execute(final HttpHost target, final HttpRequest request, final ResponseHandler<? extends T> responseHandler, final HttpContext context) throws IOException, ClientProtocolException {
        return failT();
    }

}
