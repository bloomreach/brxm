/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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

import ComponentElement from './element/componentElement';
import ContainerElement from './element/containerElement';
import HstConstants from '../hst.constants';
import ManageContentLink from './element/manageContentLink';
import MenuLink from './element/menuLink';

class PageStructureService {
  constructor(
    $log,
    $q,
    ChannelService,
    FeedbackService,
    HippoIframeService,
    HstCommentsProcessorService,
    HstComponentService,
    HstService,
    MarkupService,
    PageMetaDataService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.ChannelService = ChannelService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.HstCommentsProcessorService = HstCommentsProcessorService;
    this.HstComponentService = HstComponentService;
    this.HstService = HstService;
    this.MarkupService = MarkupService;
    this.PageMetaDataService = PageMetaDataService;

    this.changeListeners = [];
    this.embeddedLinks = [];
    this.clearParsedElements();
  }

  clearParsedElements() {
    this.embeddedLinks.splice(0)
      .forEach(element => element.getBoxElement().remove());
    this.containers = [];
    this.headContributions = [];
    this.PageMetaDataService.clear();
    this._notifyChangeListeners();
  }

  registerParsedElement(commentDomElement, metaData) {
    const type = metaData[HstConstants.TYPE];
    switch (type) {
      case HstConstants.TYPE_CONTAINER:
        this._registerContainer(commentDomElement, metaData);
        break;
      case HstConstants.TYPE_COMPONENT:
        this._registerComponent(commentDomElement, metaData);
        break;
      case HstConstants.TYPE_PAGE:
        this._registerPageMetaData(metaData);
        break;
      case HstConstants.TYPE_PROCESSED_HEAD_CONTRIBUTIONS:
      case HstConstants.TYPE_UNPROCESSED_HEAD_CONTRIBUTIONS:
        this._registerHeadContributions(metaData);
        break;
      case HstConstants.TYPE_MANAGE_CONTENT_LINK:
        this._registerManageContentLink(commentDomElement, metaData);
        break;
      case HstConstants.TYPE_EDIT_MENU_LINK:
        this._registerMenuLink(commentDomElement, metaData);
        break;
      default:
        this.$log.warn(`Ignoring unknown page structure element '${type}'`);
    }
  }

  _registerContainer(commentDomElement, metaData) {
    const container = new ContainerElement(commentDomElement, metaData, this.HstCommentsProcessorService);
    this.containers.push(container);
  }

  _registerComponent(commentDomElement, metaData) {
    if (this.containers.length === 0) {
      this.$log.warn('Unable to register component outside of a container context.');
      return;
    }

    const container = this.containers[this.containers.length - 1];
    try {
      const component = new ComponentElement(commentDomElement, metaData, container, this.HstCommentsProcessorService);
      container.addComponent(component);
    } catch (exception) {
      this.$log.debug(exception, metaData);
    }
  }

  _registerPageMetaData(metaData) {
    delete metaData[HstConstants.TYPE];
    this.PageMetaDataService.add(metaData);
  }

  _registerHeadContributions(metaData) {
    this.headContributions = this.headContributions.concat(metaData[HstConstants.HEAD_ELEMENTS]);
  }

  _registerManageContentLink(commentDomElement, metaData) {
    this.embeddedLinks.push(new ManageContentLink(commentDomElement, metaData));
  }

  _registerMenuLink(commentDomElement, metaData) {
    this.embeddedLinks.push(new MenuLink(commentDomElement, metaData));
  }

  registerChangeListener(callback) {
    this.changeListeners.push(callback);
  }

  _notifyChangeListeners() {
    this.changeListeners.forEach((callback) => {
      callback();
    });
  }

  // Attaching the embedded links to the page structure (by means of the 'enclosingElement') is only
  // done as a final step of processing an entire page or markup fragment, because it requires an
  // up-to-date and complete page structure (containers, components).
  attachEmbeddedLinks() {
    this.embeddedLinks.forEach(link => this._attachEmbeddedLink(link));
    this._notifyChangeListeners();
  }

  _attachEmbeddedLink(link) {
    let enclosingElement = link.getEnclosingElement();

    if (enclosingElement === undefined) {
      // link is not yet attached, determine enclosing element.
      const commentDomElement = link.getStartComment()[0];
      this.getContainers().some((container) => {
        container.getComponents().some((component) => {
          if (component.containsDomElement(commentDomElement)) {
            enclosingElement = component;
          }
          return enclosingElement;
        });
        if (!enclosingElement && container.containsDomElement(commentDomElement)) {
          enclosingElement = container;
        }
        return enclosingElement;
      });
      if (enclosingElement === undefined) {
        enclosingElement = null; // marks that the *page* is the enclosing element
      }
      link.setEnclosingElement(enclosingElement);

      // insert transparent placeholder into page
      link.prepareBoxElement();
    }
  }

  getEmbeddedLinks() {
    return this.embeddedLinks;
  }

  getComponentById(id) {
    let component = null;
    this.containers.some((container) => {
      component = container.getComponent(id);
      return component;
    });
    return component;
  }

  hasContainer(container) {
    return this.containers.indexOf(container) !== -1;
  }

  getContainers() {
    return this.containers;
  }

  getContainerById(id) {
    return this.containers.find(item => item.getId() === id);
  }

  /**
   * Remove the component identified by given Id
   * @param componentId
   * @return the container of the removed component
   */
  removeComponentById(componentId) {
    const component = this.getComponentById(componentId);

    if (component) {
      const oldContainer = component.getContainer();
      return this.HstComponentService.deleteComponent(oldContainer.getId(), componentId)
        .then(() => {
          this._onAfterRemoveComponent();
          return oldContainer;
        },
        (errorResponse) => {
          const errorKey = errorResponse.error === 'ITEM_ALREADY_LOCKED'
            ? 'ERROR_DELETE_COMPONENT_ITEM_ALREADY_LOCKED'
            : 'ERROR_DELETE_COMPONENT';
          const params = errorResponse.parameterMap;
          params.component = component.getLabel();
          return this._showFeedbackAndReload(errorKey, params);
        });
    }
    this.$log.debug(
      `Could not remove component with ID '${componentId}' because it does not exist in the page structure.`,
    );
    return this.$q.reject();
  }

  _onAfterRemoveComponent() {
    this.ChannelService.recordOwnChange();
  }

  getComponentByOverlayElement(componentOverlayElement) {
    let component;
    this.containers.some((container) => {
      component = container.getComponents().find(c => c.getOverlayElement().is(componentOverlayElement));
      return !!component;
    });
    return component;
  }

  getContainerByIframeElement(containerIFrameElement) {
    return this.containers.find(container => container.getBoxElement().is(containerIFrameElement));
  }

  printParsedElements() {
    this.containers.forEach((container, index) => {
      this.$log.debug(`Container ${index}`, container);
      container.items.forEach((component, itemIndex) => {
        this.$log.debug(`  Component ${itemIndex}`, component);
      });
    });
  }

  /**
   * Renders a component in the current page.
   * @param component     The component
   * @param propertiesMap Optional: the parameter names and values to use for rendering.
   *                      When omitted the persisted names and values are used.
   */
  renderComponent(component, propertiesMap = {}) {
    if (component) {
      return this.MarkupService.fetchComponentMarkup(component, propertiesMap)
        .then((response) => {
          // re-fetch component because a parallel renderComponent call may have updated the component's markup
          component = this.getComponentById(component.getId());

          const newMarkup = response.data;
          const updatedComponent = this._updateComponent(component, newMarkup);

          if ($.isEmptyObject(propertiesMap) && this.containsNewHeadContributions(updatedComponent)) {
            this.$log.info(
              `Updated '${updatedComponent.getLabel()}' component needs additional head contributions, reloading page`,
            );
            this.HippoIframeService.reload();
          }
        })
        .catch((response) => {
          if (response.status === 404) {
            // component being edited is removed (by someone else), reload the page
            this.HippoIframeService.reload();
            this.FeedbackService.showDismissible('FEEDBACK_NOT_FOUND_MESSAGE');
            return this.$q.reject();
          }
          return this.$q.resolve();
        });
    }
    return this.$q.resolve();
  }

  /**
   * Update the component with the new markup
   */
  _updateComponent(oldComponent, newMarkup) {
    const $newMarkup = $(newMarkup);
    const container = oldComponent.getContainer();

    this._removeEmbeddedLinksInComponent(oldComponent);

    oldComponent.replaceDOM($newMarkup, () => {
      this._notifyChangeListeners();
    });

    const newComponent = this._createComponent($newMarkup, container);

    if (newComponent) {
      container.replaceComponent(oldComponent, newComponent);
      // reuse the overlay element to reduce DOM manipulation and improve performance
      newComponent.setOverlayElement(oldComponent.getOverlayElement());
    } else {
      container.removeComponent(oldComponent);
    }
    this.attachEmbeddedLinks();

    return newComponent;
  }

  renderContainer(container) {
    return this.MarkupService.fetchContainerMarkup(container)
      .then(markup => this._updateContainer(container, markup))
      .then((newContainer) => {
        this._notifyChangeListeners();
        return newContainer;
      });
  }

  _updateContainer(oldContainer, newMarkup) {
    this._removeEmbeddedLinksInContainer(oldContainer);
    const $newMarkup = $(newMarkup);

    oldContainer.replaceDOM($newMarkup, () => {
      this._notifyChangeListeners();
    });

    const container = this._createContainer($newMarkup);
    const newContainer = this._replaceContainer(oldContainer, container);

    this.attachEmbeddedLinks();
    return newContainer;
  }

  containsNewHeadContributions(pageStructureElement) {
    if (!pageStructureElement) {
      return false;
    }
    const elementHeadContributions = pageStructureElement.getHeadContributions();
    return elementHeadContributions.some(contribution => !this.headContributions.includes(contribution));
  }

  _removeEmbeddedLinksInContainer(container) {
    container.getComponents().forEach(component => this._removeEmbeddedLinksInComponent(component));

    this.embeddedLinks = this._getLinksNotEnclosedInElement(this.embeddedLinks, container);
  }

  _removeEmbeddedLinksInComponent(component) {
    this.embeddedLinks = this._getLinksNotEnclosedInElement(this.embeddedLinks, component);
  }

  _getLinksNotEnclosedInElement(links, element) {
    const remainingContentLinks = [];
    links.forEach((link) => {
      if (link.getEnclosingElement() !== element) {
        remainingContentLinks.push(link);
      }
    });
    return remainingContentLinks;
  }

  addComponentToContainer(catalogComponent, container) {
    return this.HstService.addHstComponent(catalogComponent, container.getId())
      .then(
        (newComponentJson) => {
          this.ChannelService.recordOwnChange();
          // TODO: handle error when rendering container failed
          return newComponentJson.id;
        },
        (errorResponse) => {
          const errorKey = errorResponse.error === 'ITEM_ALREADY_LOCKED'
            ? 'ERROR_ADD_COMPONENT_ITEM_ALREADY_LOCKED'
            : 'ERROR_ADD_COMPONENT';
          const params = errorResponse.parameterMap;
          params.component = catalogComponent.name;
          return this._showFeedbackAndReload(errorKey, params);
        },
      );
  }

  moveComponent(component, targetContainer, targetContainerNextComponent) {
    // first update the page structure so the component is already 'moved' in the client-side state
    const sourceContainer = component.getContainer();
    sourceContainer.removeComponent(component);
    targetContainer.addComponentBefore(component, targetContainerNextComponent);

    const changedContainers = [sourceContainer];
    if (sourceContainer.getId() !== targetContainer.getId()) {
      changedContainers.push(targetContainer);
    }

    // next, push the updated container representation(s) to the backend
    const backendCallPromises = changedContainers.map(container => this._storeContainer(container));

    // last, record a channel change. The caller is responsible for re-rendering the changed container(s)
    // so their meta-data is updated and we're sure they look right
    return this.$q.all(backendCallPromises)
      .then(() => this.ChannelService.recordOwnChange())
      .then(() => changedContainers)
      .catch(() => this.FeedbackService.showError('ERROR_MOVE_COMPONENT_FAILED', {
        component: component.getLabel(),
      }));
  }

  _storeContainer(container) {
    return this.HstService.updateHstContainer(container.getId(), container.getHstRepresentation());
  }

  renderNewComponentInContainer(newComponentId, container) {
    return this.renderContainer(container)
      .then(() => this.getComponentById(newComponentId))
      .then((newComponent) => {
        if (this.containsNewHeadContributions(newComponent.getContainer())) {
          this.$log.info(
            `New '${newComponent.getLabel()}' component needs additional head contributions, reloading page`,
          );
          this.HippoIframeService.reload();
        }
      });
  }

  getContainerByOverlayElement(overlayElement) {
    return this.containers.find((container) => {
      const containerOverlay = container.getOverlayElement();
      return containerOverlay && (containerOverlay[0] === overlayElement);
    });
  }

  _replaceContainer(oldContainer, newContainer) {
    const index = this.containers.indexOf(oldContainer);
    if (index === -1) {
      this.$log.warn('Cannot find container', oldContainer);
      return null;
    }
    if (newContainer) {
      this.containers[index] = newContainer;
      // reuse the overlay element to reduce DOM manipulation and improve performance
      newContainer.setOverlayElement(oldContainer.getOverlayElement());
    } else {
      this.containers.splice(index, 1);
    }
    return newContainer;
  }

  /**
   * Create a new container with meta-data from the given markup value
   * @param markup
   * @returns {*}
   * @private
   */
  _createContainer(jQueryNodeCollection) {
    let container = null;

    this.HstCommentsProcessorService.processFragment(jQueryNodeCollection, (commentDomElement, metaData) => {
      const type = metaData[HstConstants.TYPE];
      try {
        switch (type) {
          case HstConstants.TYPE_CONTAINER:
            if (!container) {
              container = new ContainerElement(commentDomElement, metaData, this.HstCommentsProcessorService);
            } else {
              this.$log.warn('More than one container in the DOM Element!');
              return;
            }
            break;

          case HstConstants.TYPE_COMPONENT:
            if (!container) {
              this.$log.warn('Unable to register component outside of a container context.');
              return;
            }

            try {
              container.addComponent(new ComponentElement(commentDomElement, metaData,
                container, this.HstCommentsProcessorService));
            } catch (exception) {
              this.$log.debug(exception, metaData);
            }
            break;

          case HstConstants.TYPE_UNPROCESSED_HEAD_CONTRIBUTIONS:
            if (container) {
              const unprocessedElements = metaData[HstConstants.HEAD_ELEMENTS];
              container.setHeadContributions(unprocessedElements);
            }
            break;

          case HstConstants.TYPE_MANAGE_CONTENT_LINK:
            this._registerManageContentLink(commentDomElement, metaData);
            break;

          case HstConstants.TYPE_EDIT_MENU_LINK:
            this._registerMenuLink(commentDomElement, metaData);
            break;

          default:
            this.$log.warn(`Ignoring unknown page structure element '${type}'`);
            break;
        }
      } catch (exception) {
        this.$log.debug(exception, metaData);
      }
    });

    if (!container) {
      this.$log.error('Failed to create a new container');
    }

    return container;
  }

  /**
   * Create a new component with meta-data from the given markup value
   */
  _createComponent(jQueryNodeCollection, container) {
    let component = null;

    this.HstCommentsProcessorService.processFragment(jQueryNodeCollection, (commentDomElement, metaData) => {
      const type = metaData[HstConstants.TYPE];
      switch (type) {
        case HstConstants.TYPE_COMPONENT:
          try {
            component = new ComponentElement(commentDomElement, metaData, container, this.HstCommentsProcessorService);
          } catch (exception) {
            this.$log.debug(exception, metaData);
          }
          break;

        case HstConstants.TYPE_UNPROCESSED_HEAD_CONTRIBUTIONS:
          if (component) {
            const unprocessedElements = metaData[HstConstants.HEAD_ELEMENTS];
            component.setHeadContributions(unprocessedElements);
          }
          break;

        case HstConstants.TYPE_MANAGE_CONTENT_LINK:
          this._registerManageContentLink(commentDomElement, metaData);
          break;

        case HstConstants.TYPE_EDIT_MENU_LINK:
          this._registerMenuLink(commentDomElement, metaData);
          break;

        default:
          this.$log.warn(`Ignoring unknown page structure element '${type}'`);
          break;
      }
    });

    if (!component) {
      this.$log.error('Failed to create a new component');
    }

    return component;
  }

  _showFeedbackAndReload(errorKey, params) {
    this.FeedbackService.showError(errorKey, params);
    return this.HippoIframeService.reload().then(() => this.$q.reject());
  }
}

export default PageStructureService;
