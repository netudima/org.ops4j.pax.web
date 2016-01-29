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
package org.ops4j.pax.web.resources.api;

import org.osgi.framework.Bundle;

import java.util.Collection;

/**
 * <p>
 * Services implementing this interface must be able to serve
 * {@link ResourceInfo}s from other bundles.
 * </p>
 */
public interface OsgiResourceLocator {

	/**
	 * <p>
	 * Register the given bundle to take part in the lookup-process for webresources.
	 * </p>
	 * <p>
	 * This method is called from the BundleListener in this module.
	 * </p>
	 * 
	 * @param bundle
	 *            the starting bundle containing JSF resources to share
	 */
	void register(Bundle bundle);

	/**
	 * <p>
	 * Unregister the given bundle from the lookup-process for webresources.
	 * Resources must be cleaned.
	 * </p>
	 * <p>
	 * This method is called from the BundleListener in this module.
	 * </p>
	 * 
	 * @param bundle
	 *            the stopping bundle containing JSF resources
	 */
	void unregister(Bundle bundle);

	/**
	 * Lookup the given resource according to Servlet 3.0 specification.
	 * 
	 * @param resourceName
	 *            name or path of the resource to find
	 * @return {@code ResourceInfo} matching the given name, or {@code null}
	 */
	ResourceInfo locateResource(String resourceName);

	/**
	 * Lookup all resources matching the given path
	 *
	 * @param path the path under which the resources should be found
	 * @return all {@code ResourceInfo}s matching the given path
     */
	Collection<ResourceInfo> findResourcesInPath(String path);

	/**
	 * Lookup all resources matching the given name at any location
	 *
	 * @param resourceName the name under which the resources should be found within any segment of the path
	 * @return all {@code ResourceInfo}s matching the given resourceName anywhere in the path
	 */
	Collection<ResourceInfo> findResourcesMatchingAnySegment(String resourceName);


	Collection<ResourceInfo> findResources(ResourceQuery query);
}
