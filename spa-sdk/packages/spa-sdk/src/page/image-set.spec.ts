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

import { ImageSetImpl, ImageSetModel, ImageSet, TYPE_IMAGE_SET, isImageSet } from './image-set';
import { ImageFactory, ImageModel, Image } from './image';

let imageFactory: jest.MockedFunction<ImageFactory>;

const model = {
  data: {
    description: null,
    displayName: 'Banner',
    fileName: 'something.jpg',
    id: 'some-id',
    localeString: null,
    name: 'something',
    original: null,
    thumbnail: null,
  },
  type: TYPE_IMAGE_SET,
} as ImageSetModel;

function createImageSet(imageSetModel = model) {
  return new ImageSetImpl(imageSetModel, imageFactory);
}

beforeEach(() => {
  imageFactory = jest.fn();
});

describe('ImageSetImpl', () => {
  let imageSet: ImageSet;

  beforeEach(() => {
    imageSet = createImageSet();
  });

  describe('getDescription', () => {
    it('should return undefined when there is no description', () => {
      expect(imageSet.getDescription()).toBeUndefined();
    });

    it('should return an image set file name', () => {
      const image = createImageSet({ ...model, data: { ...model.data, description: 'something' } });

      expect(image.getDescription()).toBe('something');
    });
  });

  describe('getDisplayName', () => {
    it('should return a display name', () => {
      expect(imageSet.getDisplayName()).toBe('Banner');
    });
  });

  describe('getFileName', () => {
    it('should return an image set file name', () => {
      expect(imageSet.getFileName()).toBe('something.jpg');
    });
  });

  describe('getId', () => {
    it('should return an image set id', () => {
      expect(imageSet.getId()).toBe('some-id');
    });
  });

  describe('getName', () => {
    it('should return an image set name', () => {
      expect(imageSet.getName()).toBe('something');
    });
  });

  describe('getLocale', () => {
    it('should return an image set locale', () => {
      const imageSet = createImageSet({ ...model, data: { ...model.data, localeString: 'some-locale' } });

      expect(imageSet.getLocale()).toBe('some-locale');
    });

    it('should return undefined when there is no locale', () => {
      expect(imageSet.getLocale()).toBeUndefined();
    });
  });

  describe('getOriginal', () => {
    it('should return undefined when there is no original image', () => {
      const imageSet = createImageSet();

      expect(imageSet.getOriginal()).toBeUndefined();
    });

    it('should return an original image', () => {
      const imageModel = {} as ImageModel;
      const image = {} as Image;
      imageFactory.mockReturnValueOnce(image);

      const menu = createImageSet({ ...model, data: { ...model.data, original: imageModel } });

      expect(menu.getOriginal()).toBe(image);
    });
  });

  describe('getThumbnail', () => {
    it('should return undefined when there is no thumbnail', () => {
      const imageSet = createImageSet();

      expect(imageSet.getThumbnail()).toBeUndefined();
    });

    it('should return a thumbnail', () => {
      const imageModel = {} as ImageModel;
      const image = {} as Image;
      imageFactory.mockReturnValueOnce(image);

      const menu = createImageSet({ ...model, data: { ...model.data, thumbnail: imageModel } });

      expect(menu.getThumbnail()).toBe(image);
    });
  });
});

describe('isImageSet', () => {
  it('should return true', () => {
    const imageSet = createImageSet();

    expect(isImageSet(imageSet)).toBe(true);
  });

  it('should return false', () => {
    expect(isImageSet(undefined)).toBe(false);
    expect(isImageSet({})).toBe(false);
  });
});
