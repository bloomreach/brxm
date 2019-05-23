import Penpal from 'penpal';

import { ChildConnectConfig, ChildPromisedApi, ParentApi } from './api';
import { mergeIntersecting } from './utils';

export function connectToChild({
  iframe,
  methods = {},
}: ChildConnectConfig): Promise<ChildPromisedApi> {
  const proxies: ParentApi = {};
  const proxiedMethods = mergeIntersecting(methods, proxies);

  return Penpal.connectToChild({ iframe, methods: proxiedMethods }).promise;
}
