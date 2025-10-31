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
package org.sonatype.nexus.internal.web;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.common.template.TemplateThrowableAdapter;
import org.sonatype.nexus.servlet.ServletHelper;
import org.sonatype.nexus.servlet.XFrameOptions;

import jakarta.annotation.Nullable;
import org.apache.shiro.web.servlet.ShiroHttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

/**
 * Service to write a rendered HTML page
 */
@Component
public class ErrorPageService
    extends ComponentSupport
{
  private static final String TEMPLATE_RESOURCE = "errorPageHtml.vm";

  private final TemplateHelper templateHelper;

  private final XFrameOptions xFrameOptions;

  private final URL template;

  @Autowired
  public ErrorPageService(final TemplateHelper templateHelper, final XFrameOptions xFrameOptions) {
    this.templateHelper = checkNotNull(templateHelper);
    this.xFrameOptions = checkNotNull(xFrameOptions);

    template = getClass().getResource(TEMPLATE_RESOURCE);
    checkNotNull(template);
  }

  /**
   * Write an HTML error page to the provided {@link HttpServletResponse}
   *
   * @param errorInfo a description of the error
   * @param request the inbound request
   * @param response the response to write to
   *
   * @throws IOException when a failure occurs writing to the response
   */
  public void writeErrorResponse(
      final ErrorInfo errorInfo,
      final HttpServletRequest request,
      final HttpServletResponse response) throws IOException
  {
    log.debug("Writing error response for {}", errorInfo);

    writeHeaders(request, response);

    Integer errorCode = errorInfo.code;
    String errorMessage = errorInfo.message;

    // this happens if someone browses directly to the error page
    if (errorCode == null) {
      errorCode = SC_NOT_FOUND;
      errorMessage = "Not found";
    }

    // maintain custom status message when (re)setting the status code,
    // we can't use sendError because it doesn't allow custom html body
    if (errorMessage == null) {
      response.setStatus(errorCode);
      errorMessage = "Unknown error";
    }
    else {
      response.setStatus(errorCode, errorMessage);
      ServletResponse resp = response;
      if (response instanceof ShiroHttpServletResponse) {
        resp = ((ShiroHttpServletResponse) response).getResponse();
        while (resp instanceof HttpServletResponseWrapper) {
          resp = ((HttpServletResponseWrapper) resp).getResponse();
        }
      }
      if (resp instanceof org.eclipse.jetty.ee8.nested.Response) {
        ((org.eclipse.jetty.ee8.nested.Response) resp).setStatusWithReason(errorCode, errorMessage);
      }
    }

    writeResponse(errorCode, errorMessage, errorInfo.cause, request, response);
  }

  private void writeHeaders(final HttpServletRequest request, final HttpServletResponse response) {
    ServletHelper.addNoCacheResponseHeaders(response);
    response.setHeader(X_FRAME_OPTIONS, xFrameOptions.getValueForPath(request.getPathInfo()));
    response.setContentType("text/html");
  }

  private void writeResponse(
      final Integer errorCode,
      final String errorMessage,
      @Nullable final Throwable cause,
      final HttpServletRequest request,
      final HttpServletResponse response) throws IOException
  {
    TemplateParameters params = templateHelper.parameters();
    params.set("errorCode", errorCode);
    params.set("errorName", Status.fromStatusCode(errorCode).getReasonPhrase());
    params.set("errorDescription", errorMessage);

    // add cause if ?debug enabled and there is an exception
    if (cause != null && ServletHelper.isDebug(request)) {
      params.set("errorCause", new TemplateThrowableAdapter(cause));
    }

    String html = templateHelper.render(template, params);
    try (PrintWriter out = new PrintWriter(new OutputStreamWriter(response.getOutputStream()))) {
      out.println(html);
    }
  }

  /**
   * @param code the status code
   * @param message a description of the problem to be rendered to the user
   * @param cause an optional cause, will only be rendered when debug is enabled and allowed
   */
  public static record ErrorInfo(Integer code, String message, @Nullable Throwable cause)
  {
    public ErrorInfo(final Integer code, final String message) {
      this(code, message, null);
    }
  }
}
