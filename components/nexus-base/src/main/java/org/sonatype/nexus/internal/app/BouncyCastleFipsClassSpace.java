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
package org.sonatype.nexus.internal.app;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.eclipse.sisu.inject.DeferredClass;
import org.eclipse.sisu.inject.DeferredProvider;
import org.eclipse.sisu.space.ClassSpace;

@Named("bouncycastle-fips")
@Singleton
public class BouncyCastleFipsClassSpace
    implements ClassSpace
{
  private final URLClassLoader loader;

  @Inject
  public BouncyCastleFipsClassSpace() {
    URL fipsJarUrl = BouncyCastleFipsProvider.class.getProtectionDomain().getCodeSource().getLocation();
    this.loader = new URLClassLoader(new URL[]{fipsJarUrl}, null)
    {
      @Override
      public void close() {
        // Do not close the classloader
      }
    };
  }

  @Override
  public Class<?> loadClass(String name) throws TypeNotPresentException {
    try {
      return loader.loadClass(name);
    }
    catch (ClassNotFoundException e) {
      throw new TypeNotPresentException(name, e);
    }
  }

  @Override
  public DeferredClass<?> deferLoadClass(String name) {
    return new DeferredClass<>()
    {
      @Override
      public Class<Object> load() {
        try {
          @SuppressWarnings("unchecked")
          Class<Object> clazz = (Class<Object>) loader.loadClass(name);
          return clazz;
        }
        catch (ClassNotFoundException e) {
          throw new TypeNotPresentException(name, e);
        }
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public DeferredProvider<Object> asProvider() {
        return null;
      }
    };
  }

  @Override
  public URL getResource(String name) {
    return loader.getResource(name);
  }

  @Override
  public Enumeration<URL> getResources(String name) {
    try {
      return loader.getResources(name);
    }
    catch (IOException e) {
      return Collections.emptyEnumeration();
    }
  }

  @Override
  public Enumeration<URL> findEntries(String path, String pattern, boolean recurse) {
    // Simple implementation - could be enhanced if needed
    List<URL> results = new ArrayList<>();
    try {
      Enumeration<URL> resources = loader.getResources(path);
      while (resources.hasMoreElements()) {
        URL url = resources.nextElement();
        if (url.getFile().matches(pattern)) {
          results.add(url);
        }
      }
    }
    catch (IOException e) {
      // ignore
    }
    return Collections.enumeration(results);
  }
}
