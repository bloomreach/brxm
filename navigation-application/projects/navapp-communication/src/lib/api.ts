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
  iframe: HTMLIFrameElement;
  methods?: ParentApi;
}

export interface ParentConnectConfig {
  parentOrigin: string;
  methods?: ChildApi;
}

export interface ParentConfig {
  apiVersion: string;
  username: string;
  language: string;
  timezone: string;
}

export interface ChildConfig {
  apiVersion: string;
  showSiteDropdown: boolean;
}

export interface ParentApi {
  getConfig?: () => ParentConfig;
  updateNavLocation?: (location: NavLocation) => void;
  navigate?: (location: NavLocation) => void;
  showMask?: () => void;
  hideMask?: () => void;
  onUserActivity?: () => void;
  onSessionExpired?: () => void;
}

export interface ParentPromisedApi {
  getConfig?: () => Promise<ParentConfig>;
  updateNavLocation?: (location: NavLocation) => Promise<void>;
  navigate?: (location: NavLocation) => Promise<void>;
  showMask?: () => Promise<void>;
  hideMask?: () => Promise<void>;
  onUserActivity?: () => Promise<void>;
  onSessionExpired?: () => Promise<void>;
}

export interface ChildApi {
  getConfig?: () => ChildConfig;
  getNavItems?: () => NavItem[];
  getSites?: () => Site[];
  getSelectedSite?: () => SiteId;
  beforeNavigation?: () => boolean;
  onUserActivity?: () => void;
  logout?: () => void;
  navigate?: (location: NavLocation) => void;
  updateSelectedSite?: (siteId?: SiteId) => void;
}

export interface ChildPromisedApi {
  getConfig?: () => Promise<ChildConfig>;
  getNavItems?: () => Promise<NavItem[]>;
  getSites?: () => Promise<Site[]>;
  getSelectedSite?: () => Promise<SiteId>;
  beforeNavigation?: () => Promise<boolean>;
  onUserActivity?: () => Promise<void>;
  logout?: () => Promise<void>;
  navigate?: (location: NavLocation, flags?: { [key: string]: string | number | boolean }) => Promise<void>;
  updateSelectedSite?: (siteId?: SiteId) => Promise<void>;
}

export interface NavItem {
  id: string;
  displayName?: string;
  appIframeUrl: string;
  appPath: string;
}

export interface NavLocation {
  path: string;
  pathPrefix?: string;
  pathSuffix?: string;
  search?: string;
  hash?: string;
  breadcrumbLabel?: string;
}

export interface SiteId {
  siteId: number;
  accountId: number;
}

export interface Site extends SiteId {
   name: string;
   subGroups?: Site[];
 }
