package org.ops4j.pax.web.resources.jsf;

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

    OsgiResourceLocatorClosableWrapper(){
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
    ResourceInfo locateResource(final String resourceName){
        return resourceLocatorService.locateResource(resourceName);
    }


    /**
     * @see OsgiResourceLocator#findResourcesInPath(String)
     */
    Collection<ResourceInfo> findResourcesInPath(final String path){
        return resourceLocatorService.findResourcesInPath(path);
    }

    /**
     * @see OsgiResourceLocator#findResourcesMatchingAnySegment(String)
     */
    Collection<ResourceInfo> findResourcesMatchingAnySegment(final String resourceName){
        return resourceLocatorService.findResourcesMatchingAnySegment(resourceName);
    }
}
