export interface ChildConnectConfig {
  iframe: HTMLElement;
  methods: ParentApi;
}

export interface ParentConnectConfig {
  parentOrigin: string;
  methods: ChildApi;
}

export interface ParentApi {
  navigate?: (location: NavLocation) => void;
}

export interface ChildApi {
  navigate: (location: NavLocation) => void;
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
