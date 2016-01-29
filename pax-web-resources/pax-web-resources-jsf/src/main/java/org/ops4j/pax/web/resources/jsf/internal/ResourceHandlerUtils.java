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


import org.apache.commons.lang3.ClassUtils;
import org.ops4j.pax.web.resources.jsf.OsgiResource;
import org.ops4j.pax.web.resources.jsf.OsgiResourceHandler;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceWrapper;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class ResourceHandlerUtils {

	/**
	 * FIXME taken from MyFaces
	 * @param facesContext
	 * @return
	 */
	public static Optional<String> getLocalePrefixForLocateResource(final FacesContext facesContext)
    {
        String localePrefix = null;
        boolean isResourceRequest = facesContext.getApplication().getResourceHandler().isResourceRequest(facesContext);

        if (isResourceRequest)
        {
            localePrefix = facesContext.getExternalContext().getRequestParameterMap().get(OsgiResource.REQUEST_PARAM_LOCALE);
            
            if (localePrefix != null)
            {
                if (!ResourceValidationUtils.isValidLocalePrefix(localePrefix))
                {
                    return Optional.empty();
                }
                return Optional.of(localePrefix);
            }
        }
        
        String bundleName = facesContext.getApplication().getMessageBundle();

        if (null != bundleName)
        {
            Locale locale = null;
            
            if (isResourceRequest || facesContext.getViewRoot() == null)
            {
                locale = facesContext.getApplication().getViewHandler()
                                .calculateLocale(facesContext);
            }
            else
            {
                locale = facesContext.getViewRoot().getLocale();
            }

            try
            {
//                ResourceBundle bundle = ResourceBundle
//                        .getBundle(bundleName, locale, ClassUtils.getContextClassLoader());
                // FIXME lets see if this works because the ResourceHandler is created by the webapp-bundle
                // hope is, that they use the same classloader
                ResourceBundle bundle = ResourceBundle.getBundle(bundleName, locale);

                if (bundle != null)
                {
                    localePrefix = bundle.getString(ResourceHandler.LOCALE_PREFIX);
                }
            }
            catch (MissingResourceException e)
            {
                // Ignore it and return null
            }
        }
        return Optional.ofNullable(localePrefix);
    }


    public static FacesServletMapping getFacesServletMapping(final FacesContext facesContext) {
        Map<Object, Object> attributes = facesContext.getAttributes();

        // Has the mapping already been determined during this request?
        FacesServletMapping mapping = (FacesServletMapping) attributes.get(FacesServletMapping.CACHED_SERVLET_MAPPING);
        if (mapping == null)
        {
            ExternalContext externalContext = facesContext.getExternalContext();
            mapping = FacesServletMapping.calculateFacesServletMapping(externalContext.getRequestServletPath(),
                    externalContext.getRequestPathInfo());

            attributes.put(FacesServletMapping.CACHED_SERVLET_MAPPING, mapping);
        }
        return mapping;
    }

    /**
     * FIXME DISCLAIMER MYFACES
     * Trys to obtain a HttpServletResponse from the Response.
     * Note that this method also trys to unwrap any ServletResponseWrapper
     * in order to retrieve a valid HttpServletResponse.
     * @param response
     * @return if found, the HttpServletResponse, null otherwise
     */
    public static HttpServletResponse getHttpServletResponse(Object response)
    {
        // unwrap the response until we find a HttpServletResponse
        while (response != null)
        {
            if (response instanceof HttpServletResponse)
            {
                // found
                return (HttpServletResponse) response;
            }
            if (response instanceof ServletResponseWrapper)
            {
                // unwrap
                response = ((ServletResponseWrapper) response).getResponse();
            }
            // no more possibilities to find a HttpServletResponse
            break;
        }
        return null; // not found
    }


    public static String calculateResourceBasePath(FacesContext facesContext)
    {
        FacesServletMapping mapping = getFacesServletMapping(facesContext);
        ExternalContext externalContext = facesContext.getExternalContext();

        if (mapping != null)
        {
            String resourceBasePath = null;
            if (mapping.isExtensionMapping())
            {
                // Mapping using a suffix. In this case we have to strip
                // the suffix. If we have a url like:
                // http://localhost:8080/testjsf20/javax.faces.resource/imagen.jpg.jsf?ln=dojo
                //
                // The servlet path is /javax.faces.resource/imagen.jpg.jsf
                //
                // For obtain the resource name we have to remove the .jsf suffix and
                // the prefix ResourceHandler.RESOURCE_IDENTIFIER
                resourceBasePath = externalContext.getRequestServletPath();
                int stripPoint = resourceBasePath.lastIndexOf('.');
                if (stripPoint > 0)
                {
                    resourceBasePath = resourceBasePath.substring(0, stripPoint);
                }
            }
            else
            {
                // Mapping using prefix. In this case we have to strip
                // the prefix used for mapping. If we have a url like:
                // http://localhost:8080/testjsf20/faces/javax.faces.resource/imagen.jpg?ln=dojo
                //
                // The servlet path is /faces
                // and the path info is /javax.faces.resource/imagen.jpg
                //
                // For obtain the resource name we have to remove the /faces prefix and
                // then the prefix ResourceHandler.RESOURCE_IDENTIFIER
                resourceBasePath = externalContext.getRequestPathInfo();
            }
            return resourceBasePath;
        }
        else
        {
            //If no mapping is detected, just return the
            //information follows the servlet path but before
            //the query string
            return externalContext.getRequestPathInfo();
        }
    }


    public static boolean isResourceIdentifierExcluded(final FacesContext context, final String resourceIdentifier, final String[] excludedResourceExtensions)
    {
        for (int i = 0; i < excludedResourceExtensions.length; i++)
        {
            if (resourceIdentifier.endsWith(excludedResourceExtensions[i]))
            {
                return true;
            }
        }
        return false;
    }


    /**
     * Reads the specified input stream into the provided byte array storage and
     * writes it to the output stream.
     */
    public static int pipeBytes(InputStream in, OutputStream out, byte[] buffer)
            throws IOException
    {
        int count = 0;
        int length;

        while ((length = (in.read(buffer))) >= 0)
        {
            out.write(buffer, 0, length);
            count += length;
        }
        return count;
    }



    public static String getContentType(Resource resource, ExternalContext externalContext)
    {
        String contentType = resource.getContentType();

        // the resource does not provide a content-type --> determine it via mime-type
        if (contentType == null || contentType.length() == 0)
        {
            String resourceName = getWrappedResourceName(resource);

            if (resourceName != null)
            {
                contentType = externalContext.getMimeType(resourceName);
            }
        }

        return contentType;
    }


    /**
     * Recursively unwarp the resource until we find the real resourceName
     * This is needed because the JSF2 specced ResourceWrapper doesn't override
     * the getResourceName() method :(
     * @param resource
     * @return the first non-null resourceName or <code>null</code> if none set
     */
    private static String getWrappedResourceName(Resource resource)
    {
        String resourceName = resource.getResourceName();
        if (resourceName != null)
        {
            return resourceName;
        }

        if (resource instanceof ResourceWrapper)
        {
            return getWrappedResourceName(((ResourceWrapper) resource).getWrapped());
        }

        return null;
    }

}
