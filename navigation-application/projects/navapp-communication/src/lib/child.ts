import penpal from 'penpal';

import { ChildApi, ChildConnectConfig } from './api';

export function connectToChild({
  iframe,
  methods,
}: ChildConnectConfig): Promise<ChildApi> {
  const proxyConfig: ChildConnectConfig = {
    iframe,
    methods,
  };

  return penpal.connectToChild(proxyConfig).promise;
}
