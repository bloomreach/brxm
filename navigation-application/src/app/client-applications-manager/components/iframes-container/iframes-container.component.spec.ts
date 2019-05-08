import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';

import { ClientApplicationHandler } from '../../models';
import { ClientApplicationsManagerService } from '../../services';

import { IframesContainerComponent } from './iframes-container.component';

describe('IframesContainerComponent', () => {
  let component: IframesContainerComponent;
  let fixture: ComponentFixture<IframesContainerComponent>;

  const fakeApplicationsCreated$ = new Subject<ClientApplicationHandler>();

  let clientApplicationsManagerService: ClientApplicationsManagerService;

  let el: HTMLElement;

  beforeEach(() => {
    const clientApplicationsManagerServiceMock = {
      applicationCreated$: fakeApplicationsCreated$,
    } as any;

    fixture = TestBed.configureTestingModule({
      imports: [
      ],
      declarations: [
        IframesContainerComponent,
      ],
      providers: [
        { provide: ClientApplicationsManagerService, useValue: clientApplicationsManagerServiceMock },
      ],
    }).createComponent(IframesContainerComponent);

    component = fixture.componentInstance;
    el = fixture.elementRef.nativeElement;

    clientApplicationsManagerService = TestBed.get(ClientApplicationsManagerService);
  });

  beforeEach(() => {
    component.ngOnInit();
  });

  it('should be empty at the beginning', () => {
    const iframes = el.querySelectorAll('iframe');

    expect(iframes.length).toBe(0);
  });

  it('should add an iframe when client apps manager notifies about that', () => {
    const fakeApp = new ClientApplicationHandler('some/url', document.createElement('iframe'));

    fakeApplicationsCreated$.next(fakeApp);

    fixture.detectChanges();

    const iframes = el.querySelectorAll('iframe');

    expect(iframes.length).toBe(1);
  });
});
