package org.ops4j.pax.web.itest;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardFilter;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(JUnit4TestRunner.class)
public class RootAliasIntegrationTest extends ITestBase {

	private ServiceRegistration servletRoot;
	private ServiceRegistration servletSecond;

	@Configuration
	public static Option[] configure() {
//		Option[] options = baseConfigure();
//
//		Option[] options2 = options(mavenBundle()
//				.groupId("org.ops4j.pax.web.samples")
//				.artifactId("whiteboard")
//				.version(getProjectVersion()).noStart());
//
//		List<Option> list = new ArrayList<Option>(Arrays.asList(options));
//		list.addAll(Arrays.asList(options2));
//
//		return (Option[]) list.toArray(new Option[list.size()]);
		return baseConfigure();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException, ServletException {
		
		
		servletRoot = registerServlet("/myRoot");
		servletSecond = registerServlet("/myRoot/second");
		

	}
	
	private ServiceRegistration registerServlet(final String path) throws ServletException {
		
		Dictionary<String, String> initParams = new Hashtable<String, String>();
		initParams.put("alias", path);

        return bundleContext.registerService(Servlet.class.getName(),
        		new HttpServlet() {

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.getOutputStream().write(path.getBytes());
            }
        }, initParams);
        
    }

	@After
	public void tearDown() throws BundleException {
		servletRoot.unregister();
		servletSecond.unregister();
	}

	@Test
	public void testWhiteBoardSlash() throws BundleException,
			InterruptedException, IOException {
		testWebPath("http://127.0.0.1:8181/myRoot", "myRoot");
		
		testWebPath("http://127.0.0.1:8181/myRoot/second", "myRoot/second");
	}

}