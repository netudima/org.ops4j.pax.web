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

import org.apache.commons.lang3.StringUtils;
import org.ops4j.pax.web.resources.api.OsgiResourceLocator;
import org.ops4j.pax.web.resources.api.ResourceInfo;
import org.ops4j.pax.web.resources.api.ResourceQuery;
import org.ops4j.pax.web.resources.extender.internal.IndexedOsgiResourceLocator;
import org.ops4j.pax.web.resources.jsf.internal.ResourceHandlerUtils;
import org.ops4j.pax.web.resources.jsf.internal.ResourceValidationUtils;
import org.ops4j.pax.web.resources.jsf.internal.WebConfigParamUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceHandlerWrapper;
import javax.faces.application.ViewResource;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This ResourceHandler can be used in OSGi-enabled JSF applications to access
 * resources in other bundles.
 * <p>
 * It will first try to find resources provided by the application. If none was
 * found it will lookup an osgi-service with the interface {@link OsgiResourceLocator}
 * to find the requested resource in other bundles.
 * </p>
 * 	<h3>Usage</h3>
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
 * <h3>Currently Unsupported by external resource-bundle</h3>
 * <p>
 * 	Currently JSF-Contracts are not supported to be located from other bundles than the WAB. Furthermore resources
 * 	located within the WAB cannot be override because the implementation will first try to load the resource using
 * 	the underlying JSF-Implementation (e.g. Mojarra/MyFaces)
 * </p>
 * @see IndexedOsgiResourceLocator
 */
public class OsgiResourceHandler extends ResourceHandlerWrapper {

	private static final String INIT_PARAM_RESOURCE_BUFFER_SIZE = "org.ops4j.pax.web.resources.jsf.RESOURCE_BUFFER_SIZE";

	private transient Logger logger = LoggerFactory.getLogger(getClass());
	private final ResourceHandler wrapped;
	private final String[] excludedResourceExtensions;
	private final int resourceBufferSize;

	public OsgiResourceHandler(ResourceHandler wrapped) {
		this.wrapped = wrapped;

		String value = WebConfigParamUtils.getStringInitParameter(
				FacesContext.getCurrentInstance().getExternalContext(),
				ResourceHandler.RESOURCE_EXCLUDES_PARAM_NAME,
				ResourceHandler.RESOURCE_EXCLUDES_DEFAULT_VALUE);

		excludedResourceExtensions = value.split(" ");

		resourceBufferSize = WebConfigParamUtils.getIntegerInitParameter(
				FacesContext.getCurrentInstance().getExternalContext(),
				INIT_PARAM_RESOURCE_BUFFER_SIZE,
				2048);
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
                        return transformResourceInfo(resourceInfo, resourceName, localePrefix, resourceVersion, null, null);
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
		final FacesContext facesContext = FacesContext.getCurrentInstance();
		logger.warn("================== libraryName='" + libraryName + "' resourceName='" +resourceName +"'" );
		if (resourceName.charAt(0) == '/')
		{
			// If resourceName starts with '/', remove that character because it
			// does not have any meaning (with and without should point to the
			// same resource).
			resourceName = resourceName.substring(1);
		}
		if (!ResourceValidationUtils.isValidResourceName(resourceName))
		{
			logger.debug("Invalid resourceName '{}'", resourceName);
			return null;
		}
		if (libraryName != null && !ResourceValidationUtils.isValidLibraryName(libraryName))
		{
			logger.debug("Invalid libraryName '{}'", libraryName);
			return null;
		}

		final Optional<String> localePrefix = ResourceHandlerUtils.getLocalePrefixForLocateResource(facesContext);
		// Contract currently not supported: final List<String> contracts = facesContext.getResourceLibraryContracts();


		// FIXME
		ResourceInfo resourceInfo = null;
		ResourceQuery query = new ResourceQuery()
				.startsWith(localePrefix.get())
				.withSegment(resourceName)
				.withSegment(libraryName)

		getServiceAndExecute(service -> service.findResources())



		// inspect final resource for contentType
		// FIXME deal with content-type
//		if (contentType == null)
//		{
//			try(InputStream is = resourceInfo.getUrl().openConnection().getInputStream()){
//				contentType = URLConnection.guessContentTypeFromStream(is);
//			}catch(IOException e){
//				logger.error("Could not determine contentType from url-resource!", e);
//			}
//		}



		return new OsgiResource(resourceInfo.getUrl(),
				"TODOloc", resourceName, "TODOresVer", libraryName, "TODOlibVer",
				resourceInfo.getLastModified());

//		return getResource(
//				() -> super.createResource(resourceName, libraryName, contentType),
//				() -> {
//					ResourceInfo resourceInfo = getServiceAndExecute(x -> x.locateResource(
//							createResourceIdentifier(resourceName, null, libraryName, null, contentType)));
//					return transformResourceInfo(resourceInfo, resourceName, localePrefix, resourceVersion, libraryName, libraryVersion);
//				}
//		);
	}
	
	@Override
	public void handleResourceRequest(FacesContext facesContext) throws IOException {
		final Map<String, String> requestParameterMap = facesContext.getExternalContext().getRequestParameterMap();
		if(!"osgi".equals(requestParameterMap.get(OsgiResource.REQUEST_PARAM_TYPE))){
			// no OsgiResource...proceed with default ResourceHandler
			super.handleResourceRequest(facesContext);
		}

		String localePrefix = requestParameterMap.get(OsgiResource.REQUEST_PARAM_LOCALE);
		String libraryName = requestParameterMap.get(OsgiResource.REQUEST_PARAM_LIBRARY);
		String libraryVersion = requestParameterMap.get(OsgiResource.REQUEST_PARAM_LIBRARY_VERSION);
		String resourceVersion = requestParameterMap.get(OsgiResource.REQUEST_PARAM_RESOURCE_VERSION);


		String resourceBasePath = ResourceHandlerUtils.calculateResourceBasePath(facesContext);

		if (resourceBasePath == null)
		{
			// No base name could be calculated, so no further
			//advance could be done here. HttpServletResponse.SC_NOT_FOUND
			//cannot be returned since we cannot extract the
			//resource base name
			return;
		}


		// We neet to get an instance of HttpServletResponse, but sometimes
		// the response object is wrapped by several instances of
		// ServletResponseWrapper (like ResponseSwitch).
		// Since we are handling a resource, we can expect to get an
		// HttpServletResponse.
		HttpServletResponse httpServletResponse = ResourceHandlerUtils.getHttpServletResponse(
				facesContext.getExternalContext().getResponse());
		if (httpServletResponse == null)
		{
			throw new IllegalStateException("Could not obtain an instance of HttpServletResponse.");
		}

		if (ResourceHandlerUtils.isResourceIdentifierExcluded(facesContext, resourceBasePath, excludedResourceExtensions))
		{
			httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		// extract resourceName. if none was found set Response to 404
		String resourceName = null;
		if (resourceBasePath.startsWith(ResourceHandler.RESOURCE_IDENTIFIER))
		{
			resourceName = resourceBasePath
					.substring(ResourceHandler.RESOURCE_IDENTIFIER.length() + 1);

			if (resourceBasePath != null && !ResourceValidationUtils.isValidResourceName(resourceName))
			{
				httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		}
		else
		{
			//Does not have the conditions for be a resource call
			httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}


		if (libraryName != null && !ResourceValidationUtils.isValidLibraryName(libraryName))
		{
			httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}


		String resourceIdentifier = createResourceIdentifier(localePrefix, resourceName, resourceVersion, libraryName, libraryVersion);

		OsgiResource resource = null;
		// in this case we have the full path to the resource, no version-magic needed
		ResourceInfo resourceInfo = getServiceAndExecute(service -> service.locateResource(resourceIdentifier));
		if(resourceInfo == null){
			httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		resource = new OsgiResource(resourceInfo.getUrl(),
				localePrefix, resourceName, resourceVersion, libraryName, libraryVersion,
				resourceInfo.getLastModified());


		// Resource has not changed, return 304
		if (!resource.userAgentNeedsUpdate(facesContext))
		{
			httpServletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}


		// serve

		httpServletResponse.setContentType(ResourceHandlerUtils.getContentType(resource, facesContext.getExternalContext()));

		Map<String, String> headers = resource.getResponseHeaders();

		for (Map.Entry<String, String> entry : headers.entrySet())
		{
			httpServletResponse.setHeader(entry.getKey(), entry.getValue());
		}

		// Sets the preferred buffer size for the body of the response
		facesContext.getExternalContext().setResponseBufferSize(this.resourceBufferSize);

		//serve up the bytes (taken from trinidad ResourceServlet)
		try
		{

			//byte[] buffer = new byte[_BUFFER_SIZE];
			byte[] buffer = new byte[this.resourceBufferSize];

			try(InputStream in = resource.getInputStream(); OutputStream out = httpServletResponse.getOutputStream())
			{
				int count = ResourceHandlerUtils.pipeBytes(in, out, buffer);
				//set the content lenght
				if (!httpServletResponse.isCommitted())
				{
					httpServletResponse.setContentLength(count);
				}
			}
		}
		catch (IOException e)
		{
			//TODO: Log using a localized message (which one?)
			if (logger.isErrorEnabled())
			{
				logger.error("Error trying to load resource '{}' with library '{}' : {}",
						new Object[] {resourceName, libraryName, e.getMessage(), e});
			}
			// return 404
			httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}

	}

	/**
	 * Creates an ResourceIdentifier accoring to chapter 2.6.1.3 from the JSF 2.2 specification
	 * @param localePrefix
	 * @param resourceName
	 * @param resourceVersion
	 * @param libraryName
	 * @param libraryVersion
     * @return
     */
	private String createResourceIdentifier(final String localePrefix, final String resourceName, final String resourceVersion, final String libraryName, final String libraryVersion) {
		final StringBuilder sb = new StringBuilder();

		if (StringUtils.isNotBlank(localePrefix)) {
			sb.append(localePrefix).append('/');
		}
		if (StringUtils.isNotBlank(libraryName)) {
			sb.append(libraryName).append('/');
		}
		if(StringUtils.isNotBlank(libraryVersion)){
			sb.append(libraryVersion).append('/');
		}
		sb.append(resourceName).append('/');
		if(StringUtils.isNotBlank(resourceVersion)){
			sb.append(resourceVersion).append('/');
		}
		return sb.toString();
	}

  	private Resource transformResourceInfo(ResourceInfo resourceInfo, String resourceName, String localePrefix, String resourceVersion, String libraryName, String libraryVersion) {
	 if(resourceInfo == null){
	 return null;
	 }
	 return new OsgiResource(resourceInfo.getUrl(), localePrefix, resourceName, resourceVersion, libraryName, libraryVersion, resourceInfo.getLastModified());
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
