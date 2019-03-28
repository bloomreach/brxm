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
import { PageProperties } from './api';
import { ParentConnection } from './parent';
import { Ui } from './ui';

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
    expect(ui.timeZone).toBe('Europe/Amsterdam');
    expect(ui.user.id).toBe('admin');
    expect(ui.user.firstName).toBe('Ad');
    expect(ui.user.lastName).toBe('Min');
    expect(ui.user.displayName).toBe('Ad Min');
    expect(ui.version).toBe('13.0.0');
  });

  describe('ui.channel.refresh()', () => {
    it('refreshes the current channel', async () => {
      parentConnection.call = jest.fn(() => Promise.resolve());
      await ui.channel.refresh();
      expect(parentConnection.call).toHaveBeenCalledWith('refreshChannel');
    });
  });

  describe('ui.channel.page.get()', () => {
    it('returns the current page', async () => {
      const page = await ui.channel.page.get();
      expect(page.channel.id).toBe('testChannelId');
      expect(page.id).toBe('testPageId');
      expect(page.sitemapItem.id).toBe('testSitemapItemId');
      expect(page.url).toBe('http://www.example.com');
    });
  });

  describe('ui.channel.page.refresh()', () => {
    it('refreshes the current page', async () => {
      parentConnection.call = jest.fn(() => Promise.resolve());
      await ui.channel.page.refresh();
      expect(parentConnection.call).toHaveBeenCalledWith('refreshPage');
    });
  });

  describe('ui.channel.page.on(\'navigate\', listener)', () => {
    let nextPage: PageProperties;

    beforeEach(() => {
      nextPage = {
        channel: {
          id: 'channelId',
        },
        id: 'pageId',
        sitemapItem: {
          id: 'sitemapItemId',
        },
        url: 'http://www.example.com/page',
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

  describe('ui.document.field.getValue()', () => {
    it('returns the current field value', async () => {
      parentConnection.call = jest.fn(() => Promise.resolve('test'));

      const value = await ui.document.field.getValue();

      expect(parentConnection.call).toHaveBeenCalledWith('getFieldValue');
      expect(value).toEqual('test');
    });
  });

  describe('ui.document.field.getCompareValue()', () => {
    it('returns the previous field value', async () => {
      parentConnection.call = jest.fn(() => Promise.resolve('test'));

      const compareValue = await ui.document.field.getCompareValue();

      expect(parentConnection.call).toHaveBeenCalledWith('getCompareValue');
      expect(compareValue).toEqual('test');
    });
  });

  describe('ui.document.field.setValue()', () => {
    it('sets the current field value', async () => {
      parentConnection.call = jest.fn(() => Promise.resolve());
      await ui.document.field.setValue('test');
      expect(parentConnection.call).toHaveBeenCalledWith('setFieldValue', 'test');
    });
  });

  describe('ui.document.field.setHeight()', () => {
    it('sets the field height', async () => {
      parentConnection.call = jest.fn(() => Promise.resolve());
      await ui.document.field.setHeight(100);
      expect(parentConnection.call).toHaveBeenCalledWith('setFieldHeight', 100);
    });
  });
});
