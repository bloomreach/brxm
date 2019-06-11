import { ConfigResource } from './nav-config-resource.dto';

export interface UserSettings {
  userName: string;
  language: string;
  timeZone: string;
}

export interface AppSettings {
  navConfigResources: ConfigResource[];
  sitesResource?: ConfigResource;
}

export interface NavAppSettings {
  userSettings: UserSettings;
  appSettings: AppSettings;
}
