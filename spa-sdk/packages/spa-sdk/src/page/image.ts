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
import { Builder } from './factory';
import { LinkFactory } from './link-factory';
import { Link } from './link';

export const ImageFactory = Symbol.for('ImageFactory');
export const ImageModelToken = Symbol.for('ImageModelToken');

export type ImageFactory = Builder<[ImageModel], Image>;

/**
 * @hidden
 */
type ImageLinks = 'site';

/**
 * Image model.
 * @hidden
 */
export interface ImageModel {
  displayName: string;
  fileName: string | null;
  height: number;
  links: Partial<Record<ImageLinks, Link>>;
  mimeType: string;
  name: string;
  size: number;
  width: number;
}

export interface Image {
  /**
   * @return The image display name.
   */
  getDisplayName(): string;

  /**
   * @return The image file name.
   */
  getFileName(): string | undefined;

  /**
   * @return The image height.
   */
  getHeight(): number;

  /**
   * @return The image mime-type.
   */
  getMimeType(): string;

  /**
   * @return The image name.
   */
  getName(): string;

  /**
   * @return The image size.
   */
  getSize(): number;

  /**
   * @return The image link.
   */
  getUrl(): string | undefined;

  /**
   * @return The image width.
   */
  getWidth(): number;
}

@injectable()
export class ImageImpl implements Image {
  constructor(
    @inject(ImageModelToken) protected model: ImageModel,
    @inject(LinkFactory) private linkFactory: LinkFactory,
  ) {}

  getDisplayName(): string {
    return this.model.displayName;
  }

  getFileName(): string | undefined {
    return this.model.fileName ?? undefined;
  }

  getHeight(): number {
    return this.model.height;
  }

  getMimeType(): string {
    return this.model.mimeType;
  }

  getName(): string {
    return this.model.name;
  }

  getSize(): number {
    return this.model.size;
  }

  getUrl(): string | undefined {
    return this.model.links.site && this.linkFactory.create(this.model.links.site);
  }

  getWidth(): number {
    return this.model.width;
  }
}
