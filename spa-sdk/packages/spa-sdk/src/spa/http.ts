/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Visitor } from '../page';

/**
 * Fetches the page model data.
 */
export type HttpClient<T> = (config: HttpClientConfig) => Promise<HttpResponse<T>>;

/**
 * Configuration of an HTTP client call.
 */
export type HttpClientConfig = {
  /**
   * HTTP request method.
   */
  method: 'GET' | 'POST',

  /**
   * The URL to send the HTTP request to.
   */
  url: string,

  /**
   * Optional: the headers to send with the HTTP request.
   */
  headers?: HttpHeaders,

  /**
   * Optional: the data to send with the HTTP request.
   * Will only be provided when the 'method' is 'post'.
   */
  data?: any;
};

/**
 * Map of HTTP headers.
 */
export type HttpHeaders = Partial<Record<string, string | string[]>>;

/**
 * An HTTP connection.
 */
export interface HttpConnection {
  /**
   * Client's remote IP address.
   */
  remoteAddress?: string;
}

/**
 * An HTTP request
 */
export interface HttpRequest {
  /**
   * HTTP connection data.
   */
  connection?: HttpConnection;

  /**
   * All request headers (including cookies).
   */
  headers?: HttpHeaders;

  /**
   * The path part of the URL, including a query string if present.
   * For example: '/path/to/page?foo=1'. The path always starts with '/'.
   */
  path: string;

  /**
   * Current request visitor.
   */
  visitor?: Omit<Visitor, 'new'>;
}

/**
 * An HTTP response.
 */
export interface HttpResponse<T> {
  /**
   * The data returned in the response.
   */
  data: T;
}
