import penpal from 'penpal';

import { ChildConnectConfig, ChildPromisedApi, ParentApi } from './api';
import { getProxiedMethods } from './utils';

export function connectToChild({
  iframe,
  methods = {},
}: ChildConnectConfig): Promise<ChildPromisedApi> {
  const proxies: ParentApi = {};
  const proxiedMethods = getProxiedMethods(methods, proxies);

  return penpal.connectToChild({ iframe, methods: proxiedMethods }).promise;
}
