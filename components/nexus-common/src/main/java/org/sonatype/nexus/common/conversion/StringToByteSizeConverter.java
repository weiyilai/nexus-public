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
package org.sonatype.nexus.common.conversion;

import java.util.Set;

import org.sonatype.goodies.common.ByteSize;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

public class StringToByteSizeConverter
    implements ConditionalGenericConverter
{
  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Set.of(new ConvertiblePair(String.class, ByteSize.class));
  }

  @Override
  public Object convert(final Object source, final TypeDescriptor sourceType, final TypeDescriptor targetType) {
    if (source == null) {
      return null;
    }
    return ByteSize.parse((String) source);
  }

  @Override
  public boolean matches(final TypeDescriptor sourceType, final TypeDescriptor targetType) {
    return sourceType.getType() == String.class && targetType.getType() == ByteSize.class;
  }
}
