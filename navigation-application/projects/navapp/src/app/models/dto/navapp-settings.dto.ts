import { NavConfigResource } from './nav-config-resource.dto';

export interface UserSettings {
  userName: string;
  email?: string;
  language: string;
  timeZone: string;
}

export interface NavAppSettings {

  userSettings: UserSettings;

  appSettings: {
    navConfigResources: NavConfigResource[];
  };
}
