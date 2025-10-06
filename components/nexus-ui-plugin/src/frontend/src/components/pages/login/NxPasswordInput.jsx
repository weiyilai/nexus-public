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

import { faEye, faEyeSlash } from '@fortawesome/free-solid-svg-icons';
import { NxFontAwesomeIcon, NxTextInput } from '@sonatype/react-shared-components';
import React, { useState } from 'react';
import './NxPasswordInput.scss';

/**
 * Custom password input with visibility toggle functionality
 */
export default function NxPasswordInput({ validationErrors, inputAttributes, ...props }) {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <div className="nx-password-input">
      <NxTextInput
        {...props}
        type={showPassword ? 'text' : 'password'}
        validationErrors={validationErrors}
        {...inputAttributes}
      />
      <button
        type="button"
        onClick={() => setShowPassword(!showPassword)}
        className="nx-password-input__toggle"
        aria-label={showPassword ? 'Hide password' : 'Show password'}
      >
        <NxFontAwesomeIcon icon={showPassword ? faEyeSlash : faEye} />
      </button>
    </div>
  );
}
