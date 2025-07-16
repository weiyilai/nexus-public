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
package org.sonatype.nexus.testcommon.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * A JUnit 5 extension which helps {@link RequiresAuthentication} and {@link RequiresPermissions} annotated methods
 */
public class AuthenticationExtension
    implements Extension, BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback
{
  @Target({ElementType.TYPE, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface WithUser
  {
    /**
     * The name of the logged in user
     */
    String value() default "admin";

    /**
     * When no values are specified we treat this as all nexus permissions
     */
    String[] permissions() default {"nexus:*"};
  }

  private final SecurityManager manager = mock(SecurityManager.class);

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {
    ThreadContext.bind(manager);

    context.getTestClass()
        .flatMap(clazz -> AnnotationSupport.findAnnotation(clazz, WithUser.class))
        .ifPresent(this::withUser);
  }

  @Override
  public void afterAll(final ExtensionContext context) throws Exception {
    context.getTestClass()
        .flatMap(clazz -> AnnotationSupport.findAnnotation(clazz, WithUser.class))
        .ifPresent(this::unsetUser);

    ThreadContext.unbindSecurityManager();
  }

  @Override
  public void afterEach(final ExtensionContext context) throws Exception {
    unsetUser(null);
  }

  @Override
  public void beforeEach(final ExtensionContext context) throws Exception {
    context.getTestClass()
        .flatMap(clazz -> AnnotationSupport.findAnnotation(clazz, WithUser.class))
        .ifPresent(this::withUser);
    context.getTestMethod()
        .flatMap(clazz -> AnnotationSupport.findAnnotation(clazz, WithUser.class))
        .ifPresent(this::withUser);
  }

  private void withUser(final WithUser withUser) {
    SimplePrincipalCollection principals = new SimplePrincipalCollection(withUser.value(), "default");
    DelegatingSubject subject = new DelegatingSubject(principals, true, "localhost", mock(Session.class), manager);
    ThreadContext.bind(subject);

    Set<WildcardPermission> assignedPermissions = Stream.of(withUser.permissions())
        .map(WildcardPermission::new)
        .collect(Collectors.toSet());

    doAnswer(answer(assignedPermissions)).when(manager).checkPermission(any(), anyString());
    doAnswer(answer(assignedPermissions)).when(manager).checkPermissions(any(), (String[]) any());
  }

  private void unsetUser(final WithUser withUser) {
    ThreadContext.unbindSubject();
  }

  private static Answer<?> answer(final Set<WildcardPermission> assignedPermissions) {
    return i -> {
      for (int x = 1; x < i.getArguments().length; x++) {
        String permName = i.getArgument(x, String.class);
        WildcardPermission requested = new WildcardPermission(permName);
        if (!assignedPermissions.stream().anyMatch(candidate -> candidate.implies(requested))) {
          throw new AuthorizationException();
        }
      }
      return null;
    };
  }
}
