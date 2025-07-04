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
package org.sonatype.nexus.coreui.internal.capability;

import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.capability.Capability;
import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityDescriptorRegistry;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.rapture.PasswordPlaceholder;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import com.google.common.base.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link CapabilityComponent}.
 */
@ExtendWith(ValidationExtension.class)
class CapabilityComponentTest
    extends Test5Support
{
  @Mock
  private CapabilityDescriptorRegistry capabilityDescriptorRegistry;

  @Mock
  private CapabilityRegistry capabilityRegistry;

  @Mock
  private Capability capability;

  @Mock
  private CapabilityContext capabilityContext;

  @Mock
  private CapabilityReference capabilityReference;

  @Mock
  private CapabilityIdentity capabilityId;

  @Mock
  private CapabilityDescriptor capabilityDescriptor;

  @Captor
  private ArgumentCaptor<String> stringArgumentCaptor;

  @Captor
  private ArgumentCaptor<Map<String, String>> mapArgumentCaptor;

  private CapabilityComponent underTest;

  @BeforeEach
  void setup() {
    underTest = new CapabilityComponent(capabilityDescriptorRegistry, capabilityRegistry);
    when(capabilityContext.properties()).thenReturn(
        Map.of("username", "username", "password", "its a secret to everybody"));
    when(capabilityContext.descriptor()).thenReturn(capabilityDescriptor);
    when(capabilityContext.id()).thenReturn(capabilityId);
    when(capabilityReference.capability()).thenReturn(capability);
    when(capabilityReference.context()).thenReturn(capabilityContext);
    lenient().when(capabilityRegistry.get(any(CapabilityIdentity.class))).thenReturn(capabilityReference);
    lenient().when(capabilityRegistry.get(any(Predicate.class))).thenReturn(singletonList(capabilityReference));
    lenient().when(capabilityRegistry.update(any(), anyBoolean(), any(), any())).thenReturn(capabilityReference);
    when(capabilityDescriptor.type()).thenReturn(mock(CapabilityType.class));
    when(capability.isPasswordProperty(any())).thenAnswer(i -> "password".equals(i.getArgument(0)));
  }

  @Test
  void testReadPasswordNotCleartext() {
    List<CapabilityXO> capabilities = underTest.read();
    assertThat(capabilities, hasSize(1));
    assertThat(capabilities.get(0).getProperties().get("username"), is("username"));
    assertThat(capabilities.get(0).getProperties().get("password"), is(PasswordPlaceholder.get()));
    verify(capability, times(2)).isPasswordProperty(any());
  }

  @Test
  void testReadPasswordEmptyForPKI() {
    when(capabilityContext.properties()).thenReturn(
        Map.of("username", "username", "password", "its a secret to everybody", "authenticationType", "PKI"));
    List<CapabilityXO> capabilities = underTest.read();

    assertThat(capabilities, hasSize(1));
    assertThat(capabilities.get(0).getProperties().get("password"), is(""));
  }

  @Test
  void testUpdatePlacheholderRetainsCurrentPassword() {
    CapabilityXO capabilityXO = new CapabilityXO();
    capabilityXO.setId("mycap");
    capabilityXO.setProperties(Map.of("username", "username", "password", PasswordPlaceholder.get()));
    capabilityXO.setEnabled(true);

    underTest.update(capabilityXO);

    verify(capabilityRegistry).update(any(), anyBoolean(), any(), mapArgumentCaptor.capture());
    assertThat(mapArgumentCaptor.getValue().get("username"), is("username"));
    assertThat(mapArgumentCaptor.getValue().get("password"), is("its a secret to everybody"));
  }
}
