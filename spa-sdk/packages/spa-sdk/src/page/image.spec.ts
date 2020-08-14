/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import { ImageImpl, ImageModel, Image } from './image';
import { LinkFactory } from './link-factory';
import { TYPE_LINK_RESOURCE } from './link';

let linkFactory: jest.Mocked<LinkFactory>;

const model = {
  displayName: 'something',
  height: 250,
  links: {
    site: {
      href: 'url',
      type: 'resource',
    },
  },
  mimeType: 'image/jpeg',
  name: 'hippogallery:original',
  size: 163412,
  width: 1170,
} as ImageModel;

function createImage(imageModel = model) {
  return new ImageImpl(imageModel, linkFactory);
}

beforeEach(() => {
  linkFactory = { create: jest.fn() } as unknown as typeof linkFactory;
});

describe('ImageImpl', () => {
  let image: Image;

  beforeEach(() => {
    image = createImage();
  });

  describe('getDisplayName', () => {
    it('should return a display name', () => {
      expect(image.getDisplayName()).toBe('something');
    });
  });

  describe('getFileName', () => {
    it('should return undefined when there is no file name', () => {
      expect(image.getFileName()).toBeUndefined();
    });

    it('should return an image file name', () => {
      const image = createImage({ ...model, fileName: 'something.jpg' });

      expect(image.getFileName()).toBe('something.jpg');
    });
  });

  describe('getHeight', () => {
    it('should return an image height', () => {
      expect(image.getHeight()).toBe(250);
    });
  });

  describe('getMimeType', () => {
    it('should return an image mime-type', () => {
      expect(image.getMimeType()).toBe('image/jpeg');
    });
  });

  describe('getName', () => {
    it('should return an image name', () => {
      expect(image.getName()).toBe('hippogallery:original');
    });
  });

  describe('getSize', () => {
    it('should return an image size', () => {
      expect(image.getSize()).toBe(163412);
    });
  });

  describe('getUrl', () => {
    it('should return an image URL', () => {
      linkFactory.create.mockReturnValueOnce('url');

      expect(image.getUrl()).toBe('url');
      expect(linkFactory.create).toBeCalledWith({ href: 'url', type: TYPE_LINK_RESOURCE });
    });
  });

  describe('getWidth', () => {
    it('should return an image width', () => {
      expect(image.getWidth()).toBe(1170);
    });
  });
});
