import { NavConfigResource } from './nav-config-resource.dto';

export interface NavAppSettings {
  userSettings: {
    userName: string;
    language: string;
    timeZone: string;
  };

  appSettings: {
    navConfigResources: NavConfigResource[];
  };
}
