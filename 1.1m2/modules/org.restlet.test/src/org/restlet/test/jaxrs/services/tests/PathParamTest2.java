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

import java.io.IOException;

import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.test.jaxrs.services.PathParamTestService2;

/**
 * @author Stephan Koops
 */
public class PathParamTest2 extends JaxRsTestCase {

    @Override
    protected Class<?> getRootResourceClass() {
        return PathParamTestService2.class;
    }

    public void testDecoded1() throws Exception {
        Response response = get("decoded/x");
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("x", response.getEntity().getText());

        response = get("decoded/sjkg");
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("sjkg", response.getEntity().getText());
    }

    public void testDecoded2() throws Exception {
        Response response = get("decoded/%20");
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals(" ", response.getEntity().getText());
    }

    public void testEncoded() throws Exception {
        Response response = get("encoded/x");
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("x", response.getEntity().getText());

        response = get("encoded/sjkg");
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("sjkg", response.getEntity().getText());

        response = get("encoded/%20");
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("%20", response.getEntity().getText());
    }

    public void testGetBigDecimal() throws IOException {
        Response response = get("BigDecimal/413624654744743534745767");
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("413624654744743534745767", response.getEntity().getText());

        response = get("BigDecimal/abc");
        assertTrue(response.getStatus().isError());
    }

    public void testGetInt() throws IOException {
        Response response = get("int/467");
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("467", response.getEntity().getText());

        response = get("int/abc");
        assertTrue(response.getStatus().isError());
    }

    public void testGetInteger() throws IOException {
        Response response = get("Integer/4423467");
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("4423467", response.getEntity().getText());

        response = get("Integer/423645365467345743734");
        assertTrue(response.getStatus().isError());
        assertEquals(Status.CLIENT_ERROR_NOT_FOUND, response.getStatus());

        response = get("Integer/abc");
        assertTrue(response.getStatus().isError());
        assertEquals(Status.CLIENT_ERROR_NOT_FOUND, response.getStatus());
    }

    public void testGetMediaType() throws IOException {
        Response response = get("MediaType/467");
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("467", response.getEntity().getText());

        response = get("MediaType/abc");
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("abc", response.getEntity().getText());
    }

    public void testGetMn() throws IOException {
        Response response = get("mn467");
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("467", response.getEntity().getText());

        response = get("mnabc");
        assertTrue(response.getStatus().isError());
        assertEquals(Status.CLIENT_ERROR_NOT_FOUND, response.getStatus());
    }

    public void testGetWithSlashInUriParam() throws IOException {
        Response response = get("multSegm/abc/def");
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("abc/def", response.getEntity().getText());
    }
}