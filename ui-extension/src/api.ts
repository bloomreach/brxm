/*
 * Copyright 2018-2023 Bloomreach
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
 * Defines all public API of the ui-extension library.
 * @module api
 */

/**
 * Callback function for events generated by the CMS.
 */
export type EventHandler<Events> = (eventData: Events[keyof Events]) => any;

/**
 * Function to unsubscribe an [[EventHandler]] with.
 */
type UnsubscribeFn = () => void;

/**
 * Identifies which error occurred while using the ui-extension library.
 */
export enum UiExtensionErrorCode {
  /**
   * The connection with the CMS has been destroyed.
   */
  ConnectionDestroyed = 'ConnectionDestroyed',

  /**
   * A dialog was canceled.
   * @since 13.2
   */
  DialogCanceled = 'DialogCanceled',

  /**
   * A dialog is already shown to the user.
   * @since 13.2
   */
  DialogExists = 'DialogExists',

  /**
   * The version of the CMS in which the UI extension is loaded is not compatible with the version of the
   * ui-extension library used by the UI extension.
   */
  IncompatibleParent = 'IncompatibleParent',

  /**
   * An internal error occurred.
   */
  InternalError = 'InternalError',

  /**
   * The UI extension is not running in an iframe.
   */
  NotInIframe = 'NotInIframe',
}

/**
 * Error returned by the ui-extension library via a rejected Promise.
 */
export interface UiExtensionError {
  /**
   * Identifies the error to applications.
   */
  code: UiExtensionErrorCode;

  /**
   * Explains the error to humans.
   */
  message: string;
}

/**
 * Defines the different possible styling themes of the surrounding user interface.
 * @since 13.2
 */
export enum UiStyling {
  Classic = 'classic',
  Material = 'material',
}

/**
 * Properties of the CMS that loads the UI extension.
 */
export interface UiProperties {
  /**
   * The base URL of the CMS, without any query parameters.
   */
  baseUrl: string;

  /**
   * Properties of this UI extension.
   */
  extension: {
    /**
     * The configuration of this UI extension. How to interpret the string
     * (e.g. parse as JSON) is up to the implementation of the UI extension.
     */
    config: string,
  };

  /**
   * The locale of the CMS user as selected in the login page. For example: "en".
   */
  locale: string;

  /**
   * The styling of the user interface in which the extension is shown.
   * @since 13.2
   */
  styling: UiStyling;

  /**
   * The time zone of the CMS user as selected on the login page. For example: "Europe/Amsterdam".
   */
  timeZone: string;

  /**
   * Properties of the CMS user.
   */
  user: {
    /**
     * The username of the CMS user. For example: "admin".
     */
    id: string,

    /**
     * The first name of the CMS user. For example: "Suzanna".
     */
    firstName: string,

    /**
     * The last name of the CMS user. For example: "Doe".
     */
    lastName: string,

    /**
     * Concatenation of the first and last name of the CMS user, or the username if both are blank.
     * For example: "Suzanna Doe" or "admin".
     */
    displayName: string,
  };

  /**
   * The version of the CMS. For example: "13.0.0".
   */
  version: string;
}

/**
 * API to access information about and communicate with the CMS that loads the UI extension.
 */
export interface UiScope extends UiProperties {
  /**
   * API for the current channel.
   */
  channel: ChannelScope;

  /**
   * API for the current document.
   */
  document: DocumentScope;

  /**
   * API for dialogs.
   */
  dialog: DialogScope;
}

/**
 * API to access information about and communicate with the current channel shown in the Channel Manager.
 */
export interface ChannelScope extends Emitter<ChannelScopeEvents> {
  /**
   * API for the current page in the current channel.
   */
  page: PageScope;

  /**
   * Refreshes the metadata of the currently shown channel (e.g. whether it has changes, the sitemap, etc.).
   * The Channel Manager UI will be updated to reflect the channel’s refreshed metadata.
   */
  refresh: () => Promise<void>;
}

export interface ChannelScopeEvents {
  /**
   * Triggered when a user publishes channel changes.
   * @since 13.1
   * @event
   */
  'changes.publish': void;

  /**
   * Triggered when a user discards channel changes
   * @since 13.1
   * @event
   */
  'changes.discard': void;
}

/**
 * API to access information about and communicate with the current page
 * in the current channel shown in the Channel Manager.
 */
export interface PageScope extends Emitter<PageScopeEvents> {
  /**
   * @returns A Promise that resolves with [[PageProperties]] of the current page.
   */
  get(): Promise<PageProperties>;

  /**
   * Refreshes the page currently shown in the Channel Manager.
   */
  refresh(): Promise<void>;
}

/**
 * An emitter of events.
 */
export interface Emitter<Events> {
  /**
   * Subscribes a handler for events emitted by the CMS. The type of the
   * emitted value depends on the emitted event.
   *
   * @param eventName the name of the emitted event.
   * @param handler the function to call with the emitted value.
   *
   * @returns A function to unsubscribe the handler again.
   */
  on(eventName: keyof Events, handler: EventHandler<Events>): UnsubscribeFn;
}

/**
 * A map of all events related to a page in a channel and the type of value they emit.
 */
export interface PageScopeEvents {
  /**
   * Triggered when a user navigates to another page in the Channel Manager.
   * Emits the properties of the new page.
   * @event
   */
  navigate: PageProperties;
}

/**
 * Properties of a page in a channel.
 */
export interface PageProperties {
  /**
   * Properties of the channel the page is part of.
   */
  channel: {
    /**
     * The context path of the site application. For example "/site" or "/".
     * @since 13.2
     */
    contextPath: string;
    /**
     * The identifier of the channel. For example: "example-preview".
     */
    id: string;
    /**
     * The mount path of the channel. For example "/subsite", "/europe/nl" or an empty string for the root mount.
     * @since 13.2
     */
    mountPath: string;
  };

  /**
   * The UUID of the `hst:component` root node of the page hierarchy.
   */
  id: string;

  /**
   * Properties of the matched sitemap item.
   */
  sitemapItem: {
    /**
     * The UUID of the sitemap item.
     */
    id: string;
  };

  /**
   * The URL of the page relative to the mount path of the channel. For example "/news/mypage.html" or an empty string
   * for the home page.
   * @since 13.2
   */
  path: string;

  /**
   * The public URL of the page.
   */
  url: string;
}

/**
 * API to access information about and communicate with the current document.
 * @since 13.2
 */
export interface DocumentScope {
  /**
   * @since 13.2
   * @returns A Promise that resolves with [[DocumentProperties]] of the current document.
   */
  get(): Promise<DocumentProperties>;

  /**
   * Navigates a document path in the content perspective.
   * @since 14.2
   * @param id The document path.
   * @returns A Promise that resolves when the document is opened.
   */
  navigate(path: string): Promise<void>;

  /**
   * Opens a document by id in the content perspective.
   * @since 14.2
   * @param id The document id.
   * @returns A Promise that resolves when the document is opened.
   */
  open(id: string): Promise<void>;

  /**
   * API for the current field of the current document.
   * @since 13.2
   */
  field: FieldScope;
}

/**
 * Defines the different possible modes of a document editor.
 * @since 13.2
 */
export enum DocumentEditorMode {
  View = 'view',
  Compare = 'compare',
  Edit = 'edit',
}

/**
 * Properties of a document.
 * @since 13.2
 */
export interface DocumentProperties {
  /**
   * The UUID of the handle node.
   * @since 13.2
   */
  id: string;

  /**
   * Display name of the document.
   * @since 13.2
   */
  displayName: string;

  /**
   * Locale of the document, e.g. "sv". Is undefined when the document does not have a locale.
   * @since 13.2
   */
  locale: string;

  /**
   * The mode of the document editor.
   * @since 13.2
   */
  mode: DocumentEditorMode;

  /**
   * The URL name of the document.
   * @since 13.2
   */
  urlName: string;

  /**
   * UUID of the currently shown variant, typically 'draft' or 'preview'.
   * @since 13.2
   */
  variant: {
    id: string;
  };
}

/**
 * API to access information about and communicate with the current document field.
 * @since 13.2
 */
export interface FieldScope {
  /**
   * Gets the current field value.
   * @since 13.2
   * @return A promise that resolves with the current field value.
   */
  getValue(): Promise<string>;

  /**
   * Gets the current document field value.
   * @since 14.3
   * @param path The path is a string array pointing to the field or a nested subfield within the current document.
   *  For the multiple values fields, use zero-based indices to access specific values.
   * @return A promise that resolves with the field value.
   */
  getValue(...path: string[]): Promise<any>;

  /**
   * Gets the field value to compare the current value to.
   * Only valid when the document editor mode is [[DocumentEditorMode.Compare]].
   *
   * @since 13.2
   * @return A promise that resolves with the compare value, or null when the
   * document editor mode is not [[DocumentEditorMode.Compare]].
   */
  getCompareValue(): Promise<string>;

  /**
   * Gets field values to compare the current values to.
   * Only valid when the document editor mode is [[DocumentEditorMode.Compare]].
   *
   * @since 14.3
   * @param path The path is a string array pointing to the field or a nested subfield within the current document.
   *  For the multiple values fields, use zero-based indices to access specific values.
   * @return A promise that resolves with the field compare value, or null when the
   * document editor mode is not [[DocumentEditorMode.Compare]].
   */
  getCompareValue(...path: string[]): Promise<any>;

  /**
   * Updates current field value.
   * @since 13.2
   * @param value the new field value
   */
  setValue(value: string): Promise<void>;

  /**
   * Set the height of the surrounding iframe.
   * @since 13.2
   * @param height the number of pixels or
   *  'auto' for automatic height detection or
   *  'initial' for initial height from the config
   */
  setHeight(height: 'auto' | 'initial' | number): Promise<void>;
}

/**
 * API to open, close and communicate with dialogs.
 * @since 13.2
 */
export interface DialogScope {

  /**
   * Closes an open dialog, rejecting the promise returned by [[open]].
   * @since 13.2
   */
  cancel(): Promise<void>;

  /**
   * Closes an open dialog, resolving the promise returned by [[open]] with a value.
   * @param value The value selected in the dialog. The value should be compatible with [the structured clone
   * algorithm](https://developer.mozilla.org/en-US/docs/Web/API/Web_Workers_API/Structured_clone_algorithm).
   * @since 13.2
   */
  close(value: any): Promise<void>;

  /**
   * Opens a dialog.
   * @since 13.2
   */
  open(options: DialogProperties): Promise<void>;

  /**
   * @since 13.2
   * @returns A Promise that resolves with [[DialogProperties]] of the current dialog.
   */
  options(): Promise<DialogProperties>;
}

/**
 * Defines the different possible modes of a document editor.
 * @since 13.2
 */
export enum DialogSize {
  Large = 'large',
  Medium = 'medium',
  Small = 'small',
}

/**
 * Properties of a dialog.
 * @since 13.2
 */
export interface DialogProperties {
  /**
   * A value to pass to the dialog. For example the current field value, that can be used to preselect an item in the
   * dialog. The value should be compatible with [the structured clone
   * algorithm](https://developer.mozilla.org/en-US/docs/Web/API/Web_Workers_API/Structured_clone_algorithm).
   * @since 13.2
   */
  value?: any;

  /**
   * The size of the dialog. Defaults to [[Medium]].
   * @since 13.2
   */
  size?: DialogSize;

  /**
   * Title of the dialog.
   * @since 13.2
   */
  title: string;

  /**
   * The URL to load the dialog contents from. Can be absolute or relative to the url of the UI extension.
   * @since 13.2
   */
  url: string;
}
