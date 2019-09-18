/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
 * Model of a content item.
 */
export interface ContentModel {
  id: string;
  localeString?: string;
  name: string;
  [property: string]: any;
}

/**
 * Content used on the page.
 */
export interface Content {
  /**
   * Returns the content id.
   */
  getId(): string;

  /**
   * Returns the content locale.
   */
  getLocale(): string | undefined;

  /**
   * Returns the content name.
   */
  getName(): string;

  /**
   * Returns the content data as it is returned in the Page Model API.
   */
  getData(): ContentModel;
}

export class Content {
  constructor(protected model: ContentModel) {}

  getId() {
    return this.model.id;
  }

  getLocale() {
    return this.model.localeString;
  }

  getName() {
    return this.model.name;
  }

  getData() {
    return this.model;
  }
}
