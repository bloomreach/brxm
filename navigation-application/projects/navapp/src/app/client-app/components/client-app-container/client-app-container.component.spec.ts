import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';

import { ClientApplicationHandler } from '../../models';
import { ClientAppService } from '../../services';

import { ClientAppContainerComponent } from './client-app-container.component';

describe('IframesContainerComponent', () => {
  let component: ClientAppContainerComponent;
  let fixture: ComponentFixture<ClientAppContainerComponent>;

  const fakeApplicationsCreated$ = new Subject<ClientApplicationHandler>();

  let clientAppService: ClientAppService;

  let el: HTMLElement;

  beforeEach(() => {
    const clientAppServiceMock = {
      applicationCreated$: fakeApplicationsCreated$,
    } as any;

    fixture = TestBed.configureTestingModule({
      imports: [],
      declarations: [ClientAppContainerComponent],
      providers: [
        { provide: ClientAppService, useValue: clientAppServiceMock },
      ],
    }).createComponent(ClientAppContainerComponent);

    component = fixture.componentInstance;
    el = fixture.elementRef.nativeElement;

    clientAppService = TestBed.get(ClientAppService);
  });

  beforeEach(() => {
    component.ngOnInit();
  });

  it('should be empty at the beginning', () => {
    const iframes = el.querySelectorAll('iframe');

    expect(iframes.length).toBe(0);
  });

  it('should add an iframe when client apps manager notifies about that', () => {
    const fakeApp = new ClientApplicationHandler(
      'some/url',
      document.createElement('iframe'),
    );

    fakeApplicationsCreated$.next(fakeApp);

    fixture.detectChanges();

    const iframes = el.querySelectorAll('iframe');

    expect(iframes.length).toBe(1);
  });
});
