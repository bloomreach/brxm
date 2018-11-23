/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
 * @module api
 * Defines all public API of the ui-extension library.
 */

export type EventCallback<Events> = (eventData: Events[keyof Events]) => any;
type UnsubscribeFn = () => void;

export enum UiExtensionErrorCode {
  'NotInIframe' = 'NotInIframe',
  'IncompatibleParent' = 'IncompatibleParent',
  'ConnectionDestroyed' = 'ConnectionDestroyed',
  'InternalError' = 'InternalError',
}

export interface UiExtensionError {
  code: UiExtensionErrorCode;
  message: string;
}

export interface UiProperties {
  baseUrl: string;
  extension: {
    config: string,
  };
  locale: string;
  timeZone: string;
  user: {
    id: string,
  };
  version: string;
}

export interface UiScope extends UiProperties {
  channel: ChannelScope;
}

export interface ChannelScope {
  page: PageScope;
  refresh: () => Promise<void>;
}

export interface PageScope extends Emitter<PageScopeEvents> {
  get: () => Promise<PageProperties>;
  refresh: () => Promise<void>;
}

export interface Emitter<Events> {
  on: (eventName: keyof Events, listener: EventCallback<Events>) => UnsubscribeFn;
}

export interface PageScopeEvents {
  navigate: PageProperties;
}

export interface PageProperties {
  channel: {
    id: string;
  };
  id: string;
  sitemapItem: {
    id: string;
  };
  url: string;
}
