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
        console.log('navigating to location', location);
        return methods.navigate(location);
      },
    },
  };

  return penpal.connectToParent(proxyConfig).promise;
}
