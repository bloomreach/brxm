import { ChildApi, ParentApi } from './api';

export function getProxiedMethods(
  methods: ChildApi | ParentApi,
  proxies: ChildApi | ParentApi,
): ChildApi | ParentApi {
  return Object.keys(methods).reduce((obj, key) => {
    if (key in proxies) {
      obj[key] = proxies[key];
    }

    return obj;
  }, {});
}
