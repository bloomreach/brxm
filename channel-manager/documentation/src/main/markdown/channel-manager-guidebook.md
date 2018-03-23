# Channel Manager Guidebook

Guide for the Channel Manager codebase.

## Context

![Context Diagram](../plantuml/context-diagram.svg)

## Functional Overview

The main parts of the Channel Manager are depicted below, with arrows indicating how parts use each other.
Each part also lists the framework(s) it uses.

![Container Diagram](../plantuml/container-diagram.svg)

## Channel Editor

The Channel Editor manages and modifies a single channel. Such a channel is rendered in an iframe, where 
server-side proxy configuration ensures that the origin of the rendered channel is the same as the 
origin of the surrounding CMS. Frontend code can therefore read and modify the DOM of the 
rendered channel without cross-origin restrictions.    

The delivery tier augments the rendered channel with additional HTML comments. These comments 
contain meta-data about elements in the rendered channel (e.g. "here starts a container"). 
Once a page is loaded these HTML comments are parsed and turned into a model of the current 
page structure. Based on this model an 'overlay' is generated in the DOM of the channel. 
The overlay elements depict containers, components, and floating action buttons to edit menus 
and manage content.

The presentation of a channel is modified via a REST API in the delivery tier. The code of this 
REST API resides in the `hippo-site-toolkit` Git repository.

The content of a channel is modified via a REST API that resides in the `content-service` module 
of this Git repository.

### Hippo CMS

Most of the Channel Editor functionality is available in Hippo CMS (also known as the 'open source' 
or 'community' edition).

#### Preview content and presentation 

- Browse through the preview version of a channel. External links are opened in a new tab.
- Adjust the viewport dimensions (desktop / tablet / phone) 

#### Channel settings

- Edit channel settings

#### Pages

- View all pages
- Navigate to a page
- Add page
- Delete page
- Move page
- Copy page
- Edit page 

#### Components

- Add components to containers
- Delete components
- Move components within and between containers
- Edit component properties

#### Menus

- Add menu item
- Move menu item
- Edit menu item parameters
- Delete menu item

#### Content

- Create new content
- Edit existing content
- Select existing content for a component

#### Change Management

- Publish own changes
- Discard own changes
- Manage changes of others (admin)
 
#### Multi-User

- Show locks by other users
- Prevent editing of locked items
- Provide feedback who locked what

### DXP

Several modules in the DXP (or 'enterprise edition') extend the Channel Editor with 
additional functionality.

#### Projects

- Show project-specific version of a channel

#### Relevance

- Add component variant
- Edit component variant
- Delete component variant
- Preview component variant in site
- Show page as persona
- Show page as alter ego (and edit alter ego)

#### Experiments

- Create experiment
- Monitor experiment progress / status
- Stop/complete experiment
- Show experiment status on component
