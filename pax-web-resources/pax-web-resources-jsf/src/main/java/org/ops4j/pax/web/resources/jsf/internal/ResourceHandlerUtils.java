package org.ops4j.pax.web.resources.jsf.internal;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.faces.application.ResourceHandler;
import javax.faces.context.FacesContext;

public class ResourceHandlerUtils {

	/**
	 * FIXME taken from MyFaces
	 * @param context
	 * @return
	 */
	public static Optional<String> getLocalePrefixForLocateResource(final FacesContext facesContext)
    {
        String localePrefix = null;
        boolean isResourceRequest = facesContext.getApplication().getResourceHandler().isResourceRequest(facesContext);

        if (isResourceRequest)
        {
            localePrefix = facesContext.getExternalContext().getRequestParameterMap().get("loc");
            
            if (localePrefix != null)
            {
                if (!isValidLocalePrefix(localePrefix))
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
                ResourceBundle bundle = ResourceBundle
                        .getBundle(bundleName, locale);

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
	
	/**
	 * FIXME taken from MyFaces
	 * @param localePrefix
	 * @return
	 */
	private static boolean isValidLocalePrefix(String localePrefix)
    {
        for (int i = 0; i < localePrefix.length(); i++)
        {
            char c = localePrefix.charAt(i);
            if ( (c >='A' && c <='Z') || c == '_' || (c >='a' && c <='z') || (c >='0' && c <='9') )
            {
                continue;
            }
            else
            {
                return false;
            }
        }
        return true;
    }
	

	
}
