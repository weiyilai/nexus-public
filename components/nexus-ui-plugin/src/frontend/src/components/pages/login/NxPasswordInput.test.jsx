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

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import NxPasswordInput from './NxPasswordInput';

describe('NxPasswordInput', () => {
  it('should toggle password visibility', async () => {
    render(<NxPasswordInput value="test" onChange={() => {}} />);

    const input = screen.getByDisplayValue('test');
    const toggleButton = screen.getByLabelText('Show password');

    expect(input).toHaveAttribute('type', 'password');

    await userEvent.click(toggleButton);
    expect(input).toHaveAttribute('type', 'text');
    expect(screen.getByLabelText('Hide password')).toBeInTheDocument();
  });

  it('should always show toggle button', () => {
    render(<NxPasswordInput value="test" validationErrors="Error" onChange={() => {}} />);

    const toggleButton = screen.getByLabelText('Show password');
    expect(toggleButton).toBeInTheDocument();
  });

  it('should pass inputAttributes correctly', () => {
    render(
      <NxPasswordInput
        value="test"
        onChange={() => {}}
        inputAttributes={{ id: 'test-id', 'data-testid': 'password-input' }}
      />
    );

    // Query the input directly by its id attribute
    const inputElement = document.getElementById('test-id');
    expect(inputElement).toBeInTheDocument();
    expect(inputElement).toHaveAttribute('type', 'password');

    // Also verify the data-testid is available for the container
    const container = screen.getByTestId('password-input');
    expect(container).toBeInTheDocument();
  });

  it('should render with validation errors', () => {
    render(<NxPasswordInput value="test" validationErrors="This field is required" onChange={() => {}} />);

    // The component should render without crashing
    const input = screen.getByDisplayValue('test');
    expect(input).toBeInTheDocument();
  });

  it('should have proper accessibility attributes', () => {
    render(<NxPasswordInput value="test" onChange={() => {}} />);

    const toggleButton = screen.getByLabelText('Show password');
    expect(toggleButton).toHaveAttribute('aria-label', 'Show password');
    expect(toggleButton).toHaveAttribute('type', 'button');
  });
});
