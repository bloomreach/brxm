/*
 * Copyright 2019-2021 BloomReach. All rights reserved. (https://www.bloomreach.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * ChildConnectConfig is passed to the communication library by the navigation application
 * It provides the configuration and times out when not successful
 * Provide a set of methods to the application
 */
export interface ChildConnectConfig {
  /**
   * The html iframe element that penpal will use to see if that iframe’s window has a penpal instance to connect to.
   */
  iframe: HTMLIFrameElement;
  /**
   * The methods that a child can call in the parent window. This is specified in the API exposed by the parent window.
   * See [[ParentApi]].
   */
  methods?: ParentApi;

  /**
   * The time in ms after which an error will be thrown if the child has failed to connect
   */
  connectionTimeout?: number;

  /**
   * The time in ms after which an error will be thrown if a method doesn't return a response
   */
  methodInvocationTimeout?: number;
}

export interface ParentConnectConfig {
  /**
   * A required string to whitelist the parent origin to form a connection with.
   */
  parentOrigin: string;
  /**
   * The methods that a parent can call in the child window. This is specified in API exposed by the child window.
   */
  methods?: ChildApi;
}

export interface ParentConfig {
  /**
   * The API version the nav-app implements.
   * The version is formatted following the semantic versioning specification https://semver.org/
   */
  apiVersion: string;
  /** Object containing user settings such as username, language and timezone */
  userSettings: UserSettings;
}

export interface UserSettings {
  /** The human-readable name of the user currently logged in. */
  userName: string;
  /** email of user account if available */
  email?: string;
  /** The language the user specified at login. If the app supports that language, it should render its labels in that
   * language. Format: two lowercase alpha characters. Examples: 'en', 'fr', 'sq'.
   */
  language: string;
  /** The time zone the user specified at login. Where applicable, the app should display its date fields using that time zone.
   * Format: TZ database names in the form "Area/Location", e.g. "America/New_York" as specified by the TZDB group.
   * See https://en.wikipedia.org/wiki/List_of_tz_database_time_zones from more examples.
   */
  timeZone: string;
  /**
   * The account ID associated with the user.
   */
  accountId: string;
}

export interface ChildConfig {
  /** The exact API version the application implements. */
  apiVersion: string;
  /** Whether to show the site selection dropdown when this client app is active */
  showSiteDropdown?: boolean;
  /** The time in ms after which a method promise will be rejected if it has failed to resolve or reject */
  communicationTimeout?: number;
}

export interface ParentApi {
  /** Get the current parent config. */
  getConfig: () => Promise<ParentConfig>;
  /**
   * Is called by an application when the application performs internal navigation. updateNavLocation **does not**
   * trigger a subsequent navigate callback. Also, upon navigating to a new state (see navigate callback above), an
   * application may use this method to make sure the browser URL and breadcrumbs label maintained by the nav-app are
   * set appropriately. These parameters may depend on application-internal state.
   *
   * @param location the NavLocation navigated to
   */
  updateNavLocation: (location: NavLocation) => Promise<void>;
  /**
   * Is called by an application to perform internal or cross-app navigation. It **does** trigger the nav-app to perform a
   * ‘beforeNavigation’ and ‘navigate’ to route to the provided location.
   * **Do not set locations to other applications for navigation **
   * @param location The NavLocation navigated to
   */
  navigate: (location: NavLocation) => Promise<void>;
  /**
   * Is called by an application when it needs to cover the nav-app’s UI surface with a mask to prevent user
   * interactions for example because the user input is needed for a dialog.
   */
  showMask: () => Promise<void>;
  /**
   * Is called by an application when it needs to hide said mask again.
   */
  hideMask: () => Promise<void>;
  /**
   * Is called by an application when it needs to show the busy indicator to indicate the user has to wait until the application has
   * changed its ui state.
   * For each call to the application should make a @{link {ParentApi.hideBusyIndicator}} call.
   */
  showBusyIndicator: () => Promise<void>;
  /**
   * Is called by an application when it needs to hide the busy indicator to indicate the user can interact with the application again
   * For each call to the application should make a @{link {ParentApi.showBusyIndicator}} call.
   */
  hideBusyIndicator: () => Promise<void>;
  /**
   * Is called by an active application when a user makes some actions. Nav-app can propagate this calls to other
   * applications to prevent session expiration.
   */
  onUserActivity: () => Promise<void>;
  /**
   * Is called by an application when it detects that it's internal session has expired.
   */
  onSessionExpired: () => Promise<void>;
  /**
   * Pass on an error that occurred on the client side.
   * @param clientError object containing info about the error
   */
  onError: (clientError: ClientError) => Promise<void>;
}

export interface ChildApi {
  /**
   * Get the current [[ChildConfig]].
   */
  getConfig?: () => Promise<ChildConfig>;
  /**
   * Requests navigation items from the iframe application. Applications which register. If an application doesn’t have
   * navigation items to register, it returns an empty array.
   */
  getNavItems?: () => Promise<NavItem[]>;
  /**
   * Requests an array of sites to be used in the nested site selection drop-down of the nav-app’s toolbar.
   * The sites are expected to be provided (only) by the SM Navigation Provider app. Other apps that do not have sites
   * to provide should return an empty array.
   */
  getSites?: () => Promise<Site[]>;
  /**
   * Requests the currently selected site saved on the brSM side (as a cookie value).
   */
  getSelectedSite?: () => Promise<SiteId>;
  /**
   * Fired before nav-app initiates a navigation action which will leave the current location,
   * this event allows applications to clean up location-specific state (such as dirty forms)
   * if necessary.
   * The callback is responsible to resolve the Promise before nav-app can continue.
   * When the application is good to “leave”, it resolves the Promise with "true".
   * In order to prevent leaving the current location, the application should resolve the Promise with "false".
   */
  beforeNavigation?: () => Promise<boolean>;

  /**
   * Fired before nav-app initiates a logout broadcast, this event allows applications to clean up
   * location-specific state (such as dirty forms) if necessary.
   * The callback is responsible to resolve the Promise before nav-app can continue.
   * When the application is good to “logout”, it resolves the Promise.
   * In order to prevent the logout process, the application should reject the Promise.
   */
  beforeLogout?: () => Promise<void>;

  /**
   * Called to notify the child app that the user is still active to prevent logging out the user in one app while the
   * user is active in another app.
   */
  onUserActivity?: () => Promise<void>;
  /**
   * Called to let the child app initiate their logout process.
   */
  logout?: () => Promise<void>;
  /**
   * Is a command which tells the application to navigate to the specified location. If a user triggers a navigation
   * (for example by clicking a button) but the url is absolutely the same it doesn’t lead to any transitions.
   * It’s an application’s responsibility to make a decision on how to handle this navigational call.
   *
   * @param location the NavLocation to navigate to
   * @param triggeredBy The source of the navigate call so the client app is able to react appropriately on a specific source
   */
  navigate?: (location: NavLocation, triggeredBy: NavigationTrigger) => Promise<void>;
  /**
   * Sets the accountId (merchantId) and siteId (siteGroupId) to work with. Site selection is intentionally separated
   * from navigation since it represents an additional dimension in the navigation process to cover brSM needs.
   * When a user selects a site, nav-app will call this callback for the active app first, which can update the site
   * and the corresponding cookie and then resolves the promise, then the nav-app broadcast to all apps to update the
   * site. Nav-app will only wait for the Promise of the currently active app to resolve before updating the UI to
   * display the newly selected site. The selectSite should update the site state in the child UI based on whether the
   * selectSite is passed a site object. If a site object is passed the child UI is updated to that site object and
   * the cookie is updated to reflect the new site, if no site object is passed, the site is updated from the
   * cookie. This way the active app will first update its UI and cookie, and the consequent broadcast will cause all
   * other brSM apps to update their site state from the cookie that was just updated.
   *
   * @param siteId the selected SiteId
   */
  updateSelectedSite?: (siteId?: SiteId) => Promise<void>;
}

export interface NavItem {
  /**
   * Unique identifier for the navigation item. Uniqueness should be taken care of by a unique application-prefix,
   * followed by a suffix unique within that application. Non-unique navigation items will be dropped at registration
   * time. navigationItems with id unknown to the nav-app will be put in a “Extensions” menu item (sorted by their
   * displayName).
   */
  id: string;
  /**
   * Optional Human-readable name to represent the navigation item in the menu. Where available/applicable,
   * displayName must be localized to the logged-in user’s locale, before the navigation item is provided to nav-app.
   */
  displayName?: string;
  /**
   * Absolute URL for loading the app which hosts this navigation item. navigation items with the same appBaseUrl value
   * belong to the same app, and only one iframe is loaded for that app. The value of this field will be put into the
   * src attribute of that iframe.
   */
  appIframeUrl: string;
  /**
   * Field expressing the navigation item’s path inside the app referred to by the appBaseUrl field. This value has no
   * meaning to nav-app, it is used to pass to the corresponding app in order to navigate to the desired
   * path/route/location. Also, when an app signals that it has internally navigated to a different location, the
   * navigation application compares that path to the path of all navigation items for that application, in order to
   * update which navigation item is currently active. The path value must be unique across all navigation items for a
   * given application (identified by appBaseUrl), AND for all navigation items of an application, no path can be a
   * prefix-match (startsWith) of another path.
   */
  appPath: string;
}

export interface NavLocation {
  /** Value of the ‘path’ field of the selected navigation item. */
  path: string;
  /**
   * Optional: The prefix to the path. It equals a appBaseUrl.pathname of some navigation items. Can be used to
   * identify, in combination with path, the navigation item to navigate to.
   */
  pathPrefix?: string;
  /** Optional: This field is only used when an application updates the nav-app location. It may choose to specify
   * a (translated, where appropriate) label nav-app should append to its breadcrumbs.
   */
  breadcrumbLabel?: string;
  /** Optional: This field is only used when an application updates the nav-app location.
   *  When set to true it will add a new history item to the browser history instead of replacing the current item.
   */
  addHistory?: boolean;
}

export interface SiteId {
  /**
   * Identifier of a site group or a site.
   */
  siteId: number;
  /**
   * Identifier of the account.
   */
  accountId: number;
}

export interface Site extends SiteId {
  /**
   * Name of the site, to be used for displaying the drop-down option.
   */
  name: string;
  /**
   * Whether navapp or iUI is enabled for a Site
   */
  isNavappEnabled: boolean;
  /**
   * Optional field for grouping sub-sites (2nd and 3rd level nesting). Nav-app will ignore deeper nesting levels,
   * because there is no design on how to display them to the user. When selecting a Site, nav-app shall **not** include
   * this field in the object passed to the app(s).
   */
  subGroups?: Site[];
}

export enum ClientErrorCodes {
  /**
   * This code should be used if it’s impossible to determine an error. The navapp shows something like
   * “Something went wrong” without additional description in that case.
   */
  UnknownError = 0,
  /**
   * This can be used if the client app wants to reject an api call from the parent.
   */
  GenericCommunicationError,
  /**
   * This code should be used if SPA receives 403 HTTP error while attempting to access the server side.
   */
  NotAuthorizedError = 403,
  /**
   * This code should be used If the appPath provided through navigate call is impossible to recognise.
   */
  PageNotFoundError = 404,
  /**
   * This code should be used if some internal error occurred on the server side.
   */
  InternalError = 500,
  /**
   * This code should be used if SPA receives 500 (or similar) HTTP errors while attempting to access the server side.
   */
  UnableToConnectToServerError,
}

export interface ClientError {
  /**
   * Predefined error code from the error constants
   */
  errorCode: ClientErrorCodes;
  /**
   * The error type can be either "blocking" or "lenient". If an error is considered "blocking", the user should not
   * be able to continue. If an error is considered "lenient", it should only indicate to the user that an error
   * occurred, but never block the user.
   */
  errorType?: string;
  /**
   * Human readable message that can be displayed to the user
   */
  message?: string;
}

/**
 * The source of the navigate call
 */
export enum NavigationTrigger {
  NotDefined = 'NotDefined',
  InitialNavigation = 'InitialNavigation',
  Menu = 'Menu',
  Breadcrumbs = 'Breadcrumbs',
  FastTravel = 'FastTravel',
  AnotherApp = 'AnotherApp',
  PopState = 'PopState',
}
