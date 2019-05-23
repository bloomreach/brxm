import Penpal from 'penpal';

import { connectToChild } from './child';

describe('connectToChild', () => {
  beforeEach(() => {
    spyOn(Penpal, 'connectToChild').and.callThrough();
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
    expect(Penpal.connectToChild).toHaveBeenCalledWith(config);
  });

  it('should default the config methods to an empty object', () => {
    const iframe = document.createElement('iframe');
    iframe.src = 'about:blank';

    const config = {
      iframe,
    };

    connectToChild(config);
    expect(Penpal.connectToChild).toHaveBeenCalledWith({
      iframe,
      methods: {},
    });
  });
});
