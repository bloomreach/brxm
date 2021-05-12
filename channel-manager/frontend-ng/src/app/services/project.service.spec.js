/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
  let ConfigService;
  let ProjectService;
  let FeedbackService;

  const mountId = '12';
  const projects = [
    {
      id: 'master',
    },
    {
      id: 'test1',
      name: 'test1',
    },
    {
      id: 'test2',
      name: 'test2',
    },
  ];
  const currentProject = projects[1];

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
      _ConfigService_,
      _HstService_,
      _ProjectService_,
      _FeedbackService_,
    ) => {
      $httpBackend = _$httpBackend_;
      ConfigService = _ConfigService_;
      ProjectService = _ProjectService_;
      FeedbackService = _FeedbackService_;
    });

    spyOn(ConfigService, 'getCmsContextPath').and.returnValue('/test/');

    $httpBackend.expectGET(`/test/ws/projects/${mountId}/associated-with-channel`).respond(200, projects);

    $httpBackend.expectPUT(`/test/ws/projects/activeProject/${currentProject.id}`).respond(200, currentProject.id);

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

  it('selects the core if the selectedProject is master', () => {
    $httpBackend.expectDELETE('/test/ws/projects/activeProject').respond(200, currentProject.id);
    ProjectService.updateSelectedProject('master');
    $httpBackend.flush();
  });

  it('calls setproject if the selectedProject is a project', () => {
    $httpBackend.expectPUT(`/test/ws/projects/activeProject/${projects[2].id}`).respond(200, currentProject.id);
    ProjectService.updateSelectedProject(projects[2].id);
    $httpBackend.flush();
  });

  it('accepts a channel', () => {
    const channelId = 'testChannel';
    const acceptedProject = { ...currentProject, id: 'acceptedProject',};

    $httpBackend
      .expectPOST(`/test/ws/projects/${currentProject.id}/channel/approve/${channelId}`)
      .respond(200, acceptedProject);

    ProjectService.accept(channelId);
    $httpBackend.flush();
    expect(ProjectService.selectedProject).toEqual(acceptedProject);
  });

  it('rejects a channel while providing a message', () => {
    const channelId = 'testChannel';
    const message = 'testMessage';
    const rejectedProject = { ...currentProject, id: 'rejectedProject',};

    $httpBackend
      .expectPOST(`/test/ws/projects/${currentProject.id}/channel/reject/${channelId}`)
      .respond(200, rejectedProject);

    ProjectService.reject(channelId, message);
    $httpBackend.flush();
    expect(ProjectService.selectedProject).toEqual(rejectedProject);
  });

  it('updates channel when reject call fails', () => {
    spyOn(FeedbackService, 'showError');
    const channelId = 'testChannel';
    const message = 'testMessage';

    $httpBackend
      .expectPOST(`/test/ws/projects/${currentProject.id}/channel/reject/${channelId}`)
      .respond(500);

    ProjectService.reject(channelId, message);
    $httpBackend.flush();

    expect(FeedbackService.showError).toHaveBeenCalled();
  });
});
