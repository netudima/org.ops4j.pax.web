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
package org.ops4j.pax.web.resources.jsf;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceHandlerWrapper;
import javax.faces.application.ViewResource;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.ops4j.pax.web.resources.api.OsgiResourceLocator;
import org.ops4j.pax.web.resources.api.ResourceInfo;
import org.ops4j.pax.web.resources.extender.internal.IndexedOsgiResourceLocator;
import org.ops4j.pax.web.resources.jsf.internal.OsgiResourceLocatorClosableWrapper;
import org.ops4j.pax.web.resources.jsf.internal.ResourceHandlerUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This ResourceHandler can be used in OSGi-enabled JSF applications to access
 * resources in other bundles.
 * <p>
 * It will first try to find resources provided by the application. If none was
 * found it will lookup an osgi-service with the interface {@link OsgiResourceLocator}
 * to find the requested resource in other bundles.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * Bundles providing resources must set the <strong>Manifest-Header</strong>
 * <code>WebResources: true</code>.
 * </p>
 * <p>
 * This class has to be configured in the applications
 * <strong>faces-config.xml</strong>.
 * 
 * <pre>
 * {@literal
 * <?xml version="1.0" encoding="UTF-8"?>
 * <faces-config xmlns="http://xmlns.jcp.org/xml/ns/javaee"
 *     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *     xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facesconfig_2_2.xsd"
 *     version="2.2">
 *   <application>
 *     <resource-handler>org.ops4j.pax.web.resources.jsf.OsgiResourceHandler</resource-handler>
 *   </application>
 * </faces-config>
 * }
 * </pre>
 * </p>
 * 
 * @see IndexedOsgiResourceLocator
 */
public class OsgiResourceHandler extends ResourceHandlerWrapper {

	private final ResourceHandler wrapped;
	private transient Logger logger = LoggerFactory.getLogger(getClass());

	public OsgiResourceHandler(ResourceHandler wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public ResourceHandler getWrapped() {
		return wrapped;
	}

	@Override
	public ViewResource createViewResource(FacesContext context, String resourceName) {
		return getResource(
                    () -> super.createViewResource(context, resourceName),
                    () -> {
                        ResourceInfo resourceInfo = getServiceAndExecute(x -> x.locateResource(resourceName));
                        return transformResourceInfo(resourceInfo, resourceName, null);
                    }
                );
	}

	@Override
	public Resource createResource(String resourceName) {
		return createResource(resourceName, null);
	}

	@Override
	public Resource createResource(String resourceName, String libraryName) {
		return createResource(resourceName, libraryName, null);
	}

	@Override
	public Resource createResource(String resourceName, String libraryName, String contentType) {
		
		logger.warn("================== libraryName='" + libraryName + "' resourceName='" +resourceName +"'" );
		
		Resource resource = super.createResource(resourceName, libraryName, contentType);
		if(resource == null){
			final FacesContext facesContext = FacesContext.getCurrentInstance();
			final Optional<String> localePrefix = ResourceHandlerUtils.getLocalePrefixForLocateResource(facesContext);
			
			String lookup = null;
			if(libraryName != null){
				lookup = libraryName + '/' + resourceName;
			}
			
			if(localePrefix.isPresent()){
				lookup = localePrefix.get() + '/' + lookup;
			}
			
			try(OsgiResourceLocatorClosableWrapper serviceWrapper = new OsgiResourceLocatorClosableWrapper()){
				ResourceInfo res = serviceWrapper.locateResource(lookup);
				return transformResourceInfo(res, resourceName, libraryName);
				// lookup
//				Collection<ResourceInfo> matchingResources = serviceWrapper.findResourcesMatchingAnySegment(resourceName);
			}
		}



		return null;
//		return getResource(
//				() -> super.createResource(resourceName, libraryName, contentType),
//				() -> {
//					ResourceInfo resourceInfo = getServiceAndExecute(x -> x.locateResource(createResourceIdentifier(resourceName, libraryName, contentType)));
//					return transformResourceInfo(resourceInfo, resourceName, libraryName);
//				}
//		);
	}

	private String createResourceIdentifier(String resourceName, String libraryName, String contentType) {
		final String resourceIdentifier;

		if (StringUtils.isNotBlank(libraryName)) {
			resourceIdentifier = (libraryName + "/" + resourceName).replace("//", "/");
		}else{
			resourceIdentifier = resourceName;
		}

		return resourceIdentifier;
	}

  	private Resource transformResourceInfo(ResourceInfo resourceInfo, String resourceName, String libraryName) {
	 if(resourceInfo == null){
	 return null;
	 }
	 return new OsgiResource(resourceInfo.getUrl(), resourceName, libraryName, resourceInfo.getLastModified());
	 }

	 /**
	 * Will first attempt to retrieve a resource via the first given function.
	 * If that failed, the second function will be used.
	 * 
	 * @param firstLookupFunction
	 *            the function which is used to retrieve a resource in the first
	 *            place.
	 * @param secondLookupFunction
	 *            the fallback-function to apply against the
	 *            {@link OsgiResourceLocator} after the first attempt did not
	 *            yied any resource.
	 * @return a {@link Resource}, {@link ViewResource} depending on the
	 *         functions or {@code null}.
	 */
	private <R extends ViewResource> R getResource(Supplier<R> firstLookupFunction, Supplier<R> secondLookupFunction) {
		// check standard first
		R resource = firstLookupFunction.get();
		if (resource == null) {
			// lookup resource in resource bundles
			resource = secondLookupFunction.get();
		}
		return resource;
	}

	/**
	 * Gets a {@link OsgiResourceLocator}-service, applies the given function,
	 * and ungets the service.
	 * 
	 * @param function
	 *            the function to apply against the {@link OsgiResourceLocator}
	 * @return a {@link Resource}, {@link ViewResource} depending on the
	 *         functions or {@code null}.
	 */
	private ResourceInfo getServiceAndExecute(Function<OsgiResourceLocator, ResourceInfo> function) {
		// hook into OSGi-Framework
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		// get-service, execute function, and unget-service
		ServiceReference<OsgiResourceLocator> serviceRef = context.getServiceReference(OsgiResourceLocator.class);
		ResourceInfo resource = null;
		if (serviceRef != null) {
			OsgiResourceLocator resourceLocatorService = context.getService(serviceRef);
			if (resourceLocatorService != null) {
				resource = function.apply(resourceLocatorService);
				resourceLocatorService = null;
			}
		}
		context.ungetService(serviceRef);
		return resource;
	}
}
