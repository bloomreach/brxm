/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

import { Typed } from 'emittery';
import { ContainerModule } from 'inversify';
import { ComponentModel, PageModel } from './page';

export const EventBusService = Symbol.for('EventBusService');

export type EventBus = Typed<Events>;

export interface Events {
  'cms.update': CmsUpdateEvent;
  'page.update': PageUpdateEvent;
  'page.ready': PageReadyEvent;
}

/**
 * Channel Manager component update event.
 */
export interface CmsUpdateEvent {
  /**
   * Component's id.
   */
  id: ComponentModel['id'];

  /**
   * Updated component's properties.
   */
  properties: object;
}

/**
 * Page model update event.
 */
export interface PageUpdateEvent {
  /**
   * Updated part of the page model.
   */
  page: PageModel;
}

/**
 * SPA page rendered event.
 */
export interface PageReadyEvent {}

export function EventsModule() {
  return new ContainerModule((bind) => {
    bind(EventBusService).toConstantValue(new Typed<Events>());
  });
}
