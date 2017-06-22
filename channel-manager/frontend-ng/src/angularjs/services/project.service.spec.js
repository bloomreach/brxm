/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

describe('ProjectService', () => {
  let $httpBackend;
  let $q;
  let ConfigService;
  let HstService;
  let ProjectService;

  const mountId = '12';
  const projects = [
    {
      id: 'test1',
      name: 'test1',
    },
    {
      id: 'test2',
      name: 'test2',
    },
  ];
  const currentProject = projects[0];

  const channels = [
    {
      mountId: 'mountId1',
      id: 'channelId1',
    },
    {
      mountId: 'mountId2',
      id: 'channelId2',
    },
  ];

  beforeEach(() => {
    angular.mock.module('hippo-cm', ($provide) => {
      $provide.decorator('$window', ($delegate) => {
        Object.assign($delegate.parent, {
          Hippo: {
            Projects: {
              events: jasmine.createSpyObj('events', ['subscribe', 'unsubscribeAll']),
            },
          },
        });

        return $delegate;
      });
    });

    inject((
      _$httpBackend_,
      _$q_,
      _ConfigService_,
      _HstService_,
      _ProjectService_,
    ) => {
      $httpBackend = _$httpBackend_;
      $q = _$q_;
      ConfigService = _ConfigService_;
      HstService = _HstService_;
      ProjectService = _ProjectService_;
    });

    spyOn(ConfigService, 'getCmsContextPath').and.returnValue('/test/');
    spyOn(HstService, 'doPut');
    spyOn(HstService, 'doGet');

    $httpBackend.expectGET(`/test/ws/projects/${mountId}/associated-with-channel`).respond(200, projects);
    $httpBackend.expectGET('/test/ws/channels/').respond(200, channels);

    HstService.doGet.and.returnValue($q.resolve({ data: currentProject.id }));
    HstService.doPut.and.returnValue($q.resolve());

    ProjectService.load(mountId, currentProject.id);
    $httpBackend.flush();
  });

  afterEach(() => {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  it('loads projects and sets the current project', () => {
    expect(ProjectService.projects).toEqual(projects);
    expect(ProjectService.selectedProject).toEqual(currentProject);
  });

  it('selects the core if the selectedProject is not a project', () => {
    HstService.doPut.calls.reset();
    ProjectService.updateSelectedProject('something');

    expect(HstService.doPut).toHaveBeenCalledWith(null, mountId, 'selectmaster');
  });

  it('calls setproject if the selectedProject is a project', () => {
    HstService.doPut.calls.reset();
    ProjectService.updateSelectedProject(projects[1].id);

    expect(HstService.doPut).toHaveBeenCalledWith(null, mountId, 'selectbranch', projects[1].id);
  });
});
