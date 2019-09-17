/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

export interface ChildConnectConfig {
  /**
   * The html iframe element that penpal should use to see if that iframe’s window has a penpal instance to connect to.
   */
  iframe: HTMLIFrameElement;
  /**
   * The methods that a child can call in the parent window. This is specified in the API exposed by the parent window. See [[ParentApi]].
   */
  methods?: ParentApi;
  timeout?: number;
}

export interface ParentConnectConfig {
  /**
   * A required string to whitelist the parent origin to form a connection with.
   * This origin is assumed to be provided to an app via the SSO solution **(specifics are still unclear)**.
   */
  parentOrigin: string;
  /**
   * The methods that a parent can call in the child window. This is specified in API exposed by the child window.
   */
  methods?: ChildApi;
}

export interface ParentConfig {
  /** The exact API version nav-app implements. */
  apiVersion: string;
  /** The human-readable name of the user currently logged in. */
  username: string;
  /** The language the user specified at login. If the app supports that language, it should render its labels in that language. */
  language: string;
  /** The time zone the user specified at login. Where applicable, the app should display its date fields using that time zone. */
  timezone: string;
}

export interface ChildConfig {
  /** The exact API version the application implements. */
  apiVersion?: string;
  showSiteDropdown?: boolean;
  communicationTimeout?: number;
}

export interface ParentApi {
  /** Get the current parent config. */
  getConfig?: () => (ParentConfig | Promise<ParentConfig>);
  /**
   * Is called by an application when the application performs internal navigation. updateNavLocation **does not** trigger
   * a subsequent navigate callback. Also, upon navigating to a new state (see navigate callback above), an application
   * may use this method to make sure the browser URL and breadcrumbs label maintained by the nav-app are set appropriately.
   * These parameters may depend on application-internal state.
   *
   * @param location the NavLocation navigated to
   */
  updateNavLocation?: (location: NavLocation) => (void | Promise<void>);
  /**
   * Is called by an application to perform internal or cross-app navigation. It **does** trigger the nav-app to perform a
   * ‘beforeNavigate’ and ‘navigate’ to route to the provided location.
   *
   * @param location The NavLocation navigated to
   */
  navigate?: (location: NavLocation) => (void | Promise<void>);
  /**
   * Is called by an application when it needs to cover the nav-app’s UI surface with a mask to prevent user interactions
   * for example because the user input is needed for a dialog.
   */
  showMask?: () => (void | Promise<void>);
  /**
   * Is called by an application when it needs to hide said mask again.
   */
  hideMask?: () => (void | Promise<void>);
  /**
   * Is called by an active application when a user makes some actions. Nav-app can propagate this calls to other
   * applications to prevent session expiration.
   */
  onUserActivity?: () => (void | Promise<void>);
  onSessionExpired?: () => (void | Promise<void>);
  requestLogout?: () => (void | Promise<void>);
}

export interface ParentPromisedApi {
  getConfig?: () => Promise<ParentConfig>;
  updateNavLocation?: (location: NavLocation) => Promise<void>;
  navigate?: (location: NavLocation) => Promise<void>;
  showMask?: () => Promise<void>;
  hideMask?: () => Promise<void>;
  onUserActivity?: () => Promise<void>;
  onSessionExpired?: () => Promise<void>;
  requestLogout?: () => Promise<void>;
}

export interface ChildApi {
  /**
   * Get the current [[ChildConfig]].
   */
  getConfig?: () => (ChildConfig | Promise<ChildConfig>);
  /**
   * Requests navigation items from the iframe application. Applications which register. If an application doesn’t have
   * navigation items to register, it returns an empty array.
   */
  getNavItems?: () => (NavItem[] | Promise<NavItem[]>);
  /**
   * Requests an array of sites to be used in the nested site selection drop-down of the nav-app’s toolbar.
   * The sites are expected to be provided (only) by the SM Navigation Provider app. Other apps that do not have sites
   * to provide should return an empty array.
   */
  getSites?: () => (Site[] | Promise<Site[]>);
  /**
   * Requests the currently selected site saved on the brSM side (as a cookie value).
   */
  getSelectedSite?: () => (SiteId | Promise<SiteId>);
  /**
   * Fired before nav-app initiates a navigation action which will leave the current location, this event allows
   * applications to clean up location-specific state (such as dirty forms) if necessary. The callback is responsible
   * to resolve the Promise before nav-app can continue. When the application is good to “leave”, it resolves the Promise
   * with a value of `true`. In order to prevent leaving the current location, the application should resolve the Promise
   * with a value of `false`.
   */
  beforeNavigation?: () => (boolean | Promise<boolean>);
  /**
   * Called to notify the child app that the user is still active to prevent logging out the user in one app while the
   * user is active in another app.
   */
  onUserActivity?: () => (void | Promise<void>);
  /**
   * Called to let the child app initiate their logout process.
   */
  logout?: () => (void | Promise<void>);
  /**
   * Is a command which tells the application to navigate to the specified location. If a user triggers a navigation
   * (for example by clicking a button) but the url is absolutely the same it doesn’t lead to any transitions.
   * It’s an application’s responsibility to make a decision on how to handle this navigational call.
   *
   * @param location the NavLocation to navigate to
   * @param flags TODO document
   */
  navigate?: (location: NavLocation, flags?: NavigateFlags) => (void | Promise<void>);
  /**
   * Sets the accountId (merchantId) and siteId (siteGroupId) to work with. Site selection is intentionally separated
   * from navigation since it represents an additional dimension in the navigation process to cover brSM needs.
   * When a user selects a site, nav-app will call this callback for the active app first, which can update the site and
   * the corresponding cookie and then resolves the promise, then the nav-app broadcast to all apps to update the site.
   * Nav-app will only wait for the Promise of the currently active app to resolve before updating the UI to display the
   * newly selected site. The selectSite should update the site state in the child UI based on whether the selectSite is
   * passed a site object. If a site object is passed the child UI is updated to that site object and **the cookie is
   * updated** to reflect the new site, if no site object is passed, **the site is updated from the cookie**. This way the
   * active app will first update its UI and cookie, and the consequent broadcast will cause all other brSM apps to
   * update their site state from the cookie that was just updated.
   *
   * @param siteId the selected SiteId
   */
  updateSelectedSite?: (siteId?: SiteId) => (void | Promise<void>);
}

export interface ChildPromisedApi {
  getConfig?: () => Promise<ChildConfig>;
  getNavItems?: () => Promise<NavItem[]>;
  getSites?: () => Promise<Site[]>;
  getSelectedSite?: () => Promise<SiteId>;
  beforeNavigation?: () => Promise<boolean>;
  onUserActivity?: () => Promise<void>;
  logout?: () => Promise<void>;
  navigate?: (location: NavLocation, flags?: NavigateFlags) => Promise<void>;
  updateSelectedSite?: (siteId?: SiteId) => Promise<void>;
}

export interface NavigateFlags {
  [key: string]: string | number | boolean;
}

export interface NavItem {
  /**
   * Unique identifier for the navigation item. Uniqueness should be taken care of by a unique application-prefix,
   * followed by a suffix unique within that application. Non-unique navigation items will be dropped at registration time.
   * navigationItems with id unknown to the nav-app will be put in a “Extensions” menu item (sorted by their displayName).
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
   * meaning to nav-app, it is used to pass to the corresponding app in order to navigate to the desired path/route/location.
   * Also, when an app signals that it has internally navigated to a different location, the navigation application
   * compares that path to the path of all navigation items for that application, in order to update which navigation
   * item is currently active. The path value must be unique across all navigation items for a given
   * application (identified by appBaseUrl), AND for all navigation items of an application, no path can be a
   * prefix-match (startsWith) of another path.
   */
  appPath: string;
}

export interface NavLocation {
  /** Value of the ‘path’ field of the selected navigation item. */
  path: string;
  /**
   * Optional: The prefix to the path. It equals a appBaseUrl.pathname of some navigation items. Can be used to identify,
   * in combination with path, the navigation item to navigate to.
   */
  pathPrefix?: string;
  /** Optional: Suffix of the browser URL, starting after the closest matching navigation item path from a navigation item,
   * up to and not including the search. To be used to give optional additional information to an app for its router.
   * The application could interpret the value of this field (which may be an empty string) to navigate to the correct
   * “sub-state” of the specified navigation item.
   */
  pathSuffix?: string;
  /** Optional: Any query parameters found in the browser URL. */
  search?: string;
  /** Optional: Any hash found in the browser URL. */
  hash?: string;
  /** Optional: This field is only used when an application updates the nav-app location. It may choose to specify
   * a (translated, where appropriate) label nav-app should append to its breadcrumbs.
   */
  breadcrumbLabel?: string;
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
   * Optional field for grouping sub-sites (2nd and 3rd level nesting). Nav-app will ignore deeper nesting levels,
   * because there is no design on how to display them to the user. When selecting a Site, nav-app shall **not** include
   * this field in the object passed to the app(s).
   */
  subGroups?: Site[];
}
