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
package org.sonatype.nexus.bootstrap.entrypoint;

import org.sonatype.nexus.common.conversion.StringToByteSizeConverter;
import org.sonatype.nexus.common.conversion.StringToDurationConverter;
import org.sonatype.nexus.common.conversion.StringToTimeConverter;
import org.sonatype.nexus.common.conversion.StringToUriConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

@Configuration
public class CommonConfiguration
{
  @Bean
  public ConversionService converterService() {
    DefaultConversionService conversionService = new DefaultConversionService();

    conversionService.addConverter(new StringToByteSizeConverter());
    conversionService.addConverter(new StringToDurationConverter());
    conversionService.addConverter(new StringToTimeConverter());
    conversionService.addConverter(new StringToUriConverter());

    return conversionService;
  }
}
