import Penpal from 'penpal';

import {
  ChildApi,
  NavItem,
  NavLocation,
  ParentConnectConfig,
  ParentPromisedApi,
} from './api';
import { mergeIntersecting } from './utils';

export function createProxies(methods: ChildApi): ChildApi {
  return {
    getNavItems(): NavItem[] {
      return methods.getNavItems();
    },
    navigate(location: NavLocation): void {
      return methods.navigate(location);
    },
  };
}

export function connectToParent({
  parentOrigin,
  methods = {},
}: ParentConnectConfig): Promise<ParentPromisedApi> {
  const proxies: ChildApi = createProxies(methods);
  const proxiedMethods = mergeIntersecting(methods, proxies);

  return Penpal.connectToParent({ parentOrigin, methods: proxiedMethods })
    .promise;
}
