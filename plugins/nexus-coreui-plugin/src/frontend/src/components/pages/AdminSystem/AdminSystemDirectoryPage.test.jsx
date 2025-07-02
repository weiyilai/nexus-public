/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import givenUserLoggedIn from '../../../testUtils/givenUserLoggedIn';
import givenBundleActiveStates from '../../../testUtils/givenBundleActiveStates';
import givenExtJSState from '../../../testUtils/givenExtJSState';
import givenPermissions from '../../../testUtils/givenPermissions';
import { Permissions } from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../constants/UIStrings';
import { runLinkNotVisibleTest, runLinkVisiblityTest } from '../../../testUtils/directoryPageTestUtils';
import { ROUTE_NAMES } from '../../../routerConfig/routeNames/routeNames';

describe('AdminSystemDirectoryPage', () => {
  beforeEach(() => {
    givenUserLoggedIn();

    givenBundleActiveStates({
      'nexus-coreui-plugin': true
    });

    givenExtJSState(defaultExtState());
  });

  describe('API Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.SETTINGS.READ]: true });
      await runLinkVisiblityTestForSystemPage(UIStrings.API.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({ [Permissions.SETTINGS.READ]: false });
      await runLinkNotVisibleTestForSystemPage(UIStrings.API.MENU);
    });
  });

  describe('Capabilities Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ ['nexus:capabilities:read']: true});
      await runLinkVisiblityTestForSystemPage(UIStrings.CAPABILITIES.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({ ['nexus:capabilities:read']: false });
      await runLinkNotVisibleTestForSystemPage(UIStrings.CAPABILITIES.MENU);
    });
  });

  describe('Email Server Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.SETTINGS.READ]: true });
      await runLinkVisiblityTestForSystemPage(UIStrings.EMAIL_SERVER.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({ [Permissions.SETTINGS.READ]: false });
      await runLinkNotVisibleTestForSystemPage(UIStrings.EMAIL_SERVER.MENU);
    });
  });

  describe('HTTP Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.SETTINGS.READ]: true });
      await runLinkVisiblityTestForSystemPage(UIStrings.HTTP.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({ [Permissions.SETTINGS.READ]: false });
      await runLinkNotVisibleTestForSystemPage(UIStrings.HTTP.MENU);
    });
  });

  describe('Licensing Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.LICENSING.READ]: true });
      await runLinkVisiblityTestForSystemPage(UIStrings.LICENSING.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({ [Permissions.LICENSING.READ]: false });
      await runLinkNotVisibleTestForSystemPage(UIStrings.LICENSING.MENU);
    });
  });

  describe('Nodes Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.ADMIN]: true });
      await runLinkVisiblityTestForSystemPage(UIStrings.NODES.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({ [Permissions.ADMIN]: false });
      await runLinkNotVisibleTestForSystemPage(UIStrings.NODES.MENU);
    });
  });

  describe('Tasks Link', () => {
    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.TASKS.READ]: true });
      await runLinkVisiblityTestForSystemPage(UIStrings.TASKS.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({ [Permissions.TASKS.READ]: false });
      await runLinkNotVisibleTestForSystemPage(UIStrings.TASKS.MENU);
    });
  });

  describe('Upgrade Link', () => {
    beforeEach(() => {
      givenExtJSState({
        ...defaultExtState(),
        capabilityActiveTypes: ['migration'],
        capabilityCreatedTypes: ['migration']
      })
    });

    it('shows given permissions', async () => {
      givenPermissions({ [Permissions.MIGRATION.READ]: true });
      await runLinkVisiblityTestForSystemPage(UIStrings.UPGRADE.MENU);
    });

    it('does not show without permissions', async () => {
      givenPermissions({ [Permissions.MIGRATION.READ]: false });
      await runLinkNotVisibleTestForSystemPage(UIStrings.UPGRADE.MENU);
    });
  });

  function defaultExtState() {
    return { usertoken: { licenseValid: true } };
  }

  async function runLinkVisiblityTestForSystemPage(menu) {
    await runLinkVisiblityTest(ROUTE_NAMES.ADMIN.SYSTEM.DIRECTORY, 'System', menu);
  }

  async function runLinkNotVisibleTestForSystemPage(menu) {
    await runLinkNotVisibleTest(ROUTE_NAMES.ADMIN.SYSTEM.DIRECTORY, 'System', menu);
  }
});
