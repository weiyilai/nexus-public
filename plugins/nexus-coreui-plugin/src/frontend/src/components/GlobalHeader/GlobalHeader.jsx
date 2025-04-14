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

import React from "react";

import {
  NxButton,
  NxFontAwesomeIcon,
  NxGlobalHeader2,
} from "@sonatype/react-shared-components";
import { faSync } from "@fortawesome/free-solid-svg-icons";
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import proLogo from "../../../../art/logos/logo-pro-edition-header.svg"
import ceLogo from "../../../../art/logos/logo-community-edition-header.svg";
import coreLogo from "../../../../art/logos/logo-core-edition-header.svg";
import { useRouter } from '@uirouter/react';

import './Globalheader.scss'
import HelpMenu from './HelpMenu';
import LoginAndUserButton from './LogInAndUserProfileMenu';
import Search from './Search';
import SystemStatus from './SystemStatus';

export default function GlobalHeader() {
  const COMMUNITY = "COMMUNITY";
  const PRO = "PRO";

  const router = useRouter();
  const edition = ExtJS.useState(() => ExtJS.state().getEdition());

  const refreshTitle = "Refresh"

  return (
      <NxGlobalHeader2
          homeHref="/"
          logoProps={getLogoProps()}
          className="nxrm-global-header"
      >
        <Search />

        <SystemStatus />

        <NxButton
            title={refreshTitle}
            aria-label={refreshTitle}
            variant="icon-only"
            onClick={onRefreshClick}
            data-analytics-id="nxrm-global-header-refresh-button"
        >
          <NxFontAwesomeIcon icon={faSync} />
        </NxButton>

        <HelpMenu />

        <LoginAndUserButton />
      </NxGlobalHeader2>);

  function onRefreshClick() {
    if (ExtJS.isExtJsRendered()) {
      ExtJS.refresh();
    } else {
      router.stateService.reload();
    }
  }

  function getLogoProps() {
    return {
      lightPath: getLogo(),
      darkPath: getLogo(), // Needs to be replaced with a true dark mode logo before we can enable dark mode
      altText: `Sonatype Nexus Repository ${getEditionText()}`
    }
  }

  function getLogo() {
    return edition === COMMUNITY ? ceLogo
        : edition === PRO ? proLogo
        : coreLogo; // Core or catch all for unknown
  }

  function getEditionText() {
    return edition === COMMUNITY ? "Community"
        : edition === PRO ? "Professional"
        : "Core";
  }
}
