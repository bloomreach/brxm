/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Disable TSLint for imports that start with an uppercase letter
 * @see https://github.com/Microsoft/tslint-microsoft-contrib/issues/387
 */
import Emittery = require('emittery'); // tslint:disable-line:import-name
import { parent } from './__mocks__/penpal';
import { DialogProperties, PageProperties } from './api';
import { ParentConnection } from './parent';
import { Ui } from './ui';

const observe = jest.fn();
const disconnect = jest.fn();

window['MutationObserver'] = class {
  constructor(callback: () => {}) {}
  observe(element: HTMLElement, init: MutationObserverInit) {
    observe(element, init);
    return () => this.disconnect();
  }
  disconnect() { disconnect(); }
};

afterEach(() => {
  observe.mockClear();
  disconnect.mockClear();
});

describe('Ui.init()', () => {
  let parentConnection: ParentConnection;
  let eventEmitter: Emittery;
  let ui: Ui;

  beforeEach(() => {
    parentConnection = new ParentConnection(parent);
    eventEmitter = new Emittery();

    ui = new Ui(parentConnection, eventEmitter);
    return ui.init();
  });

  it('initializes the UI properties', () => {
    expect(ui.baseUrl).toBe('https://cms.example.com');
    expect(ui.extension.config).toBe('testConfig');
    expect(ui.locale).toBe('en');
    expect(ui.styling).toBe('classic');
    expect(ui.timeZone).toBe('Europe/Amsterdam');
    expect(ui.user.id).toBe('admin');
    expect(ui.user.firstName).toBe('Ad');
    expect(ui.user.lastName).toBe('Min');
    expect(ui.user.displayName).toBe('Ad Min');
    expect(ui.version).toBe('13.0.0');
  });

  describe('ui.channel.refresh()', () => {
    it('refreshes the current channel', async () => {
      parentConnection.call = jest.fn().mockReturnValue(Promise.resolve());
      await ui.channel.refresh();
      expect(parentConnection.call).toHaveBeenCalledWith('refreshChannel');
    });
  });

  describe('ui.channel.page.get()', () => {
    it('returns the current page', async () => {
      const page = await ui.channel.page.get();
      expect(page.channel.contextPath).toBe('/site');
      expect(page.channel.id).toBe('testChannelId');
      expect(page.channel.mountPath).toBe('/sub-mount');
      expect(page.id).toBe('testPageId');
      expect(page.sitemapItem.id).toBe('testSitemapItemId');
      expect(page.path).toBe('/news/mypage.html');
      expect(page.url).toBe('http://www.example.com/site/sub-mount/news/mypage.html');
    });
  });

  describe('ui.channel.page.refresh()', () => {
    it('refreshes the current page', async () => {
      parentConnection.call = jest.fn().mockReturnValue(Promise.resolve());
      await ui.channel.page.refresh();
      expect(parentConnection.call).toHaveBeenCalledWith('refreshPage');
    });
  });

  describe('ui.channel.page.on(\'navigate\', listener)', () => {
    let nextPage: PageProperties;

    beforeEach(() => {
      nextPage = {
        channel: {
          contextPath: '/',
          id: 'channelId',
          mountPath: '',
        },
        id: 'pageId',
        sitemapItem: {
          id: 'sitemapItemId',
        },
        path: '/news/mypage.html',
        url: 'http://www.example.com/news/mypage.html',
      };
    });

    it('calls the listener whenever the parent emits a \'channel.page.navigate\' event', async () => {
      const listener = jest.fn();

      ui.channel.page.on('navigate', listener);

      await eventEmitter.emit('channel.page.navigate', nextPage);
      expect(listener).toHaveBeenCalledWith(nextPage);
    });

    it('returns an unbind function', async () => {
      const listener = jest.fn();

      const unbind = ui.channel.page.on('navigate', listener);
      unbind();

      await eventEmitter.emit('channel.page.navigate', nextPage);
      expect(listener).not.toHaveBeenCalled();
    });
  });

  describe('ui.document.get()', () => {
    it('returns the current document properties', async () => {
      parentConnection.call = jest.fn().mockReturnValue(Promise.resolve({ id: 'test' }));
      const documentProperties = await ui.document.get();

      expect(parentConnection.call).toHaveBeenCalledWith('getDocument');
      expect(documentProperties).toEqual({ id: 'test' });
    });
  });

  describe('ui.document.field', () => {
    beforeEach(() => { ui.document.field; });

    it('reacts on focus event', async () => {
      parentConnection.call = jest.fn().mockReturnValue(Promise.resolve());

      window.dispatchEvent(new Event('focus'));
      expect(parentConnection.call).toHaveBeenCalledWith('emitEvent', 'focus');
    });

    it('reacts on blur event', async () => {
      parentConnection.call = jest.fn().mockReturnValue(Promise.resolve());

      window.dispatchEvent(new Event('blur'));
      expect(parentConnection.call).toHaveBeenCalledWith('emitEvent', 'blur');
    });
  });

  describe('ui.document.field.getValue()', () => {
    it('returns the current field value', async () => {
      parentConnection.call = jest.fn().mockReturnValue(Promise.resolve('test'));

      const value = await ui.document.field.getValue();

      expect(parentConnection.call).toHaveBeenCalledWith('getFieldValue');
      expect(value).toEqual('test');
    });
  });

  describe('ui.document.field.getCompareValue()', () => {
    it('returns the previous field value', async () => {
      parentConnection.call = jest.fn().mockReturnValue(Promise.resolve('test'));

      const compareValue = await ui.document.field.getCompareValue();

      expect(parentConnection.call).toHaveBeenCalledWith('getFieldCompareValue');
      expect(compareValue).toEqual('test');
    });
  });

  describe('ui.document.field.setValue()', () => {
    it('sets the current field value', async () => {
      parentConnection.call = jest.fn().mockReturnValue(Promise.resolve());
      await ui.document.field.setValue('test');
      expect(parentConnection.call).toHaveBeenCalledWith('setFieldValue', 'test');
    });
  });

  describe('ui.document.field.setHeight()', () => {
    it('sets the field height', async () => {
      parentConnection.call = jest.fn().mockReturnValue(Promise.resolve());
      await ui.document.field.setHeight(100);
      expect(parentConnection.call).toHaveBeenCalledWith('setFieldHeight', 100);
    });

    it('resets the cached height used by updateHeight()', async () => {
      parentConnection.call = jest.fn().mockReturnValue(Promise.resolve());
      Object.defineProperty(document.body, 'scrollHeight', { value: 42 });

      await ui.document.field.updateHeight();
      await ui.document.field.setHeight(100);
      await ui.document.field.updateHeight();

      expect(parentConnection.call).toHaveBeenNthCalledWith(1, 'setFieldHeight', 42);
      expect(parentConnection.call).toHaveBeenNthCalledWith(2, 'setFieldHeight', 100);
      expect(parentConnection.call).toHaveBeenNthCalledWith(3, 'setFieldHeight', 42);
    });
  });

  describe('ui.document.field.updateHeight()', () => {
    it('sets the field height to the value of document.body.scrollHeight', async () => {
      parentConnection.call = jest.fn().mockReturnValue(Promise.resolve());
      Object.defineProperty(document.body, 'scrollHeight', { value: 42 });
      await ui.document.field.updateHeight();
      expect(parentConnection.call).toHaveBeenCalledWith('setFieldHeight', 42);
    });

    it('does not set the field height if it has not changed', async () => {
      parentConnection.call = jest.fn().mockReturnValue(Promise.resolve());
      Object.defineProperty(document.body, 'scrollHeight', { value: 42 });
      await ui.document.field.updateHeight();
      await ui.document.field.updateHeight();
      expect(parentConnection.call).toHaveBeenCalledWith('setFieldHeight', 42);
      expect(parentConnection.call).toHaveBeenCalledTimes(1);
    });
  });

  describe('ui.document.field.autoUpdateHeight()', () => {
    it('starts a MutationObserver for document.body', async () => {
      await ui.document.field.autoUpdateHeight();
      expect(observe).toHaveBeenCalledWith(document.body, {
        attributes: true,
        characterData: true,
        childList: true,
        subtree: true,
      });
    });

    it('hides vertical overflow on document.body to prevent vertical scrollbars', async () => {
      await ui.document.field.autoUpdateHeight();
      expect(document.body.style.overflowY).toBe('hidden');
    });

    describe('and the returned function', () => {
      it('disconnects the MutationObserver', async () => {
        const stopFn = await ui.document.field.autoUpdateHeight();
        stopFn();
        expect(disconnect).toHaveBeenCalled();
      });

      it('restores the original vertical overflow on document.body', async () => {
        document.body.style.overflowY = 'scroll';
        const stopFn = await ui.document.field.autoUpdateHeight();
        stopFn();
        expect(document.body.style.overflowY).toBe('scroll');
      });
    });
  });

  describe('ui.dialog.cancel()', () => {
    it('cancels an open dialog', async () => {
      parentConnection.call = jest.fn().mockReturnValue(Promise.resolve());
      await ui.dialog.cancel();
      expect(parentConnection.call).toHaveBeenCalledWith('cancelDialog');
    });
  });

  describe('ui.dialog.close()', () => {
    it('closes an open dialog', async () => {
      parentConnection.call = jest.fn().mockReturnValue(Promise.resolve());
      const transferable = { value: 'test value' };
      await ui.dialog.close(transferable);
      expect(parentConnection.call).toHaveBeenCalledWith('closeDialog', transferable);
    });
  });

  describe('ui.dialog.open()', () => {
    it('opens a dialog', async () => {
      parentConnection.call = jest.fn().mockReturnValue(Promise.resolve());
      const dialogProperties = {} as DialogProperties;
      await ui.dialog.open(dialogProperties);
      expect(parentConnection.call).toHaveBeenCalledWith('openDialog', dialogProperties);
    });
  });

  describe('ui.dialog.options()', () => {
    it('gets the dialog options', async () => {
      parentConnection.call = jest.fn().mockReturnValue(Promise.resolve());
      await ui.dialog.options();
      expect(parentConnection.call).toHaveBeenCalledWith('getDialogOptions');
    });
  });
});
