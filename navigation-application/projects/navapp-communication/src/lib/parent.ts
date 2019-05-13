import penpal from 'penpal';

import { NavLocation, ParentApi, ParentConnectConfig } from './api';

export function connectToParent({
  parentOrigin,
  methods,
}: ParentConnectConfig): Promise<ParentApi> {
  const proxyConfig: ParentConnectConfig = {
    parentOrigin,
    methods: {
      navigate(location: NavLocation): any {
        return methods.navigate(location);
      },
      getNavItems(): any {
        return methods.getNavItems();
      },
    },
  };

  return penpal.connectToParent(proxyConfig).promise;
}
