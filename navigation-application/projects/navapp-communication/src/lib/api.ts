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
  beforeNavigation?: () => boolean;
  onUserActivity?: () => void;
  logout?: () => void;
  navigate?: (location: NavLocation) => void;
  selectSite?: (site?: Site) => void;
}

export interface ChildPromisedApi {
  getConfig?: () => Promise<ChildConfig>;
  getNavItems?: () => Promise<NavItem[]>;
  getSites?: () => Promise<Site[]>;
  beforeNavigation?: () => Promise<boolean>;
  onUserActivity?: () => Promise<void>;
  logout?: () => Promise<void>;
  navigate?: (location: NavLocation) => Promise<void>;
  selectSite?: (site?: Site) => Promise<void>;
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

export interface Site {
   id: number;
   name: string;
   subGroups?: Site[];
 }
