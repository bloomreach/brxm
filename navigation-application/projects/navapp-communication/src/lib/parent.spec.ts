import penpal from 'penpal';

import { connectToParent } from './parent';

describe('connectToParent', () => {
  beforeEach(() => {
    spyOn(penpal, 'connectToParent').and.callThrough();
    spyOn(console, 'log');
  });

  it('should pass config to penpal connectToParent', () => {
    const parentOrigin = 'about:blank';
    const config = {
      parentOrigin,
      methods: {
        navigate: () => {},
      },
    };

    connectToParent(config);
    expect(penpal.connectToParent).toHaveBeenCalledWith(config);
  });

  it('should default config methods to an empty object', () => {
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

  describe('proxied methods', () => {
    describe('getNavItems', () => {
      it('should log "Proxied method" on calling getNavItems', () => {
        const parentOrigin = 'about:blank';
        const config = {
          parentOrigin,
          methods: jasmine.createSpyObj(['getNavItems']),
        };

        connectToParent(config);
        expect(penpal.connectToParent).toHaveBeenCalled();
      });
    });
  });
});
