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
   * HTTP client that will be used to fetch the page model.
   */
  httpClient: HttpClient;

  /**
   * Current user's request.
   */
  request: HttpRequest;

  /**
   * Options for generating the page model API URL.
   */
  options: PageModelUrlOptions;
}

/**
 * Fetches the page model data.
 */
export type HttpClient = (config: HttpClientConfig) => Promise<any>;

/**
 * Configuration of an HTTP client call.
 */
export type HttpClientConfig = {
  /**
   * HTTP request method.
   */
  method: 'get' | 'post',

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
export type HttpHeaders = {
  [name: string]: string;
};

/**
 * An HTTP request
 */
export interface HttpRequest {
  /**
   * The path part of the URL, including a query string if present.
   * For example: '/path/to/page?foo=1'. The path always starts with '/'.
   */
  path: string;

  /**
   * All request headers (including cookies).
   */
  headers?: HttpHeaders;
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

/**
 * The current page to render.
 */
export interface Page {

  /**
   * Gets a component in the page (e.g. getComponent('main', 'right')).
   * Without any arguments it returns the root component.
   *
   * @param componentNames the names of the component and its parents.
   * @return The component, or null if no such component exists.
   */
  getComponent(...componentNames: string[]): Component | null;

  /**
   * Returns the title of the page, if configured.
   * @returns the title of the page, or null if not configured.
   */
  getTitle(): string | undefined;
}

/**
 * A component in the current page.
 */
export interface Component {

  /**
   * @return The name of this component.
   */
  getName(): string;

  /**
   * @return the parameters of this component.
   */
  getParameters(): ComponentParameters;

}

/**
 * Parameters of a component.
 */
export type ComponentParameters = {
  [name: string]: string;
};

/**
 * A component that holds an ordered list of container item components.
 */
export interface Container extends Component {

  getType(): string; // TODO use type ContainerType;

  getChildren(): ContainerItem[];

}

/**
 * Type of a container, which determines the HTML element rendered for the
 * container and HTML elements rendered for each container item inside it.
 */
export enum ContainerType {
  DivDiv = 'hst.vbox',
  UlLi = 'hst.unorderedlist',
  OlLi = 'hst.orderedlist',
  DivSpan = 'hst.span',
  NoMarkup = 'hst.nomarkup',
}

/**
 * A component that can be configured in the UI.
 */
export interface ContainerItem extends Component {

  /**
   * Returns the type of a container item. The available types depend on which
   * container items have been configured in the backend.
   *
   * @return the type of a container item (e.g. "Banner").
   */
  getType(): string;

  /**
   * Returns whether the component should not render anything.
   * Hiding components is only possible with the Relevance feature.
   *
   * @return whether the component is hidden or not.
   */
  isHidden(): boolean;

}
