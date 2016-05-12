/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
import { WidgetTypes } from './widget.types';

export class ChannelEditCtrl {
  constructor($log, $element, $translate, FeedbackService, ChannelService, HippoIframeService) {
    'ngInject';
    this.$log = $log;
    this.ChannelService = ChannelService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;

    this.feedbackParent = $element.find('.feedback-parent');

    this.pageTitle = $translate.instant('SUBPAGE_CHANNEL_EDIT_TITLE', {
      channelName: ChannelService.getName(),
    });

    this.channelInfoDescription = {};
    ChannelService.getChannelInfoDescription()
      .then((channelInfoDescription) => {
        this.channelInfoDescription = channelInfoDescription;
      })
      .catch(() => {
        this.onError({ key: 'ERROR_CHANNEL_INFO_RETRIEVAL_FAILED' });
      });

    this.values = ChannelService.getChannel().properties;
  }

  save() {
    this.ChannelService.saveProperties(this.values)
      .then(() => {
        this.HippoIframeService.reload();
        this.ChannelService.recordOwnChange();
        this.onSuccess({ key: 'CHANNEL_PROPERTIES_SAVE_SUCCESS' });
      })
      .catch(() => {
        this._showError('ERROR_CHANNEL_PROPERTIES_SAVE_FAILED');
      });
  }

  getLabel(fieldName) {
    const localizedLabel = this.channelInfoDescription.i18nResources[fieldName];
    return localizedLabel || this._getPropertyDefinition(fieldName).displayName || fieldName;
  }

  getFieldGroups() {
    return this.channelInfoDescription.fieldGroups;
  }

  getType(fieldName) {
    const fieldAnnotation = this._getFirstFieldAnnotation(fieldName);
    if (fieldAnnotation) {
      const widgetType = WidgetTypes[fieldAnnotation.type];
      if (widgetType) {
        return widgetType;
      }
    }

    const propertyDefinition = this._getPropertyDefinition(fieldName);
    if (propertyDefinition.valueType === 'BOOLEAN') {
      return WidgetTypes.CheckBox;
    }

    // default widget
    return WidgetTypes.InputBox;
  }

  _getPropertyDefinition(fieldName) {
    return this.channelInfoDescription.propertyDefinitions[fieldName];
  }

  _getFirstFieldAnnotation(fieldName) {
    const propertyDefinition = this._getPropertyDefinition(fieldName);
    if (!propertyDefinition) {
      this.$log.warn('Property definition for the field "{}" not found. Please check your ChannelInfo class', fieldName);
      return undefined;
    }

    const fieldAnnotations = propertyDefinition.annotations;
    if (!fieldAnnotations || fieldAnnotations.length === 0) {
      return undefined;
    }
    if (fieldAnnotations.length > 1) {
      this.$log.warn('Field "{}" contains multiple annotations, that is incorrect. Please check your ChannelInfo class', fieldName);
    }
    return fieldAnnotations[0];
  }

  getDropDownListValues(fieldName) {
    const fieldAnnotation = this._getFirstFieldAnnotation(fieldName);
    if (fieldAnnotation.type !== 'DropDownList') {
      this.$log.debug('Field "{}" is not a dropdown.', fieldName);
      return [];
    }
    return fieldAnnotation.value;
  }

  _showError(key, params) {
    this.FeedbackService.showError(key, params, this.feedbackParent);
  }

  back() {
    this.onDone();
  }
}
