/*!
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Subject } from 'rxjs';

import { NavigationStartEvent } from '../deep-linking/events/navigation-start.event';
import { NavigationStopEvent } from '../deep-linking/events/navigation-stop.event';

import { BusyIndicatorService } from './busy-indicator.service';

describe('BusyIndicatorService', () => {
  let service: BusyIndicatorService;

  const events$ = new Subject();
  const deepLinkingServiceMock = {
    events$,
  } as any;

  beforeEach(() => {
    service = new BusyIndicatorService(deepLinkingServiceMock);
  });

  it('busy indicator should be hidden by default', () => {
    expect(service.isVisible).toBeFalsy();
  });

  it('should show the busy indicator', () => {
    service.show();

    expect(service.isVisible).toBeTruthy();
  });

  it('should hide the busy indicator', () => {
    service.hide();

    expect(service.isVisible).toBeFalsy();
  });

  it('should show the busy indicator when NavigationStartEvent event is emitted', () => {
    spyOn(service, 'show');

    events$.next(new NavigationStartEvent());

    expect(service.show).toHaveBeenCalled();
  });

  it('should hide the busy indicator when NavigationStopEvent event is emitted', () => {
    spyOn(service, 'hide');

    events$.next(new NavigationStopEvent());

    expect(service.hide).toHaveBeenCalled();
  });
});
