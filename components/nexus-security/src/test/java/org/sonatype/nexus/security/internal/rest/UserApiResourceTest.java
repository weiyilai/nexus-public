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
package org.sonatype.nexus.security.internal.rest;

import java.util.Collection;
import java.util.Collections;

import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.goodies.testsupport.hamcrest.BeanMatchers;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.config.AdminPasswordFileManager;
import org.sonatype.nexus.security.internal.AdminPasswordFileManagerImpl;
import org.sonatype.nexus.security.internal.RealmToSource;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserSearchCriteria;
import org.sonatype.nexus.security.user.UserStatus;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.ValidationExecutor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class UserApiResourceTest
    extends Test5Support
{
  public static final String USER_ID = "jsmith";

  private static final String SAML_REALM_NAME = "SamlRealm";

  private static final String CROWD_REALM_NAME = "Crowd";

  private static final String LDAP_REALM_NAME = "LdapRealm";

  private static final String NEXUS_AUTHENTICATING_REALM_NAME = "NexusAuthenticatingRealm";

  @ValidationExecutor
  private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private ApplicationDirectories applicationDirectories;

  private AdminPasswordFileManager adminPasswordFileManager;

  @Mock
  private UserManager userManager;

  private UserApiResource underTest;

  @BeforeEach
  void setup() throws Exception {
    when(applicationDirectories.getWorkDirectory()).thenReturn(util.createTempDir());
    adminPasswordFileManager = new AdminPasswordFileManagerImpl(applicationDirectories);
    underTest = new UserApiResource(securitySystem, adminPasswordFileManager);

    final User user = createUser();
    lenient().when(securitySystem.getUser(any(), any())).thenAnswer(i -> {
      if ("jdoe".equals(i.getArguments()[0]) && "LDAP".equals(i.getArguments()[1])) {
        throw new UserNotFoundException((String) i.getArguments()[0]);
      }
      return user;
    });
    lenient().when(securitySystem.getUser(user.getUserId())).thenReturn(user);

    UserManager ldap = mock(UserManager.class);
    lenient().when(ldap.supportsWrite()).thenReturn(false);
    lenient().when(securitySystem.getUserManager("LDAP")).thenReturn(ldap);

    lenient().when(securitySystem.getUserManager(UserManager.DEFAULT_SOURCE)).thenReturn(userManager);
    lenient().when(securitySystem.listRoles(UserManager.DEFAULT_SOURCE))
        .thenReturn(Collections.singleton(new Role("nx-admin", null, null, null, true, null, null)));
    lenient().when(userManager.supportsWrite()).thenReturn(true);
  }

  @AfterEach
  void cleanup() {
    adminPasswordFileManager.removeFile();
  }

  /*
   * Get users
   */
  @Test
  void testGetUsers() {
    when(securitySystem.searchUsers(any())).thenReturn(Collections.singleton(createUser()));
    Collection<ApiUser> users = underTest.getUsers("js", UserManager.DEFAULT_SOURCE);

    assertThat(users, hasSize(1));
    assertThat(users, contains(BeanMatchers.similarTo(underTest.fromUser(createUser()))));

    ArgumentCaptor<UserSearchCriteria> captor = ArgumentCaptor.forClass(UserSearchCriteria.class);
    verify(securitySystem).searchUsers(captor.capture());

    UserSearchCriteria criteria = captor.getValue();
    assertThat(criteria.getUserId(), is("js"));
    assertThat(criteria.getSource(), is(UserManager.DEFAULT_SOURCE));
    assertNull(criteria.getLimit());
  }

  @Test
  void testGetUsers_nonDefaultLimit() {
    when(securitySystem.searchUsers(any())).thenReturn(Collections.singleton(createUser()));

    underTest.getUsers("js", null);

    ArgumentCaptor<UserSearchCriteria> captor = ArgumentCaptor.forClass(UserSearchCriteria.class);
    verify(securitySystem).searchUsers(captor.capture());

    UserSearchCriteria criteria = captor.getValue();
    assertThat(criteria.getUserId(), is("js"));
    assertNull(criteria.getSource());
    assertThat(criteria.getLimit(), is(100));
  }

  /*
   * Create user
   */
  @Test
  void testCreateUser() throws Exception {
    User user = createUser();
    when(securitySystem.addUser(user, "admin123")).thenReturn(user);
    ApiCreateUser createUser = new ApiCreateUser(USER_ID, "John", "Smith", "jsmith@example.org", "admin123",
        ApiUserStatus.disabled, Collections.singleton("nx-admin"));

    ApiUser returned = underTest.createUser(createUser);

    assertThat(returned, BeanMatchers.similarTo(underTest.fromUser(user)));

    verify(securitySystem).addUser(user, "admin123");
  }

  @Test
  void testCreateUser_missingUserManager() throws Exception {
    User user = createUser();
    when(securitySystem.addUser(user, "admin123")).thenThrow(new NoSuchUserManagerException(user.getSource()));

    ApiCreateUser createUser = new ApiCreateUser(USER_ID, "John", "Smith", "jsmith@example.org", "admin123",
        ApiUserStatus.disabled, Collections.singleton("nx-admin"));

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.createUser(createUser), "Unable to locate source: default");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.NOT_FOUND));
  }

  /*
   * Delete user
   */
  @Test
  void testDeleteUsersWithNoRealm() throws Exception {
    underTest.deleteUser(USER_ID, null);

    verify(securitySystem).deleteUser(USER_ID, UserManager.DEFAULT_SOURCE);
  }

  @Test
  void testDeleteUsersWithSamlRealm() throws Exception {
    when(securitySystem.isValidRealm(SAML_REALM_NAME)).thenReturn(true);
    when(securitySystem.getUser(
        USER_ID,
        RealmToSource.getSource(SAML_REALM_NAME))).thenReturn(createUserWithSource(SAML_REALM_NAME));
    underTest.deleteUser(USER_ID, SAML_REALM_NAME);

    verify(securitySystem).deleteUser(USER_ID, RealmToSource.getSource(SAML_REALM_NAME));
  }

  @Test
  void testDeleteUsersWithEmptyRealm() {
    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.deleteUser(USER_ID, ""), "Invalid or empty realm name.");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.BAD_REQUEST));
  }

  @Test
  void testDeleteUsersWithInvalidRealm() {
    when(securitySystem.isValidRealm(any())).thenReturn(false);

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.deleteUser(USER_ID, "InvalidRealm123"), "Invalid or empty realm name.");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.BAD_REQUEST));
  }

  @Test
  void testDeleteUsersWithCrowdRealm() throws Exception {
    when(securitySystem.isValidRealm(CROWD_REALM_NAME)).thenReturn(true);
    when(securitySystem.getUser(USER_ID,
        RealmToSource.getSource(CROWD_REALM_NAME))).thenReturn(createUserWithSource(CROWD_REALM_NAME));
    underTest.deleteUser(USER_ID, CROWD_REALM_NAME);

    verify(securitySystem).deleteUser(USER_ID, RealmToSource.getSource(CROWD_REALM_NAME));
  }

  @Test
  void testDeleteUsersWithLdapRealm() throws Exception {
    when(securitySystem.isValidRealm(LDAP_REALM_NAME)).thenReturn(true);
    when(securitySystem.getUser(USER_ID,
        RealmToSource.getSource(LDAP_REALM_NAME))).thenReturn(createUserWithSource(LDAP_REALM_NAME));
    underTest.deleteUser(USER_ID, LDAP_REALM_NAME);

    verify(securitySystem).deleteUser(USER_ID, RealmToSource.getSource(LDAP_REALM_NAME));
  }

  @Test
  void testDeleteUsersWithDefaultRealm() throws Exception {
    when(securitySystem.isValidRealm(NEXUS_AUTHENTICATING_REALM_NAME)).thenReturn(true);
    when(securitySystem.getUser(USER_ID,
        RealmToSource.getSource(NEXUS_AUTHENTICATING_REALM_NAME))).thenReturn(
        createUserWithSource(NEXUS_AUTHENTICATING_REALM_NAME));
    underTest.deleteUser(USER_ID, NEXUS_AUTHENTICATING_REALM_NAME);

    verify(securitySystem).deleteUser(USER_ID, "default");
  }

  @Test
  void testDeleteUsers_missingUser() throws Exception {
    when(securitySystem.getUser("unknownuser")).thenThrow(new UserNotFoundException("unknownuser"));

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.deleteUser("unknownuser", null), "User 'unknownuser' not found.");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.NOT_FOUND));
  }

  @Test
  void testDeleteUsers_somethingWonky() throws Exception {
    User user = createUser();
    doThrow(new NoSuchUserManagerException(user.getSource())).when(securitySystem)
        .deleteUser(user.getUserId(),
            user.getSource());

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.deleteUser(USER_ID, null), "Unable to locate source: " + user.getSource());
    assertThat(ex.getResponse().getStatusInfo(), is(Status.NOT_FOUND));
  }

  /*
   * Update user
   */
  @Test
  void testUpdateUser() throws Exception {
    User user = createUser();
    underTest.updateUser(USER_ID, underTest.fromUser(user));

    verify(securitySystem).updateUser(user);
  }

  @Test
  void testUpdateUser_nullExternal() throws Exception {
    User user = createUser();
    ApiUser apiUser = underTest.fromUser(user);
    apiUser.setExternalRoles(null);

    underTest.updateUser(USER_ID, apiUser);

    verify(securitySystem).updateUser(user);
  }

  @Test
  void testUpdateUser_externalSource() throws Exception {
    User user = createUser();
    user.setSource("LDAP");
    ApiUser apiUser = underTest.fromUser(user);
    underTest.updateUser(USER_ID, apiUser);

    verify(securitySystem).setUsersRoles(USER_ID, "LDAP", user.getRoles());
  }

  @Test
  void testUpdateUser_externalSource_unknownUser() {
    User user = createUser();

    ApiUser apiUser = new ApiUser("jdoe", user.getFirstName(), user.getLastName(), user.getEmailAddress(),
        "LDAP", ApiUserStatus.convert(user.getStatus()), true, Collections.singleton("nx-admin"),
        Collections.emptySet());

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.updateUser("jdoe", apiUser), "User 'jdoe' not found.");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.NOT_FOUND));
  }

  @Test
  void testDeleteLdapUser() throws Exception {
    when(securitySystem.getUser(any())).thenReturn(createLdapUser());

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.deleteUser("tanderson", null), "Non-local user cannot be deleted.");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.BAD_REQUEST));
  }

  @Test
  void testUpdateUser_mismatch() throws Exception {
    User user = createUser();
    ApiUser apiUser = underTest.fromUser(user);
    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.updateUser("fred", apiUser), "The path's userId does not match the body");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.BAD_REQUEST));
  }

  @Test
  void testUpdateUser_unknownSource() throws Exception {
    User user = createUser();

    when(securitySystem.updateUser(user)).thenThrow(new NoSuchUserManagerException(user.getSource()));

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.updateUser(USER_ID, underTest.fromUser(user)), "Unable to locate source: " + user.getSource());
    assertThat(ex.getResponse().getStatusInfo(), is(Status.NOT_FOUND));
  }

  @Test
  void testUpdateUser_unknownUser() throws Exception {
    User user = createUser();

    when(securitySystem.updateUser(user)).thenThrow(new UserNotFoundException(user.getUserId()));

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.updateUser(USER_ID, underTest.fromUser(user)),
        "User '%s' not found.".formatted(user.getUserId()));
    assertThat(ex.getResponse().getStatusInfo(), is(Status.NOT_FOUND));
  }

  /*
   * Change password
   */

  @Test
  void testChangePassword() throws Exception {
    underTest.changePassword("test", "test");

    verify(securitySystem).changePassword("test", "test");
  }

  @Test
  void testChangePassword_invalidUser() throws Exception {
    doThrow(new UserNotFoundException("test")).when(securitySystem).changePassword("test", "test");

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.changePassword("test", "test"), "User 'test' not found.");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.NOT_FOUND));
  }

  @Test
  void testChangePassword_missingPassword() throws Exception {
    ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
        () -> underTest.changePassword("test", null), "Password must be supplied.");
    assertThat(ex.getConstraintViolations(), hasSize(1));

    verify(securitySystem, never()).changePassword(any(), any());
  }

  @Test
  void testChangePassword_emptyPassword() throws Exception {
    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.changePassword("test", ""), "Password must be supplied.");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.BAD_REQUEST));
    verify(securitySystem, never()).changePassword(any(), any());
  }

  @Test
  void testChangePassword_defaultAdminRemoval() throws Exception {
    adminPasswordFileManager.writeFile("oldPassword");

    underTest.changePassword("admin", "newPassword");

    assertThat(adminPasswordFileManager.exists(), is(false));
  }

  @Test
  void testChangePassword_defaultAdminNotRemoved() throws Exception {
    adminPasswordFileManager.writeFile("oldPassword");

    underTest.changePassword("test", "test");

    verify(securitySystem).changePassword("test", "test");
    assertThat(adminPasswordFileManager.exists(), is(true));
  }

  private static User createUser() {
    User user = new User();
    user.setEmailAddress("john@example.org");
    user.setFirstName("John");
    user.setLastName("Smith");
    user.setReadOnly(false);
    user.setStatus(UserStatus.disabled);
    user.setUserId(USER_ID);
    user.setVersion(1);
    user.setSource(UserManager.DEFAULT_SOURCE);
    user.setRoles(Collections.singleton(new RoleIdentifier(UserManager.DEFAULT_SOURCE, "nx-admin")));
    return user;
  }

  private static User createLdapUser() {
    User user = new User();
    user.setEmailAddress("thomas@example.org");
    user.setFirstName("Thomas");
    user.setLastName("Anderson");
    user.setReadOnly(false);
    user.setStatus(UserStatus.disabled);
    user.setUserId("tanderson");
    user.setVersion(1);
    user.setSource("LDAP");
    user.setRoles(Collections.singleton(new RoleIdentifier(UserManager.DEFAULT_SOURCE, "nx-admin")));
    return user;
  }

  private static User createUserWithSource(final String realm) {
    User user = new User();
    user.setEmailAddress("john@example.org");
    user.setFirstName("John");
    user.setLastName("Smith");
    user.setReadOnly(false);
    user.setStatus(UserStatus.disabled);
    user.setUserId(USER_ID);
    user.setVersion(1);
    user.setSource(RealmToSource.getSource(realm));
    user.setRoles(Collections.singleton(new RoleIdentifier(RealmToSource.getSource(realm), "nx-admin")));
    return user;
  }
}
