/**
 * Copyright 2005-2008 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of the following open
 * source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 (the "Licenses"). You can
 * select the license that you prefer but you may not use this file except in
 * compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.gnu.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.sun.com/cddl/cddl.html
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royaltee free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package com.noelios.restlet.component;

import java.util.Iterator;

import org.restlet.Component;
import org.restlet.VirtualHost;
import org.restlet.data.LocalReference;
import org.restlet.data.Protocol;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.noelios.restlet.TemplateDispatcher;

/**
 * Component client dispatcher.
 * 
 * Concurrency note: instances of this class or its subclasses can be invoked by
 * several threads at the same time and therefore must be thread-safe. You
 * should be especially careful when storing state as member variables.
 * 
 * @author Jerome Louvel
 */
public class ComponentClientDispatcher extends TemplateDispatcher {
    /**
     * Constructor.
     * 
     * @param componentContext
     *            The component context.
     */
    public ComponentClientDispatcher(ComponentContext componentContext) {
        super(componentContext);
    }

    @Override
    protected void doHandle(Request request, Response response) {
        super.doHandle(request, response);
        final Protocol protocol = request.getProtocol();

        if (protocol.equals(Protocol.RIAP)) {
            // Consider that the request is confidential
            request.setConfidential(true);

            // Let's dispatch it
            final LocalReference cr = new LocalReference(request
                    .getResourceRef());
            final Component component = getComponent();

            if (component != null) {
                if (cr.getRiapAuthorityType() == LocalReference.RIAP_COMPONENT) {
                    // This causes the baseRef of the resource reference to be
                    // set as if it had actually arrived from a server
                    // connector.
                    request.getResourceRef().setBaseRef(
                            request.getResourceRef().getHostIdentifier());

                    // Ask the private internal route to handle the call
                    component.getInternalRouter().handle(request, response);
                } else if (cr.getRiapAuthorityType() == LocalReference.RIAP_HOST) {
                    VirtualHost host = null;
                    VirtualHost currentHost = null;
                    final Integer hostHashCode = VirtualHost.getCurrent();

                    // Lookup the virtual host
                    for (final Iterator<VirtualHost> hostIter = getComponent()
                            .getHosts().iterator(); (host == null)
                            && hostIter.hasNext();) {
                        currentHost = hostIter.next();

                        if (currentHost.hashCode() == hostHashCode) {
                            host = currentHost;
                        }
                    }

                    if ((host == null) && (component.getDefaultHost() != null)) {
                        if (component.getDefaultHost().hashCode() == hostHashCode) {
                            host = component.getDefaultHost();
                        }
                    }

                    if (host != null) {
                        // This causes the baseRef of the resource reference to
                        // be set as if it had actually arrived from a server
                        // connector.
                        request.getResourceRef().setBaseRef(
                                request.getResourceRef().getHostIdentifier());

                        // Ask the virtual host to handle the call
                        host.handle(request, response);
                    } else {
                        getLogger()
                                .warning(
                                        "No virtual host is available to route the RIAP Host request.");
                    }
                } else {
                    getLogger()
                            .warning(
                                    "Unknown RIAP authority. Only \"component\" is supported.");
                }
            } else {
                getLogger().warning(
                        "No compoent is available to route the RIAP request.");
            }
        } else {
            getComponentContext().getComponentHelper().getClientRouter()
                    .handle(request, response);
        }
    }

    /**
     * Returns the parent component.
     * 
     * @return The parent component.
     */
    private Component getComponent() {
        Component result = null;

        if ((getComponentContext() != null)
                && (getComponentContext().getComponentHelper() != null)) {
            result = getComponentContext().getComponentHelper().getHelped();
        }

        return result;

    }

    /**
     * Returns the component context.
     * 
     * @return The component context.
     */
    private ComponentContext getComponentContext() {
        return (ComponentContext) getContext();
    }
}
