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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MediatorSupportTest.TestConfig.class})
public class MediatorSupportTest
    extends TestSupport
{
  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private BarBean barBean1;

  @Autowired
  private BarBean barBean2;

  @Autowired
  private FooBean fooBean1;

  @Autowired
  private FooBean fooBean2;

  @Autowired
  private BarMediator underTest;

  @Autowired
  private FooMediator underTest2;

  @Test
  public void shouldInvokeSingletonsOnce() {
    assertThat(barBean1.equals(barBean2), is(true));
    assertThat(underTest.registered, is(List.of(barBean1)));

    underTest.on(new ContextRefreshedEvent(applicationContext));

    assertThat(underTest.registered, is(List.of(barBean1)));
  }

  @Test
  @Ignore
  public void shouldInvokePrototypesAddOnce() {
    assertThat(fooBean1.equals(fooBean2), is(false));
    assertThat(underTest2.registered.size(), is(List.of(fooBean1, fooBean2))); // FooBean is prototype so we expect 2
                                                                               // instances

    underTest.on(new ContextRefreshedEvent(applicationContext));

    assertThat(underTest.registered, is(List.of(fooBean1, fooBean2)));
  }

  private static class BarMediator
      extends MediatorSupport<BarBean>
  {
    final List<BarBean> registered = new ArrayList<>();

    public BarMediator(final Class<BarBean> clazz) {
      super(clazz);
    }

    @Override
    protected void add(final BarBean instance) {
      registered.add(instance);
    }
  }

  private static class FooMediator
      extends MediatorSupport<FooBean>
  {
    final List<FooBean> registered = new ArrayList<>();

    public FooMediator(final Class<FooBean> clazz) {
      super(clazz);
    }

    @Override
    protected void add(final FooBean instance) {
      registered.add(instance);
    }
  }

  record FooBean(String name)
  {
  }

  record BarBean(String name)
  {
  }

  @Configuration
  static class TestConfig
  {
    @Bean
    BarMediator barMediator() {
      return new BarMediator(BarBean.class);
    }

    @Bean
    FooMediator fooMediator() {
      return new FooMediator(FooBean.class);
    }

    @Bean
    @Scope(scopeName = SCOPE_PROTOTYPE)
    public FooBean fooBean() {
      return new FooBean("test" + UUID.randomUUID());
    }

    @Bean
    public BarBean barBean() {
      return new BarBean("bar " + UUID.randomUUID());
    }
  }
}
