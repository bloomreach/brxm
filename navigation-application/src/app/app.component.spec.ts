import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { ClientApplicationsManagerModule } from './client-applications-manager';
import { CommunicationModule } from './communication';
import { LeftMenuModule } from './left-menu';
import { NavigationConfigurationService } from './services';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;

  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        AppRoutingModule,
        LeftMenuModule,
        ClientApplicationsManagerModule,
        CommunicationModule,
      ],
      declarations: [AppComponent],
      providers: [NavigationConfigurationService],
    }).createComponent(AppComponent);

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
  });

  it('should create the app', () => {
    expect(component).toBeDefined();
  });
});
