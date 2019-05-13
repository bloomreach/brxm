import penpal from 'penpal';

import {
  NavItem,
  NavLocation,
  ParentConnectConfig,
  ParentPromisedApi,
} from './api';

export function connectToParent({
  parentOrigin,
  methods,
}: ParentConnectConfig): Promise<ParentPromisedApi> {
  const proxyConfig: ParentConnectConfig = {
    parentOrigin,
    methods: {
      navigate(location: NavLocation): void {
        return methods.navigate(location);
      },
      getNavItems(): NavItem[] {
        return methods.getNavItems();
      },
    },
  };

  return penpal.connectToParent(proxyConfig).promise;
}
