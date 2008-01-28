/*
 * Copyright 2005-2008 Noelios Consulting.
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the "License"). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.txt See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL HEADER in each file and
 * include the License file at http://www.opensource.org/licenses/cddl1.txt If
 * applicable, add the following below this CDDL HEADER, with the fields
 * enclosed by brackets "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 */

package org.restlet.test.jaxrs.services.tests;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;

import javax.ws.rs.core.SecurityContext;

import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.jaxrs.AllowAllAuthenticator;
import org.restlet.ext.jaxrs.Authenticator;
import org.restlet.ext.jaxrs.ForbidAllAuthenticator;
import org.restlet.test.jaxrs.services.SecurityContextService;

/**
 * This test class checks if the Request.evaluatePreconditions methods works
 * fine.
 * 
 * @author Stephan Koops
 */
public class SecurityContextTest extends JaxRsTestCase {
    private static final Class<SecurityContextService> SEC_CONT_SERV = SecurityContextService.class;

    @Override
    @SuppressWarnings("unchecked")
    protected Collection createRootResourceColl() {
        return Collections.singleton(SEC_CONT_SERV);
    }

    /**
     * @param authenticator
     * @return true, if it could be set, or false if not.
     */
    private boolean setAuthroizator(Authenticator authenticator) {
        ServerWrapper serverWrapper = getServerWrapper();
        if (serverWrapper instanceof RestletServerWrapper) {
            RestletServerWrapper restletServerWrapper = ((RestletServerWrapper) serverWrapper);
            restletServerWrapper.setAuthorizator(authenticator);
            return true;
        }
        return false;
    }

    @Override
    protected boolean shouldStartServerInSetUp() {
        return false;
    }

    /**
     * @param authenticator
     * @throws Exception
     */
    private boolean startServer(Authenticator authenticator) throws Exception {
        if (!setAuthroizator(authenticator))
            return false;
        startServer();
        return true;
    }

    /**
     * Allow access, but forbid all rules
     * 
     * @throws Exception
     */
    public void test2() throws Exception {
        if (!startServer(new Authenticator() {
            public boolean checkSecret(String identifier, char[] secret) {
                return true;
            }

            public boolean isUserInRole(Principal principal, String role) {
                return false;
            }
        }))
            return;
        Response response;
        response = accessServer(SEC_CONT_SERV, Method.GET);
        assertEquals(Status.CLIENT_ERROR_FORBIDDEN, response.getStatus());

        response = accessServer(SEC_CONT_SERV, Method.POST);
        assertEquals(Status.CLIENT_ERROR_FORBIDDEN, response.getStatus());
    }

    public void testAllowAll() throws Exception {
        if (!startServer(AllowAllAuthenticator.getInstance()))
            return;
        Response response;
        response = accessServer(SEC_CONT_SERV, Method.GET);
        assertEquals(Status.SUCCESS_OK, response.getStatus());

        response = accessServer(SEC_CONT_SERV, Method.POST);
        assertEquals(Status.SUCCESS_CREATED, response.getStatus());
        Reference expecretLocation = createReference(SEC_CONT_SERV, null);
        assertTrue("The location must start with " + expecretLocation
                + "; it is " + response.getLocationRef(), response
                .getLocationRef().toString().startsWith(
                        expecretLocation.toString()));
    }

    public void testAuthenticationSchemeBasic() throws Exception {
        if (!startServer(AllowAllAuthenticator.getInstance()))
            return;
        ChallengeResponse cr = new ChallengeResponse(
                ChallengeScheme.HTTP_BASIC, "u", "p");
        Response response;
        response = accessServer(SEC_CONT_SERV, "authenticationScheme",
                Method.GET, null, cr);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        String entity = response.getEntity().getText();
        assertEquals(SecurityContext.BASIC_AUTH, entity);
    }

    public void _testAuthenticationSchemeDigest() throws Exception {
        if (!setAuthroizator(AllowAllAuthenticator.getInstance()))
            return;
        startServer(ChallengeScheme.HTTP_DIGEST);
        ChallengeResponse cr = new ChallengeResponse(
                ChallengeScheme.HTTP_DIGEST, "u", "p");
        Response response;
        response = accessServer(SEC_CONT_SERV, "authenticationScheme",
                Method.GET, null, cr);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        String entity = response.getEntity().getText();
        assertEquals(SecurityContext.DIGEST_AUTH, entity);
    }

    public void testForbidAll() throws Exception {
        if (!startServer(ForbidAllAuthenticator.getInstance()))
            return;
        Response response;
        response = accessServer(SEC_CONT_SERV, Method.GET);
        assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED, response.getStatus());

        response = accessServer(SEC_CONT_SERV, Method.POST);
        assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED, response.getStatus());
    }

    public void testSecure() throws Exception {
        if (!startServer(AllowAllAuthenticator.getInstance()))
            return;
        Response response;
        response = accessServer(SEC_CONT_SERV, "secure", Method.GET);
        assertEquals(Status.CLIENT_ERROR_NOT_FOUND, response.getStatus());
    }
    // hope that Restlet Request.isConfidential(); works right with HTTPS

    /**
     * @throws Exception
     */
    public void testNoRoles() throws Exception {
        Authenticator exampleAuthorizator = new Authenticator() {
            /**
             * @returns true, if the first char of the password is 'a'
             * @see Authenticator#checkSecret(String, char[])
             */
            public boolean checkSecret(String identifier, char[] secret) {
                if (secret[0] == 'a')
                    return true;
                return false;
            }

            /**
             * @return true, if the role name and the username starts with the
             *         same char.
             * @see Authenticator#isUserInRole(String)
             */
            public boolean isUserInRole(Principal principal, String role) {
                if (principal == null)
                    throw new IllegalArgumentException("No principal given");
                if (role == null)
                    throw new IllegalArgumentException("No role given");
                if (role.charAt(0) == principal.getName().charAt(0))
                    return true;
                return false;
            }
        };
        if (!startServer(exampleAuthorizator))
            return;
        ChallengeResponse challengeResponse = new ChallengeResponse(
                ChallengeScheme.HTTP_BASIC, "fsdf", "xyz");
        Response response = accessServer(SEC_CONT_SERV, null, Method.GET, null,
                challengeResponse);
        assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED, response.getStatus());

        challengeResponse = new ChallengeResponse(ChallengeScheme.HTTP_BASIC,
                "fsdf", "baj");
        response = accessServer(SEC_CONT_SERV, null, Method.GET, null,
                challengeResponse);
        assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED, response.getStatus());

        challengeResponse = new ChallengeResponse(ChallengeScheme.HTTP_BASIC,
                "fsdf", "abj");
        response = accessServer(SEC_CONT_SERV, null, Method.GET, null,
                challengeResponse);
        assertEquals(Status.CLIENT_ERROR_FORBIDDEN, response.getStatus());

        challengeResponse = new ChallengeResponse(ChallengeScheme.HTTP_BASIC,
                "bsdf", "abaj");
        response = accessServer(SEC_CONT_SERV, null, Method.GET, null,
                challengeResponse);
        assertEquals(Status.SUCCESS_OK, response.getStatus());

        challengeResponse = new ChallengeResponse(ChallengeScheme.HTTP_BASIC,
                "fsdf", "axa2");
        response = accessServer(SEC_CONT_SERV, null, Method.POST, null,
                challengeResponse);
        assertEquals(Status.SUCCESS_CREATED, response.getStatus());

        response = accessServer(SEC_CONT_SERV, null, Method.PUT, null,
                challengeResponse);
        assertEquals(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED, response
                .getStatus());
    }

    public void testUserPrincipal() throws Exception {
        if (!startServer(AllowAllAuthenticator.getInstance()))
            return;
        Response response;
        response = accessServer(SEC_CONT_SERV, "userPrincipal", Method.GET);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        String entity = response.getEntity().getText();
        assertEquals("", entity);

        response = accessServer(SEC_CONT_SERV, "userPrincipal", Method.GET,
                null, new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "abc",
                        "def"));
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        entity = response.getEntity().getText();
        assertEquals("abc", entity);

        response = accessServer(SEC_CONT_SERV, "userPrincipal", Method.GET,
                null, new ChallengeResponse(ChallengeScheme.HTTP_BASIC,
                        "asdfsdfbc", "def"));
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        entity = response.getEntity().getText();
        assertEquals("asdfsdfbc", entity);
    }
}