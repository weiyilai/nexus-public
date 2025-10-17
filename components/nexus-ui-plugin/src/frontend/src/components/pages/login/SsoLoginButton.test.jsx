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

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { useRouter } from '@uirouter/react';
import SsoLoginButton from './SsoLoginButton';

jest.mock('@sonatype/nexus-ui-plugin');
jest.mock('@uirouter/react');

describe('SsoLoginButton', () => {
  const mockUseState = jest.fn();
  const mockState = jest.fn();

  beforeEach(() => {
    ExtJS.useState = mockUseState;
    ExtJS.state = mockState;
    mockState.mockReturnValue({
      getValue: jest.fn()
    });

    useRouter.mockReturnValue({
      globals: {
        params: {}
      }
    });

    Object.defineProperty(window, 'location', {
      value: {
        assign: jest.fn()
      },
      writable: true
    });

    jest.clearAllMocks();
  });

  describe('visibility', () => {
    it('renders null when neither SAML nor OAuth2 is enabled', () => {
      mockUseState
        .mockReturnValueOnce(false) // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath

      const { container } = render(<SsoLoginButton />);
      expect(container.firstChild).toBeNull();
    });

    it('renders button when SAML is enabled', () => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath

      render(<SsoLoginButton />);
      expect(screen.getByRole('button', { name: /continue with sso/i })).toBeInTheDocument();
    });

    it('renders button when OAuth2 is enabled', () => {
      mockUseState
        .mockReturnValueOnce(false) // samlEnabled
        .mockReturnValueOnce(true)  // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath

      render(<SsoLoginButton />);
      expect(screen.getByRole('button', { name: /continue with sso/i })).toBeInTheDocument();
    });

    it('renders button when both SAML and OAuth2 are enabled', () => {
      mockUseState
        .mockReturnValueOnce(true) // samlEnabled
        .mockReturnValueOnce(true) // oauth2Enabled
        .mockReturnValueOnce('');  // contextPath

      render(<SsoLoginButton />);
      expect(screen.getByRole('button', { name: /continue with sso/i })).toBeInTheDocument();
    });
  });

  describe('button properties', () => {
    beforeEach(() => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath
    });

    it('has correct attributes', () => {
      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      expect(button).toHaveAttribute('type', 'button');
      expect(button).toHaveAttribute('data-analytics-id', 'nxrm-login-sso');
      expect(button).toHaveClass('sso-login-button');
    });

    it('displays correct text when not loading', () => {
      render(<SsoLoginButton />);
      expect(screen.getByText('Continue with SSO')).toBeInTheDocument();
    });
  });

  describe('SAML redirect', () => {
    it('redirects to /saml with empty context path', async () => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/saml');
      });
    });

    it('redirects to context path + /saml with context path', async () => {
      mockUseState
        .mockReturnValueOnce(true)      // samlEnabled
        .mockReturnValueOnce(false)     // oauth2Enabled
        .mockReturnValueOnce('/nexus'); // contextPath

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/nexus/saml');
      });
    });

    it('handles context path with trailing slash', async () => {
      mockUseState
        .mockReturnValueOnce(true)       // samlEnabled
        .mockReturnValueOnce(false)      // oauth2Enabled
        .mockReturnValueOnce('/nexus/'); // contextPath

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/nexus/saml');
      });
    });
  });

  describe('OAuth2 redirect', () => {
    it('redirects to /oidc/login with empty context path', async () => {
      mockUseState
        .mockReturnValueOnce(false) // samlEnabled
        .mockReturnValueOnce(true)  // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/oidc/login');
      });
    });

    it('redirects to context path + /oidc/login with context path', async () => {
      mockUseState
        .mockReturnValueOnce(false)     // samlEnabled
        .mockReturnValueOnce(true)      // oauth2Enabled
        .mockReturnValueOnce('/nexus'); // contextPath

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/nexus/oidc/login');
      });
    });
  });

  describe('SAML priority', () => {
    it('prefers SAML when both SAML and OAuth2 are enabled', async () => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(true)  // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/saml');
      });
    });
  });

  describe('button interaction', () => {
    beforeEach(() => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath
    });

    it('triggers redirect when clicked', () => {
      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      expect(window.location.assign).toHaveBeenCalledWith('/saml');
    });
  });

  describe('accessibility', () => {
    beforeEach(() => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath
    });

    it('is keyboard accessible', () => {
      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      // Check that the button can receive focus and has proper attributes
      expect(button).toHaveAttribute('type', 'button');
      expect(button).not.toHaveAttribute('tabindex', '-1');
    });
  });

  describe('analytics', () => {
    beforeEach(() => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath
    });

    it('has analytics attribute', () => {
      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      expect(button).toHaveAttribute('data-analytics-id', 'nxrm-login-sso');
    });
  });

  describe('SAML redirect with returnTo parameter', () => {
    it('redirects to /saml without hash when returnTo is not provided', async () => {
      mockUseState
          .mockReturnValueOnce(true)  // samlEnabled
          .mockReturnValueOnce(false) // oauth2Enabled
          .mockReturnValueOnce('');   // contextPath

      useRouter.mockReturnValue({
        globals: {
          params: {} // No returnTo
        }
      });

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/saml');
      });
    });

    it('redirects to /saml with hash parameter when returnTo is provided', async () => {
      mockUseState
          .mockReturnValueOnce(true)  // samlEnabled
          .mockReturnValueOnce(false) // oauth2Enabled
          .mockReturnValueOnce('');   // contextPath

      useRouter.mockReturnValue({
        globals: {
          params: {
            returnTo: '%23admin%2Fsupport%2Fstatus'
          }
        }
      });

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/saml?hash=%2523admin%252Fsupport%252Fstatus');
      });
    });

    it('redirects to /saml with hash parameter for different routes', async () => {
      mockUseState
          .mockReturnValueOnce(true)  // samlEnabled
          .mockReturnValueOnce(false) // oauth2Enabled
          .mockReturnValueOnce('');   // contextPath

      useRouter.mockReturnValue({
        globals: {
          params: {
            returnTo: '%23browse%2Fwelcome'
          }
        }
      });

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/saml?hash=%2523browse%252Fwelcome');
      });
    });

    it('redirects with context path and hash parameter when returnTo is provided', async () => {
      mockUseState
          .mockReturnValueOnce(true)      // samlEnabled
          .mockReturnValueOnce(false)     // oauth2Enabled
          .mockReturnValueOnce('/nexus'); // contextPath

      useRouter.mockReturnValue({
        globals: {
          params: {
            returnTo: '%23admin%2Frepository%2Frepositories'
          }
        }
      });

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/nexus/saml?hash=%2523admin%252Frepository%252Frepositories');
      });
    });

    it('handles empty returnTo parameter', async () => {
      mockUseState
          .mockReturnValueOnce(true)  // samlEnabled
          .mockReturnValueOnce(false) // oauth2Enabled
          .mockReturnValueOnce('');   // contextPath

      useRouter.mockReturnValue({
        globals: {
          params: {
            returnTo: ''
          }
        }
      });

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/saml');
      });
    });

    it('handles returnTo with query parameters', async () => {
      mockUseState
          .mockReturnValueOnce(true)  // samlEnabled
          .mockReturnValueOnce(false) // oauth2Enabled
          .mockReturnValueOnce('');   // contextPath

      useRouter.mockReturnValue({
        globals: {
          params: {
            returnTo: '%23admin%2Fsupport%2Fstatus%3Ftab%3Dsystem'
          }
        }
      });

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/saml?hash=%2523admin%252Fsupport%252Fstatus%253Ftab%253Dsystem');
      });
    });

    it('does not add hash parameter for OAuth2 even with returnTo', async () => {
      mockUseState
          .mockReturnValueOnce(false) // samlEnabled
          .mockReturnValueOnce(true)  // oauth2Enabled
          .mockReturnValueOnce('');   // contextPath

      useRouter.mockReturnValue({
        globals: {
          params: {
            returnTo: '%23admin%2Fsupport%2Fstatus'
          }
        }
      });

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/oidc/login');
      });
    });
  });

  describe('button behavior on click', () => {
    it('calls window.location.assign when clicked', () => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      expect(window.location.assign).toHaveBeenCalledWith('/saml');
      expect(window.location.assign).toHaveBeenCalledTimes(1);
    });

    it('prevents multiple redirects on rapid clicks', () => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      // Simulate rapid clicks
      fireEvent.click(button);
      fireEvent.click(button);
      fireEvent.click(button);

      // Only the first click should trigger redirect (button gets disabled)
      expect(window.location.assign).toHaveBeenCalledTimes(1);
    });
  });

  describe('edge cases', () => {
    it('handles context path as single slash', async () => {
      mockUseState
          .mockReturnValueOnce(true)  // samlEnabled
          .mockReturnValueOnce(false) // oauth2Enabled
          .mockReturnValueOnce('/');  // contextPath

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/saml');
      });
    });

    it('handles returnTo with special characters', async () => {
      mockUseState
          .mockReturnValueOnce(true)  // samlEnabled
          .mockReturnValueOnce(false) // oauth2Enabled
          .mockReturnValueOnce('');   // contextPath

      useRouter.mockReturnValue({
        globals: {
          params: {
            returnTo: '%23browse%2Fsearch%3Fq%3Dtest%2520package'
          }
        }
      });

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/saml?hash=%2523browse%252Fsearch%253Fq%253Dtest%252520package');
      });
    });
  });
});
