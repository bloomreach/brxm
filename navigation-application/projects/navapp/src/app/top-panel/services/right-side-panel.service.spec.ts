/*
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

import { MatSidenav } from '@angular/material';

import { RightSidePanelService } from './right-side-panel.service';

describe('RightSidePanelService', () => {
  let service: RightSidePanelService;
  let sidenavMock: MatSidenav;

  beforeEach(() => {
    sidenavMock = jasmine.createSpyObj(['open', 'close']);

    service = new RightSidePanelService();
    service.setSidenav(sidenavMock);
  });

  it('should call open method', () => {
    service.open();

    expect(sidenavMock.open).toHaveBeenCalled();
  });

  it('should call close method', () => {
    service.close();

    expect(sidenavMock.close).toHaveBeenCalled();
  });
});
