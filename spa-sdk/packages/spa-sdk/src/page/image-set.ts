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

import { injectable, inject } from 'inversify';
import { ImageFactory, ImageModel, Image } from './image';

export const ImageSetModelToken = Symbol.for('ImageSetModelToken');

export const TYPE_IMAGE_SET = 'imageset';

/**
 * @hidden
 */
interface ImageSetDataModel {
  description: string | null;
  displayName: string;
  fileName: string;
  id: string;
  localeString: string | null;
  name: string;
  original: ImageModel | null;
  thumbnail: ImageModel | null;
}

/**
 * Image set model.
 * @hidden
 */
export interface ImageSetModel {
  data: ImageSetDataModel;
  type: typeof TYPE_IMAGE_SET;
}

export interface ImageSet {
  /**
   * @return The image set description.
   */
  getDescription(): string | undefined;

  /**
   * @return The image set display name.
   */
  getDisplayName(): string;

  /**
   * @return The image set file name.
   */
  getFileName(): string;

  /**
   * @return The image set id.
   */
  getId(): string;

  /**
   * @return The image set locale.
   */
  getLocale(): string | undefined;

  /**
   * @return The image name.
   */
  getName(): string;

  /**
   * @return The original image.
   */
  getOriginal(): Image | undefined;

  /**
   * @return The thumbnail.
   */
  getThumbnail(): Image | undefined;
}

@injectable()
export class ImageSetImpl implements ImageSet {
  private original?: Image;

  private thumbnail?: Image;

  constructor(
    @inject(ImageSetModelToken) protected model: ImageSetModel,
    @inject(ImageFactory) imageFactory: ImageFactory,
  ) {
    this.original = model.data.original
      ? imageFactory(model.data.original)
      : undefined;

    this.thumbnail = model.data.thumbnail
      ? imageFactory(model.data.thumbnail)
      : undefined;
  }

  getDescription(): string | undefined {
    return this.model.data.description ?? undefined;
  }

  getDisplayName(): string {
    return this.model.data.displayName;
  }

  getFileName(): string {
    return this.model.data.fileName ?? undefined;
  }

  getId(): string {
    return this.model.data.id;
  }

  getLocale(): string | undefined {
    return this.model.data.localeString ?? undefined;
  }

  getName(): string {
    return this.model.data.name;
  }

  getOriginal(): Image | undefined {
    return this.original;
  }

  getThumbnail(): Image | undefined {
    return this.thumbnail;
  }
}

/**
 * Checks whether a value is an image set.
 * @param value The value to check.
 */
export function isImageSet(value: unknown): value is ImageSet {
  return value instanceof ImageSetImpl;
}
