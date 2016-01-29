/* Copyright 2016 Marc Schlegel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.resources.jsf.internal;

import org.ops4j.pax.web.resources.api.OsgiResourceLocator;
import org.ops4j.pax.web.resources.api.ResourceInfo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import java.util.Collection;

/**
 * Use try-with-resources to automatically cleanup service at the end of try-block, or call close() manually.
 *
 * <pre>
 * {@literal
 *      try(OsgiResourceLocatorClosableWrapper serviceWrapper = new OsgiResourceLocatorClosableWrapper())
 *      {
 *          ResourceInfo res = serviceWrapper.locateResource(...)
 *      }
 * }
 * </pre>
 */
public class OsgiResourceLocatorClosableWrapper implements AutoCloseable {
    private final BundleContext context;
    private final ServiceReference<OsgiResourceLocator> serviceRef;
    private volatile OsgiResourceLocator resourceLocatorService;

    public OsgiResourceLocatorClosableWrapper(){
        // hook into OSGi-Framework
        this.context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        // get-service, execute function, and unget-service
        this.serviceRef = context.getServiceReference(OsgiResourceLocator.class);
        if (serviceRef != null) {
            this.resourceLocatorService = context.getService(serviceRef);
        }
        context.ungetService(serviceRef);
    }

    /**
     * Ungets the OSGi-Service for the {@link OsgiResourceLocator}.
     * Exception has been removed because we cannot do anything about it anyways.
     */
    @Override
    public void close() {
        resourceLocatorService = null;
        context.ungetService(serviceRef);
    }

    /**
     * @see OsgiResourceLocator#locateResource(String)
     */
    public ResourceInfo locateResource(final String resourceName){
        return resourceLocatorService.locateResource(resourceName);
    }


    /**
     * @see OsgiResourceLocator#findResourcesInPath(String)
     */
    public Collection<ResourceInfo> findResourcesInPath(final String path){
        return resourceLocatorService.findResourcesInPath(path);
    }

    /**
     * @see OsgiResourceLocator#findResourcesMatchingAnySegment(String)
     */
    public Collection<ResourceInfo> findResourcesMatchingAnySegment(final String resourceName){
        return resourceLocatorService.findResourcesMatchingAnySegment(resourceName);
    }
}
