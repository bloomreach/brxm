import penpal from 'penpal';

import { connectToChild } from './child';

describe('connectToChild', () => {
  beforeEach(() => {
    spyOn(penpal, 'connectToChild').and.callThrough();
  });

  it('should pass config to penpal connectToChild', () => {
    const iframe = document.createElement('iframe');
    iframe.src = 'about:blank';

    const config = {
      iframe,
      methods: {
        navigate: () => {},
      },
    };

    connectToChild(config);
    expect(penpal.connectToChild).toHaveBeenCalledWith(config);
  });

  it('should default config methods to an empty object', () => {
    const iframe = document.createElement('iframe');
    iframe.src = 'about:blank';

    const config = {
      iframe,
    };

    connectToChild(config);
    expect(penpal.connectToChild).toHaveBeenCalledWith({
      iframe,
      methods: {},
    });
  });
});
