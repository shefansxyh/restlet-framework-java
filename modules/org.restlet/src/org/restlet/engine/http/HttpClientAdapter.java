/**
 * Copyright 2005-2009 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.engine.http;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.logging.Level;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Uniform;
import org.restlet.data.AuthenticationInfo;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ClientInfo;
import org.restlet.data.Conditions;
import org.restlet.data.Dimension;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.data.Warning;
import org.restlet.engine.Edition;
import org.restlet.engine.Engine;
import org.restlet.engine.util.DateUtils;
import org.restlet.util.Series;

/**
 * Converter of high-level uniform calls into low-level HTTP client calls.
 * 
 * @author Jerome Louvel
 */
public class HttpClientAdapter extends HttpAdapter {
    /**
     * Copies headers into a response.
     * 
     * @param headers
     *            The headers to copy.
     * @param response
     *            The response to update.
     */
    public static void copyResponseTransportHeaders(Series<Parameter> headers,
            Response response) {
        // Read info from headers
        for (Parameter header : headers) {
            if (header.getName()
                    .equalsIgnoreCase(HttpConstants.HEADER_LOCATION)) {
                response.setLocationRef(header.getValue());
            } else if (header.getName().equalsIgnoreCase(
                    HttpConstants.HEADER_AGE)) {
                try {
                    response.setAge(Integer.parseInt(header.getValue()));
                } catch (NumberFormatException nfe) {
                    Context.getCurrentLogger().log(
                            Level.WARNING,
                            "Error during Age header parsing. Header: "
                                    + header.getValue(), nfe);
                }
            } else if (header.getName().equalsIgnoreCase(
                    HttpConstants.HEADER_DATE)) {
                Date date = DateUtils.parse(header.getValue());

                if (date == null) {
                    date = new Date();
                }

                response.setDate(date);
            } else if (header.getName().equalsIgnoreCase(
                    HttpConstants.HEADER_RETRY_AFTER)) {
                // [ifndef gwt]
                Date retryAfter = DateUtils.parse(header.getValue());

                if (retryAfter == null) {
                    // The date might be expressed as a number of seconds
                    try {
                        int retryAfterSecs = Integer
                                .parseInt(header.getValue());
                        java.util.Calendar calendar = java.util.Calendar
                                .getInstance();
                        calendar.add(java.util.Calendar.SECOND, retryAfterSecs);
                        retryAfter = calendar.getTime();
                    } catch (NumberFormatException nfe) {
                        Context.getCurrentLogger().log(
                                Level.WARNING,
                                "Error during Retry-After header parsing. Header: "
                                        + header.getValue(), nfe);
                    }
                }

                response.setRetryAfter(retryAfter);
                // [enddef]
            } else if ((header.getName()
                    .equalsIgnoreCase(HttpConstants.HEADER_SET_COOKIE))
                    || (header.getName()
                            .equalsIgnoreCase(HttpConstants.HEADER_SET_COOKIE2))) {
                try {
                    CookieReader cr = new CookieReader(header.getValue());
                    response.getCookieSettings().add(cr.readCookieSetting());
                } catch (Exception e) {
                    Context.getCurrentLogger().log(
                            Level.WARNING,
                            "Error during cookie setting parsing. Header: "
                                    + header.getValue(), e);
                }
            } else if (header.getName().equalsIgnoreCase(
                    HttpConstants.HEADER_WWW_AUTHENTICATE)) {
                // [ifndef gwt]
                ChallengeRequest request = org.restlet.engine.security.AuthenticatorUtils
                        .parseRequest(response, header.getValue(), headers);
                response.getChallengeRequests().add(request);
                // [enddef]
            } else if (header.getName().equalsIgnoreCase(
                    HttpConstants.HEADER_PROXY_AUTHENTICATE)) {
                // [ifndef gwt]
                ChallengeRequest request = org.restlet.engine.security.AuthenticatorUtils
                        .parseRequest(response, header.getValue(), headers);
                response.getProxyChallengeRequests().add(request);
                // [enddef]
            } else if (header.getName().equalsIgnoreCase(
                    HttpConstants.HEADER_AUTHENTICATION_INFO)) {
                // [ifndef gwt]
                AuthenticationInfo authenticationInfo = org.restlet.engine.security.AuthenticatorUtils
                        .parseAuthenticationInfo(header.getValue());
                response.setAuthenticationInfo(authenticationInfo);
                // [enddef]
            } else if (header.getName().equalsIgnoreCase(
                    HttpConstants.HEADER_SERVER)) {
                response.getServerInfo().setAgent(header.getValue());
            } else if (header.getName().equalsIgnoreCase(
                    HttpConstants.HEADER_ALLOW)) {
                HeaderReader hr = new HeaderReader(header.getValue());
                String value = hr.readValue();
                Set<Method> allowedMethods = response.getAllowedMethods();

                while (value != null) {
                    allowedMethods.add(Method.valueOf(value));
                    value = hr.readValue();
                }
            } else if (header.getName().equalsIgnoreCase(
                    HttpConstants.HEADER_VARY)) {
                HeaderReader hr = new HeaderReader(header.getValue());
                String value = hr.readValue();
                Set<Dimension> dimensions = response.getDimensions();

                while (value != null) {
                    if (value.equalsIgnoreCase(HttpConstants.HEADER_ACCEPT)) {
                        dimensions.add(Dimension.MEDIA_TYPE);
                    } else if (value
                            .equalsIgnoreCase(HttpConstants.HEADER_ACCEPT_CHARSET)) {
                        dimensions.add(Dimension.CHARACTER_SET);
                    } else if (value
                            .equalsIgnoreCase(HttpConstants.HEADER_ACCEPT_ENCODING)) {
                        dimensions.add(Dimension.ENCODING);
                    } else if (value
                            .equalsIgnoreCase(HttpConstants.HEADER_ACCEPT_LANGUAGE)) {
                        dimensions.add(Dimension.LANGUAGE);
                    } else if (value
                            .equalsIgnoreCase(HttpConstants.HEADER_AUTHORIZATION)) {
                        dimensions.add(Dimension.AUTHORIZATION);
                    } else if (value
                            .equalsIgnoreCase(HttpConstants.HEADER_USER_AGENT)) {
                        dimensions.add(Dimension.CLIENT_AGENT);
                    } else if (value.equals("*")) {
                        dimensions.add(Dimension.UNSPECIFIED);
                    }

                    value = hr.readValue();
                }
            } else if (header.getName().equalsIgnoreCase(
                    HttpConstants.HEADER_WARNING)) {
                WarningReader hr = new WarningReader(header.getValue());
                try {
                    response.getWarnings().add(hr.readWarning());
                } catch (Exception e) {
                    Context.getCurrentLogger().log(
                            Level.WARNING,
                            "Error during warning parsing. Header: "
                                    + header.getValue(), e);
                }
            } else if (header.getName().equalsIgnoreCase(
                    HttpConstants.HEADER_CACHE_CONTROL)) {
                CacheControlReader ccr = new CacheControlReader(header
                        .getValue());
                try {
                    response.getCacheDirectives().addAll(ccr.readDirectives());
                } catch (Exception e) {
                    Context.getCurrentLogger().log(
                            Level.WARNING,
                            "Error during cache control parsing. Header: "
                                    + header.getValue(), e);
                }
            }
        }
    }

    /**
     * Constructor.
     * 
     * @param context
     *            The context to use.
     */
    public HttpClientAdapter(Context context) {
        super(context);
    }

    /**
     * Adds the request headers of a uniform call to a HTTP client call.
     * 
     * @param httpCall
     *            The HTTP client call.
     * @param request
     *            The high-level request.
     */
    @SuppressWarnings("unchecked")
    protected void addRequestHeaders(HttpClientCall httpCall, Request request) {
        if (httpCall != null) {
            Series<Parameter> requestHeaders = httpCall.getRequestHeaders();

            // Manually add the host name and port when it is potentially
            // different from the one specified in the target resource
            // reference.
            Reference hostRef = (request.getResourceRef().getBaseRef() != null) ? request
                    .getResourceRef().getBaseRef()
                    : request.getResourceRef();

            if (hostRef.getHostDomain() != null) {
                String host = hostRef.getHostDomain();
                int hostRefPortValue = hostRef.getHostPort();

                if ((hostRefPortValue != -1)
                        && (hostRefPortValue != request.getProtocol()
                                .getDefaultPort())) {
                    host = host + ':' + hostRefPortValue;
                }

                requestHeaders.add(HttpConstants.HEADER_HOST, host);
            }

            // Add the date
            request.setDate(new Date());
            requestHeaders.add(HttpConstants.HEADER_DATE, DateUtils
                    .format(request.getDate()));

            // Add the user agent header
            if (request.getClientInfo().getAgent() != null) {
                requestHeaders.add(HttpConstants.HEADER_USER_AGENT, request
                        .getClientInfo().getAgent());
            } else {
                requestHeaders.add(HttpConstants.HEADER_USER_AGENT,
                        Engine.VERSION_HEADER);
            }

            // Add the from header
            if (request.getClientInfo().getFrom() != null) {
                requestHeaders.add(HttpConstants.HEADER_FROM, request
                        .getClientInfo().getFrom());
            }

            // Add the conditions
            Conditions condition = request.getConditions();
            if (!condition.getMatch().isEmpty()) {
                StringBuilder value = new StringBuilder();

                for (int i = 0; i < condition.getMatch().size(); i++) {
                    if (i > 0) {
                        value.append(", ");
                    }
                    value.append(condition.getMatch().get(i).format());
                }

                httpCall.getRequestHeaders().add(HttpConstants.HEADER_IF_MATCH,
                        value.toString());
            }

            if (condition.getModifiedSince() != null) {
                String imsDate = DateUtils.format(condition.getModifiedSince());
                requestHeaders.add(HttpConstants.HEADER_IF_MODIFIED_SINCE,
                        imsDate);
            }

            if (!condition.getNoneMatch().isEmpty()) {
                StringBuilder value = new StringBuilder();

                for (int i = 0; i < condition.getNoneMatch().size(); i++) {
                    if (i > 0) {
                        value.append(", ");
                    }
                    value.append(condition.getNoneMatch().get(i).format());
                }

                requestHeaders.add(HttpConstants.HEADER_IF_NONE_MATCH, value
                        .toString());
            }

            if (condition.getRangeTag() != null
                    && condition.getRangeDate() != null) {
                getLogger()
                        .log(
                                Level.WARNING,
                                "Unable to format the HTTP If-Range header due to the presence of both entity tag and modification date.");
            } else {
                if (condition.getRangeTag() != null) {
                    requestHeaders.add(HttpConstants.HEADER_IF_RANGE, condition
                            .getRangeTag().format());
                } else if (condition.getRangeDate() != null) {
                    String rDate = DateUtils.format(condition.getRangeDate(),
                            DateUtils.FORMAT_RFC_1123.get(0));
                    requestHeaders.add(HttpConstants.HEADER_IF_RANGE, rDate);
                }
            }

            if (condition.getUnmodifiedSince() != null) {
                String iusDate = DateUtils
                        .format(condition.getUnmodifiedSince(),
                                DateUtils.FORMAT_RFC_1123.get(0));
                requestHeaders.add(HttpConstants.HEADER_IF_UNMODIFIED_SINCE,
                        iusDate);
            }

            // Add the cookies
            if (request.getCookies().size() > 0) {
                String cookies = CookieUtils.format(request.getCookies());
                requestHeaders.add(HttpConstants.HEADER_COOKIE, cookies);
            }

            // Add the referrer header
            if (request.getReferrerRef() != null) {
                requestHeaders.add(HttpConstants.HEADER_REFERRER, request
                        .getReferrerRef().toString());
            }

            // Add the preferences
            ClientInfo client = request.getClientInfo();
            if (client.getAcceptedMediaTypes().size() > 0) {
                try {
                    requestHeaders.add(HttpConstants.HEADER_ACCEPT,
                            PreferenceUtils.format(client
                                    .getAcceptedMediaTypes()));
                } catch (IOException ioe) {
                    getLogger().log(Level.WARNING,
                            "Unable to format the HTTP Accept header", ioe);
                }
            } else {
                requestHeaders.add(HttpConstants.HEADER_ACCEPT, MediaType.ALL
                        .getName());
            }

            if (client.getAcceptedCharacterSets().size() > 0) {
                try {
                    requestHeaders.add(HttpConstants.HEADER_ACCEPT_CHARSET,
                            PreferenceUtils.format(client
                                    .getAcceptedCharacterSets()));
                } catch (IOException ioe) {
                    getLogger().log(Level.WARNING,
                            "Unable to format the HTTP Accept header", ioe);
                }
            }

            if (client.getAcceptedEncodings().size() > 0) {
                try {
                    requestHeaders.add(HttpConstants.HEADER_ACCEPT_ENCODING,
                            PreferenceUtils.format(client
                                    .getAcceptedEncodings()));
                } catch (IOException ioe) {
                    getLogger().log(Level.WARNING,
                            "Unable to format the HTTP Accept header", ioe);
                }
            }

            if (client.getAcceptedLanguages().size() > 0) {
                try {
                    requestHeaders.add(HttpConstants.HEADER_ACCEPT_LANGUAGE,
                            PreferenceUtils.format(client
                                    .getAcceptedLanguages()));
                } catch (IOException ioe) {
                    getLogger().log(Level.WARNING,
                            "Unable to format the HTTP Accept header", ioe);
                }
            }

            // [ifndef gwt]
            // Add Range header
            if (!request.getRanges().isEmpty()) {
                requestHeaders.add(HttpConstants.HEADER_RANGE,
                        org.restlet.engine.util.RangeUtils.formatRanges(request
                                .getRanges()));
            }
            // [enddef]

            // Add entity headers
            if (request.isEntityAvailable()) {
                if (request.getEntity().getMediaType() != null) {
                    String contentType = request.getEntity().getMediaType()
                            .toString();

                    // Specify the character set parameter if required
                    if ((request.getEntity().getMediaType().getParameters()
                            .getFirstValue("charset") == null)
                            && (request.getEntity().getCharacterSet() != null)) {
                        contentType = contentType
                                + "; charset="
                                + request.getEntity().getCharacterSet()
                                        .getName();
                    }

                    requestHeaders.add(HttpConstants.HEADER_CONTENT_TYPE,
                            contentType);
                }

                if (!request.getEntity().getEncodings().isEmpty()) {
                    final StringBuilder value = new StringBuilder();
                    for (int i = 0; i < request.getEntity().getEncodings()
                            .size(); i++) {
                        if (i > 0) {
                            value.append(", ");
                        }
                        value.append(request.getEntity().getEncodings().get(i)
                                .getName());
                    }
                    requestHeaders.add(HttpConstants.HEADER_CONTENT_ENCODING,
                            value.toString());
                }

                if (!request.getEntity().getLanguages().isEmpty()) {
                    final StringBuilder value = new StringBuilder();
                    for (int i = 0; i < request.getEntity().getLanguages()
                            .size(); i++) {
                        if (i > 0) {
                            value.append(", ");
                        }
                        value.append(request.getEntity().getLanguages().get(i)
                                .getName());
                    }
                    requestHeaders.add(HttpConstants.HEADER_CONTENT_LANGUAGE,
                            value.toString());
                }

                if (request.getEntity().getSize() > 0) {
                    requestHeaders.add(HttpConstants.HEADER_CONTENT_LENGTH,
                            String.valueOf(request.getEntity().getSize()));
                }
                // [ifndef gwt]
                if (request.getEntity().getRange() != null) {
                    try {
                        requestHeaders.add(HttpConstants.HEADER_CONTENT_RANGE,
                                org.restlet.engine.util.RangeUtils
                                        .formatContentRange(request.getEntity()
                                                .getRange(), request
                                                .getEntity().getSize()));
                    } catch (Exception e) {
                        getLogger()
                                .log(
                                        Level.WARNING,
                                        "Unable to format the HTTP Content-Range header",
                                        e);
                    }
                }
                // [enddef]

                // [ifndef gwt]
                // Add Checksum
                if (request.getEntity().getDigest() != null
                        && org.restlet.data.Digest.ALGORITHM_MD5.equals(request
                                .getEntity().getDigest().getAlgorithm())) {
                    requestHeaders
                            .add(HttpConstants.HEADER_CONTENT_MD5,
                                    org.restlet.engine.util.Base64.encode(
                                            request.getEntity().getDigest()
                                                    .getValue(), false));
                }
                // [enddef]
            }

            // Add user-defined extension headers
            Series<Parameter> additionalHeaders = (Series<Parameter>) request
                    .getAttributes().get(HttpConstants.ATTRIBUTE_HEADERS);
            addAdditionalHeaders(requestHeaders, additionalHeaders);

            // [ifndef gwt]
            // Add the security headers. NOTE: This must stay at the end because
            // the AWS challenge scheme requires access to all HTTP headers
            ChallengeResponse challengeResponse = request
                    .getChallengeResponse();
            if (challengeResponse != null) {
                try {
                    requestHeaders.add(HttpConstants.HEADER_AUTHORIZATION,
                            org.restlet.engine.security.AuthenticatorUtils
                                    .formatResponse(challengeResponse, request,
                                            requestHeaders));
                } catch (IOException e) {
                    Context.getCurrentLogger().log(Level.WARNING,
                            "Unable to write the Authorization header", e);
                }
            }

            ChallengeResponse proxyChallengeResponse = request
                    .getProxyChallengeResponse();
            if (proxyChallengeResponse != null) {
                try {
                    requestHeaders.add(
                            HttpConstants.HEADER_PROXY_AUTHORIZATION,
                            org.restlet.engine.security.AuthenticatorUtils
                                    .formatResponse(proxyChallengeResponse,
                                            request, requestHeaders));
                } catch (IOException e) {
                    Context
                            .getCurrentLogger()
                            .log(
                                    Level.WARNING,
                                    "Unable to write the Proxy-Authorization header",
                                    e);
                }
            }
            // [enddef]

            // Add the warning headers
            if (!request.getWarnings().isEmpty()) {
                for (Warning warning : request.getWarnings()) {
                    requestHeaders.add(HttpConstants.HEADER_WARNING,
                            WarningUtils.format(warning));
                }
            }
            // Add the Cache-control headers
            if (!request.getCacheDirectives().isEmpty()) {
                requestHeaders.add(HttpConstants.HEADER_CACHE_CONTROL,
                        CacheControlUtils.format(request.getCacheDirectives()));
            }
        }
    }

    /**
     * Commits the changes to a handled HTTP client call back into the original
     * uniform call. The default implementation first invokes the
     * "addResponseHeaders" then asks the "htppCall" to send the response back
     * to the client.
     * 
     * @param httpCall
     *            The original HTTP call.
     * @param request
     *            The high-level request.
     * @param response
     *            The high-level response.
     * @throws Exception
     */
    public void commit(final HttpClientCall httpCall, Request request,
            Response response) throws Exception {
        if (httpCall != null) {
            // Check if the call is asynchronous
            if (response.getOnReceived() != null) {
                final Uniform userCallback = response.getOnReceived();

                // Send the request to the client
                httpCall.sendRequest(request, response, new Uniform() {
                    public void handle(Request request, Response response) {
                        try {
                            updateResponse(response, new Status(httpCall
                                    .getStatusCode(), null, httpCall
                                    .getReasonPhrase(), null), httpCall);
                            userCallback.handle(request, response);
                        } catch (Exception e) {
                            // Unexpected exception occurred
                            if ((response.getStatus() == null)
                                    || !response.getStatus().isError()) {
                                response.setStatus(
                                        Status.CONNECTOR_ERROR_INTERNAL, e);
                                userCallback.handle(request, response);
                            }
                        }
                    }
                });
            } else {
                if (Edition.CURRENT == Edition.GWT) {
                    System.err
                            .println("HTTP client calls must have a callback in the GWT edition");
                } else {
                    // [ifndef gwt]
                    updateResponse(response, httpCall.sendRequest(request),
                            httpCall);
                    // [enddef]
                }
            }
        }
    }

    /**
     * Reads the response headers of a handled HTTP client call to update the
     * original uniform call.
     * 
     * @param httpCall
     *            The handled HTTP client call.
     * @param response
     *            The high-level response to update.
     */
    protected void readResponseHeaders(HttpClientCall httpCall,
            Response response) {
        try {
            Series<Parameter> responseHeaders = httpCall.getResponseHeaders();
            // Put the response headers in the call's attributes map
            response.getAttributes().put(HttpConstants.ATTRIBUTE_HEADERS,
                    responseHeaders);
            copyResponseTransportHeaders(responseHeaders, response);
        } catch (Exception e) {
            getLogger()
                    .log(
                            Level.FINE,
                            "An error occured during the processing of the HTTP response.",
                            e);
            response.setStatus(Status.CONNECTOR_ERROR_INTERNAL, e);
        }
    }

    /**
     * Converts a low-level HTTP call into a high-level uniform call.
     * 
     * @param client
     *            The HTTP client that will handle the call.
     * @param request
     *            The high-level request.
     * @return A new high-level uniform call.
     */
    public HttpClientCall toSpecific(HttpClientHelper client, Request request) {
        // Create the low-level HTTP client call
        HttpClientCall result = client.create(request);

        // Add the request headers
        addRequestHeaders(result, request);

        return result;
    }

    /**
     * Updates the response with information from the lower-level HTTP client
     * call.
     * 
     * @param response
     *            The response to update.
     * @param status
     *            The response status to apply.
     * @param httpCall
     *            The source HTTP client call.
     * @throws IOException
     */
    public void updateResponse(Response response, Status status,
            HttpClientCall httpCall) {
        // Send the request to the client
        response.setStatus(status);

        // Get the server address
        response.getServerInfo().setAddress(httpCall.getServerAddress());
        response.getServerInfo().setPort(httpCall.getServerPort());

        // Read the response headers
        readResponseHeaders(httpCall, response);

        // Set the entity
        response.setEntity(httpCall.getResponseEntity(response));
        // Release the representation's content for some obvious cases
        if (response.getEntity() != null) {
            if (response.getEntity().getSize() == 0) {
                response.getEntity().release();
            } else if (response.getRequest().getMethod().equals(Method.HEAD)) {
                response.getEntity().release();
            } else if (response.getStatus().equals(Status.SUCCESS_NO_CONTENT)) {
                response.getEntity().release();
            } else if (response.getStatus()
                    .equals(Status.SUCCESS_RESET_CONTENT)) {
                response.getEntity().release();
                response.setEntity(null);
            } else if (response.getStatus().equals(
                    Status.REDIRECTION_NOT_MODIFIED)) {
                response.getEntity().release();
            } else if (response.getStatus().isInformational()) {
                response.getEntity().release();
                response.setEntity(null);
            }
        }
    }
}
