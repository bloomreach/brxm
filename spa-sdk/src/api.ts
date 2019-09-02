/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

/**
 * Defines all public API of the spa-sdk library.
 * @module api
 */

/**
 * Configuration of the SPA SDK.
 */
export interface Configuration {
  /**
   * Current user's request.
   */
  request: Request;

  /**
   * Options for generating the page model API URL.
   */
  options: PageModelUrlOptions;
}

/**
 * An HTTP request
 */
export interface Request {
  /**
   * The path part of the URL, including a query string if present.
   * For example: '/path/to/page?foo=1'. The path always starts with '/'.
   */
  path: string;

  /**
   * All request headers (including cookies).
   */
  headers?: Object;
}

/**
 * Configuration options for generating the page model URL.
 */
export interface PageModelUrlOptions {

  /**
   * URL mapping for the live page model.
   */
  live: PageModelUrlMapping;

  /**
   * URL mapping for the preview page model.
   */
  preview: PageModelUrlMapping;

  /**
   * Optional custom suffix for requests to the page model API. Must start with a slash.
   * The default suffix is '/resourceapi'.
   */
  pageModelApiSuffix?: string;
}

/**
 * Mapping of the incoming HTTP request path to the URL of the page model API.
 */
export interface PageModelUrlMapping {

  /**
   * Base path of the SPA. Everything after it will be interpreted as a route into the page model.
   * The default base path is '/'.
   */
  spaBasePath?: string;

  /**
   * Base URL to fetch the page model from.
   */
  pageModelBaseUrl: string;
}
