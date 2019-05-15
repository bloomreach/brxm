import penpal from 'penpal';

import { connectToParent, createProxies } from './parent';

describe('connectToParent', () => {
  beforeEach(() => {
    spyOn(penpal, 'connectToParent').and.callThrough();
  });

  it('should pass config to penpal connectToParent', () => {
    const parentOrigin = 'about:blank';
    const methods = {
      logout: () => {},
    };

    const config = {
      parentOrigin,
      methods,
    };

    connectToParent(config);
    expect(penpal.connectToParent).toHaveBeenCalledWith(config);
  });

  it('should default the config methods to an empty object', () => {
    const parentOrigin = 'about:blank';
    const config = {
      parentOrigin,
    };

    connectToParent(config);
    expect(penpal.connectToParent).toHaveBeenCalledWith({
      parentOrigin,
      methods: {},
    });
  });

  it('should proxy methods', () => {
    const methods = {
      navigate: jasmine.createSpy('navigate'),
      getNavItems: jasmine.createSpy('getNavItems'),
    };
    const proxies = createProxies(methods);

    proxies.getNavItems();
    proxies.navigate({ path: 'test' });

    expect(proxies.navigate).not.toBe(methods.navigate);

    expect(methods.navigate).toHaveBeenCalled();
    expect(methods.getNavItems).toHaveBeenCalled();
  });

  it('should pass proxied methods if available', () => {
    const parentOrigin = 'about:blank';
    const methods = {
      navigate: () => {},
    };
    const config = {
      parentOrigin,
      methods,
    };

    connectToParent(config);
    expect(penpal.connectToParent).toHaveBeenCalled();
    expect(penpal.connectToParent).not.toHaveBeenCalledWith(config); // So therefore the proxy is called.
  });
});
