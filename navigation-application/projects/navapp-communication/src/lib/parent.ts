import penpal from 'penpal';

import {
  ChildApi,
  NavItem,
  ParentConnectConfig,
  ParentPromisedApi,
} from './api';
import { mergeIntersecting } from './utils';

export function connectToParent({
  parentOrigin,
  methods = {},
}: ParentConnectConfig): Promise<ParentPromisedApi> {
  const proxies: ChildApi = {
    getNavItems(): NavItem[] {
      console.log('Proxied method');
      return methods.getNavItems();
    },
  };
  const proxiedMethods = mergeIntersecting(methods, proxies);

  return penpal.connectToParent({ parentOrigin, methods: proxiedMethods })
    .promise;
}
