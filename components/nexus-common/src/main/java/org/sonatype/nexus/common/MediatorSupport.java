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
package org.sonatype.nexus.common;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.sonatype.goodies.common.ComponentSupport;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class MediatorSupport<T>
    extends ComponentSupport
{
  private final Set<String> registeredBeanNames = ConcurrentHashMap.newKeySet();

  private final Class<T> clazz;

  protected MediatorSupport(final Class<T> clazz) {
    this.clazz = checkNotNull(clazz);
  }

  @EventListener
  public void on(final ContextRefreshedEvent event) {
    final Predicate<String> isNewBean = beanName -> !registeredBeanNames.contains(beanName);
    Arrays.stream(event.getApplicationContext().getBeanNamesForType(clazz))
        .filter(isNewBean)
        .forEach(beanName -> {
          // todo: what we want here is to get the newly instantiated bean
          // but event.getApplicationContext().getBean(beanName, clazz) instantiates a new one if the bean scope is
          // prototype.
          // Thus the wrong instance is passed to add below
          final T theBean = event.getApplicationContext().getBean(beanName, clazz);
          add(theBean);
          registeredBeanNames.add(beanName);
        });
  }

  protected abstract void add(T instance);
}
