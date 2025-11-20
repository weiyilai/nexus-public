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
package org.sonatype.nexus.bootstrap.entrypoint.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.sonatype.goodies.testsupport.Test5Support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class NexusPropertiesTest
    extends Test5Support
{
  private static final String ANALYTICS_PROPERTY = "nexus.analytics.enabled";

  private static final String TRUE = "true";

  @BeforeEach
  void setUp() {
    // Clear the analytics system property before each test
    System.clearProperty(ANALYTICS_PROPERTY);
  }

  @AfterEach
  void tearDown() {
    // Clean up after tests
    System.clearProperty(ANALYTICS_PROPERTY);
  }

  @Test
  void enforceCommunityEditionAnalytics_SetsPropertyToTrue() {
    NexusProperties nexusProperties = createNexusProperties();

    nexusProperties.enforceCommunityEditionAnalytics();

    assertThat(nexusProperties.getProperty(ANALYTICS_PROPERTY), is(TRUE));
  }

  @Test
  void enforceCommunityEditionAnalytics_SetsSystemPropertyToTrue() {
    NexusProperties nexusProperties = createNexusProperties();

    nexusProperties.enforceCommunityEditionAnalytics();

    assertThat(System.getProperty(ANALYTICS_PROPERTY), is(TRUE));
  }

  @Test
  void enforceCommunityEditionAnalytics_OverridesExistingFalseValue() {
    // Set the system property to false initially
    System.setProperty(ANALYTICS_PROPERTY, "false");

    NexusProperties nexusProperties = createNexusProperties();

    nexusProperties.enforceCommunityEditionAnalytics();

    // Both the NexusProperties and System property should now be true
    assertThat(nexusProperties.getProperty(ANALYTICS_PROPERTY), is(TRUE));
    assertThat(System.getProperty(ANALYTICS_PROPERTY), is(TRUE));
  }

  /**
   * Helper method to create a NexusProperties instance with minimal setup.
   * Note: This will trigger the full initialization, but for this specific test
   * we're only interested in testing the enforceCommunityEditionAnalytics method.
   */
  private NexusProperties createNexusProperties() {
    try {
      // Create temp directories for basedir and datadir
      File basedir = util.createTempDir();
      File datadir = util.createTempDir();

      // Set required system properties for NexusProperties initialization
      System.setProperty(NexusDirectoryConfiguration.BASEDIR_SYS_PROP, basedir.getAbsolutePath());
      System.setProperty(NexusDirectoryConfiguration.DATADIR_SYS_PROP, datadir.getAbsolutePath());

      // Create the etc directory in basedir and the required nexus-default.properties file
      File etcDir = new File(basedir, "etc");
      etcDir.mkdirs();
      File defaultPropertiesFile = new File(etcDir, "nexus-default.properties");
      // Create an empty properties file - NexusProperties just needs it to exist
      Files.write(defaultPropertiesFile.toPath(), "# Default properties for test\n".getBytes());

      return new NexusProperties();
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to set up test environment", e);
    }
  }
}
