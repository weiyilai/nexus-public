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
package org.sonatype.nexus.internal.security.apikey;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.internal.security.apikey.store.ApiKeyStore;
import org.sonatype.nexus.internal.security.apikey.store.ApiKeyStoreImpl;
import org.sonatype.nexus.internal.security.apikey.store.ApiKeyStoreV2Impl;
import org.sonatype.nexus.internal.security.apikey.store.ApiKeyV2DAO;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.KeyValueEvent;
import org.sonatype.nexus.security.UserPrincipalsExpired;
import org.sonatype.nexus.security.UserPrincipalsHelper;
import org.sonatype.nexus.security.authc.apikey.ApiKeyFactory;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseExtension;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
@ExtendWith(DatabaseExtension.class)
class ApiKeyServiceImplTest
    extends Test5Support
{
  private static final String NUGET = "Nuget";

  private static final PrincipalCollection PRINCIPALS = new SimplePrincipalCollection("bob", "ldap");

  private static final PrincipalCollection MISSING_USER = new SimplePrincipalCollection("johndoe", "ldap");

  private static final char[] TOKEN = "token".toCharArray();

  @Mock
  private DatabaseCheck check;

  @Mock
  private GlobalKeyValueStore kv;

  @Mock
  private UserPrincipalsHelper principalsHelper;

  @Mock
  private List<ApiKeyFactory> apiKeyFactories;

  @Mock
  private DefaultApiKeyFactory defaultApiKeyFactory;

  @Mock
  private EventManager eventManager;

  @Mock
  private ApiKeyStoreImpl v1;

  @Mock
  private ApiKeyStoreV2Impl v2;

  @DataSessionConfiguration(daos = ApiKeyV2DAO.class)
  TestDataSessionSupplier sessionRule;

  private ApiKeyServiceImpl underTest;

  @BeforeEach
  public void setup() {
    underTest = new ApiKeyServiceImpl(v1, v2, check, kv, principalsHelper, apiKeyFactories, defaultApiKeyFactory);
  }

  @DatabaseTest
  public void testBrowse_migrationComplete() {
    setMigrationComplete();
    underTest.browse(NUGET);
    verify(v2).browse(NUGET);
    verifyNoInteractions(v1);
  }

  @DatabaseTest
  public void testBrowse_migrationInProgress() {
    setMigrationInProgress();
    underTest.browse(NUGET);
    verify(v1).browse(NUGET);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testBrowse_migrationNotStarted() {
    setMigrationNotStarted();
    underTest.browse(NUGET);
    verify(v1).browse(NUGET);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testBrowseByCreatedDate_migrationComplete() {
    setMigrationComplete();
    underTest.browseByCreatedDate(NUGET, OffsetDateTime.now());
    verify(v2).browseByCreatedDate(anyString(), any());
    verifyNoInteractions(v1);
  }

  @DatabaseTest
  public void testBrowseByCreatedDate_migrationInProgress() {
    setMigrationInProgress();
    underTest.browseByCreatedDate(NUGET, OffsetDateTime.now());
    verify(v1).browseByCreatedDate(anyString(), any());
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testBrowseByCreatedDate_migrationNotStarted() {
    setMigrationNotStarted();
    underTest.browseByCreatedDate(NUGET, OffsetDateTime.now());
    verify(v1).browseByCreatedDate(anyString(), any());
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testBrowsePaginated_migrationComplete() {
    setMigrationComplete();
    underTest.browsePaginated(NUGET, 0, 100);
    verify(v2).browsePaginated(NUGET, 0, 100);
    verifyNoInteractions(v1);
  }

  @DatabaseTest
  public void testBrowsePaginated_migrationInProgress() {
    setMigrationInProgress();
    underTest.browsePaginated(NUGET, 0, 100);
    verify(v1).browsePaginated(NUGET, 0, 100);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testBrowsePaginated_migrationNotStarted() {
    setMigrationNotStarted();
    underTest.browsePaginated(NUGET, 0, 100);
    verify(v1).browsePaginated(NUGET, 0, 100);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testCount_migrationComplete() {
    setMigrationComplete();
    underTest.count(NUGET);
    verify(v2).count(NUGET);
    verifyNoInteractions(v1);
  }

  @DatabaseTest
  public void testCount_migrationInProgress() {
    setMigrationInProgress();

    when(v1.count(any())).thenReturn(9);
    when(v2.count(anyString())).thenReturn(99);

    assertThat(underTest.count(NUGET), is(99));
    verify(v1).count(NUGET);
    verify(v2).count(NUGET);
  }

  @DatabaseTest
  public void testCount_migrationNotStarted() {
    setMigrationNotStarted();
    underTest.count(NUGET);
    verify(v1).count(NUGET);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testCreateApiKey_migrationComplete() {
    setMigrationComplete();
    underTest.createApiKey(NUGET, PRINCIPALS);
    verify(v2).persistApiKey(anyString(), any(), any());
    verifyNoInteractions(v1);
  }

  @DatabaseTest
  public void testCreateApiKey_migrationInProgress() {
    setMigrationInProgress();
    underTest.createApiKey(NUGET, PRINCIPALS);
    verify(v1).persistApiKey(anyString(), any(), any());
    verify(v2).persistApiKey(anyString(), any(), any());
  }

  @DatabaseTest
  public void testCreateApiKey_migrationNotStarted() {
    setMigrationNotStarted();
    underTest.createApiKey(NUGET, PRINCIPALS);
    verify(v1).persistApiKey(anyString(), any(), any());
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testDeleteApiKey_migrationComplete() {
    setMigrationComplete();
    underTest.deleteApiKey(NUGET, PRINCIPALS);
    verify(v2).deleteApiKey(NUGET, PRINCIPALS);
    verifyNoInteractions(v1);
  }

  @DatabaseTest
  public void testDeleteApiKey_migrationInProgress() {
    setMigrationInProgress();
    when(v1.deleteApiKey(anyString(), any())).thenReturn(99);
    when(v2.deleteApiKey(anyString(), any())).thenReturn(9);

    assertThat(underTest.deleteApiKey(NUGET, PRINCIPALS), is(99));
    verify(v1).deleteApiKey(NUGET, PRINCIPALS);
    verify(v2).deleteApiKey(NUGET, PRINCIPALS);
  }

  @DatabaseTest
  public void testDeleteApiKey_migrationNotStarted() {
    setMigrationNotStarted();
    underTest.deleteApiKey(NUGET, PRINCIPALS);
    verify(v1).deleteApiKey(NUGET, PRINCIPALS);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testDeleteApiKeys_OffsetDateTime_migrationComplete() {
    setMigrationComplete();
    underTest.deleteApiKeys(OffsetDateTime.now());
    verify(v2).deleteApiKeys(any(OffsetDateTime.class));
    verifyNoInteractions(v1);
  }

  @DatabaseTest
  public void testDeleteApiKeys_OffsetDateTime_migrationInProgress() {
    setMigrationInProgress();
    when(v1.deleteApiKeys(any(OffsetDateTime.class))).thenReturn(9);
    when(v2.deleteApiKeys(any(OffsetDateTime.class))).thenReturn(99);

    assertThat(underTest.deleteApiKeys(OffsetDateTime.now()), is(99));
    verify(v1).deleteApiKeys(any(OffsetDateTime.class));
    verify(v2).deleteApiKeys(any(OffsetDateTime.class));
  }

  @DatabaseTest
  public void testDeleteApiKeys_OffsetDateTime_migrationNotStarted() {
    setMigrationNotStarted();
    underTest.deleteApiKeys(OffsetDateTime.now());
    verify(v1).deleteApiKeys(any(OffsetDateTime.class));
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testDeleteApiKeys_Principals_migrationComplete() {
    setMigrationComplete();
    underTest.deleteApiKeys(PRINCIPALS);
    verify(v2).deleteApiKeys(PRINCIPALS);
    verifyNoInteractions(v1);
  }

  @DatabaseTest
  public void testDeleteApiKeys_Principals_migrationInProgress() {
    setMigrationInProgress();
    when(v1.deleteApiKeys(PRINCIPALS)).thenReturn(9);
    when(v2.deleteApiKeys(PRINCIPALS)).thenReturn(99);

    assertThat(underTest.deleteApiKeys(PRINCIPALS), is(99));
    verify(v1).deleteApiKeys(PRINCIPALS);
    verify(v2).deleteApiKeys(PRINCIPALS);
  }

  @DatabaseTest
  public void testDeleteApiKeys_Principals_migrationNotStarted() {
    setMigrationNotStarted();
    underTest.deleteApiKeys(PRINCIPALS);
    verify(v1).deleteApiKeys(PRINCIPALS);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testDeleteApiKeys_Domain_migrationComplete() {
    setMigrationComplete();
    underTest.deleteApiKeys(NUGET);
    verify(v2).deleteApiKeys(NUGET);
    verifyNoInteractions(v1);
  }

  @DatabaseTest
  public void testDeleteApiKeys_Domain_migrationInProgress() {
    setMigrationInProgress();
    when(v1.deleteApiKeys(NUGET)).thenReturn(9);
    when(v2.deleteApiKeys(NUGET)).thenReturn(99);

    assertThat(underTest.deleteApiKeys(NUGET), is(99));
    verify(v1).deleteApiKeys(NUGET);
    verify(v2).deleteApiKeys(NUGET);
  }

  @DatabaseTest
  public void testDeleteApiKeys_Domain_migrationNotStarted() {
    setMigrationNotStarted();
    underTest.deleteApiKeys(NUGET);
    verify(v1).deleteApiKeys(NUGET);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testGetApiKey_migrationComplete() {
    setMigrationComplete();
    underTest.getApiKey(NUGET, PRINCIPALS);
    verify(v2).getApiKey(NUGET, PRINCIPALS);
    verifyNoInteractions(v1);
  }

  @DatabaseTest
  public void testGetApiKey_migrationInProgress() {
    setMigrationInProgress();
    underTest.getApiKey(NUGET, PRINCIPALS);
    verify(v1).getApiKey(NUGET, PRINCIPALS);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testGetApiKey_migrationNotStarted() {
    setMigrationNotStarted();
    underTest.getApiKey(NUGET, PRINCIPALS);
    verify(v1).getApiKey(NUGET, PRINCIPALS);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testGetApiKeyByToken_migrationComplete() {
    setMigrationComplete();
    underTest.getApiKeyByToken(NUGET, TOKEN);
    verify(v2).getApiKeyByToken(NUGET, TOKEN);
    verifyNoInteractions(v1);
  }

  @DatabaseTest
  public void testGetApiKeyByToken_migrationInProgress() {
    setMigrationInProgress();
    underTest.getApiKeyByToken(NUGET, TOKEN);
    verify(v1).getApiKeyByToken(NUGET, TOKEN);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testGetApiKeyByToken_migrationNotStarted() {
    setMigrationNotStarted();
    underTest.getApiKeyByToken(NUGET, TOKEN);
    verify(v1).getApiKeyByToken(NUGET, TOKEN);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testPersistApiKey_migrationComplete() {
    setMigrationComplete();
    underTest.persistApiKey(NUGET, PRINCIPALS, TOKEN);
    verify(v2).persistApiKey(NUGET, PRINCIPALS, TOKEN, null);
    verifyNoInteractions(v1);
  }

  @DatabaseTest
  public void testPersistApiKey_migrationInProgress() {
    setMigrationInProgress();
    underTest.persistApiKey(NUGET, PRINCIPALS, TOKEN);
    verify(v1).persistApiKey(NUGET, PRINCIPALS, TOKEN, null);
    verify(v2).persistApiKey(NUGET, PRINCIPALS, TOKEN, null);
  }

  @DatabaseTest
  public void testPersistApiKey_migrationNotStarted() {
    setMigrationNotStarted();
    underTest.persistApiKey(NUGET, PRINCIPALS, TOKEN);
    verify(v1).persistApiKey(NUGET, PRINCIPALS, TOKEN, null);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testPersistApiKey_withCreated_migrationComplete() {
    setMigrationComplete();
    underTest.persistApiKey(NUGET, PRINCIPALS, TOKEN, OffsetDateTime.now());
    verify(v2).persistApiKey(eq(NUGET), any(), eq(TOKEN), any());
    verifyNoInteractions(v1);
  }

  @DatabaseTest
  public void testPersistApiKey_withCreated_migrationInProgress() {
    setMigrationInProgress();
    underTest.persistApiKey(NUGET, PRINCIPALS, TOKEN, OffsetDateTime.now());
    verify(v1).persistApiKey(eq(NUGET), any(), eq(TOKEN), any());
    verify(v2).persistApiKey(eq(NUGET), any(), eq(TOKEN), any());
  }

  @DatabaseTest
  public void testPersistApiKey_withCreated_migrationNotStarted() {
    setMigrationNotStarted();
    underTest.persistApiKey(NUGET, PRINCIPALS, TOKEN, OffsetDateTime.now());
    verify(v1).persistApiKey(eq(NUGET), any(), eq(TOKEN), any());
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testPurgeApiKeys_migrationComplete() {
    setupPurge(v2);
    setMigrationComplete();
    underTest.purgeApiKeys();
    verify(v2).browsePrincipals();
    verify(v2).deleteApiKeys(MISSING_USER);
    verifyNoInteractions(v1);
  }

  @DatabaseTest
  public void testPurgeApiKeys_migrationInProgress() {
    setupPurge(v1);
    setMigrationInProgress();
    underTest.purgeApiKeys();
    verify(v1).browsePrincipals();
    verify(v1).deleteApiKeys(MISSING_USER);
    verify(v2).deleteApiKeys(MISSING_USER);
    verifyNoMoreInteractions(v2);
  }

  @DatabaseTest
  public void testPurgeApiKeys_migrationNotStarted() {
    setupPurge(v1);
    setMigrationNotStarted();
    underTest.purgeApiKeys();
    verify(v1).browsePrincipals();
    verify(v1).deleteApiKeys(MISSING_USER);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testUpdateApiKeyRealm_migrationComplete() {
    setMigrationComplete();
    ApiKeyInternal from = mock(ApiKeyInternal.class);
    PrincipalCollection to = mock(PrincipalCollection.class);
    underTest.updateApiKeyRealm(from, to);
    verify(v2).updateApiKey(from, to);
    verifyNoInteractions(v1);
  }

  @DatabaseTest
  public void testUpdateApiKeyRealm_migrationInProgress() {
    setMigrationInProgress();
    ApiKeyInternal from = mock(ApiKeyInternal.class);
    PrincipalCollection to = mock(PrincipalCollection.class);
    underTest.updateApiKeyRealm(from, to);
    verify(v1).updateApiKey(from, to);
    verify(v2).updateApiKey(from, to);
  }

  @DatabaseTest
  public void testUpdateApiKeyRealm_migrationNotStarted() {
    setMigrationNotStarted();
    ApiKeyInternal from = mock(ApiKeyInternal.class);
    PrincipalCollection to = mock(PrincipalCollection.class);
    underTest.updateApiKeyRealm(from, to);
    verify(v1).updateApiKey(from, to);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testOn_KeyValueEvent() {
    setMigrationNotStarted();
    assertFalse(underTest.isMigrationComplete());
    underTest.on(new KeyValueEvent(ApiKeyServiceImpl.MIGRATION_COMPLETE, true));
    assertTrue(underTest.isMigrationComplete());
  }

  @DatabaseTest
  public void testOn_UserPrincipalsExpired_migrationNotStarted() {
    setMigrationNotStarted();
    underTest.on(new UserPrincipalsExpired("bob", "ldap"));
    verify(v1).deleteApiKeys(PRINCIPALS);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testOn_UserPrincipalsExpired_migrationInProgress() {
    setMigrationInProgress();
    underTest.on(new UserPrincipalsExpired("bob", "ldap"));
    verify(v1).deleteApiKeys(PRINCIPALS);
    verify(v2).deleteApiKeys(PRINCIPALS);
  }

  @DatabaseTest
  public void testOn_UserPrincipalsExpired_migrationComplete() {
    setMigrationComplete();
    underTest.on(new UserPrincipalsExpired("bob", "ldap"));
    verifyNoInteractions(v1);
    verify(v2).deleteApiKeys(PRINCIPALS);
  }

  @DatabaseTest
  public void testOn_UserPrincipalsExpired_null_migrationNotStarted() {
    setMigrationNotStarted();
    setupPurge(v1);
    underTest.on(new UserPrincipalsExpired());
    verify(v1).deleteApiKeys(MISSING_USER);
    verifyNoInteractions(v2);
  }

  @DatabaseTest
  public void testOn_UserPrincipalsExpired_null_migrationInProgress() {
    setMigrationInProgress();
    setupPurge(v1, v2);
    underTest.on(new UserPrincipalsExpired());
    verify(v1).deleteApiKeys(MISSING_USER);
    verify(v2).deleteApiKeys(MISSING_USER);
  }

  @DatabaseTest
  public void testOn_UserPrincipalsExpired_null_migrationComplete() {
    setMigrationComplete();
    setupPurge(v2);
    underTest.on(new UserPrincipalsExpired());
    verify(v2).deleteApiKeys(MISSING_USER);
    verifyNoInteractions(v1);
  }

  private void setupPurge(final ApiKeyStore... stores) {
    for (ApiKeyStore store : stores) {
      when(store.browsePrincipals()).thenReturn(Arrays.asList(PRINCIPALS, MISSING_USER));
    }

    try {
      when(principalsHelper.getUserStatus(MISSING_USER)).thenThrow(new UserNotFoundException("Missing"));
    }
    catch (UserNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private void setMigrationComplete() {
    lenient().when(check.isAtLeast(anyString())).thenReturn(true);
    when(kv.getBoolean(any())).thenReturn(Optional.of(true));
    try {
      underTest.start();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void setMigrationInProgress() {
    lenient().when(check.isAtLeast(anyString())).thenReturn(true);
    lenient().when(kv.getBoolean(any())).thenReturn(Optional.empty());
  }

  private void setMigrationNotStarted() {
    lenient().when(check.isAtLeast(anyString())).thenReturn(false);
    lenient().when(kv.getBoolean(any())).thenReturn(Optional.empty());
  }
}
