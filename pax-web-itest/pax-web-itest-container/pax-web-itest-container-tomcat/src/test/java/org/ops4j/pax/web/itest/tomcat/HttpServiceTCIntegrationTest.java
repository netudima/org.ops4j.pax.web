/*
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
 package org.ops4j.pax.web.itest.tomcat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Dictionary;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.WaitCondition;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 */
@RunWith(PaxExam.class)
public class HttpServiceTCIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(HttpServiceTCIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public Option[] configure() {
		return configureTomcat();
	}

	@Before
	public void setUp() throws Exception {
		waitForServer("http://127.0.0.1:8282/");
		initServletListener(null);
		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-hs/" + VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
		waitForServletListener();
		
	}

	@After
	public void tearDown() throws BundleException {
		LOG.info("tear down ... ");
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
		
		Bundle[] bundles = bundleContext.getBundles();
		for (Bundle b : bundles) {
			Dictionary<?,?> headers = b.getHeaders();
			String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null) {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath);
			} else {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
			}
		}
		
		LOG.info(" ... good bye ... ");
	}

	/**
	 * You will get a list of bundles installed by default plus your testcase,
	 * wrapped into a bundle called pax-exam-probe
	 */
	@Test
	public void listBundles() {
		for (Bundle b : bundleContext.getBundles()) {
			System.out.println("Bundle " + b.getBundleId() + " : "
					+ b.getSymbolicName());
		}

	}

	@Test
	public void testSubPath() throws Exception {
		String path = "http://127.0.0.1:8282/helloworld/hs";
		LOG.info("testSubPath - call path {}", path);
		testClient.testWebPath(path, "Hello World");
		
		//test to retrive Image
		path = "http://127.0.0.1:8282/images/logo.png";
		LOG.info("testSubPath - call path {}", path);
		testClient.testWebPath(path, "", 200, false);
		
	}

	@Test
	public void testRootPath() throws Exception {

		String path = "http://127.0.0.1:8282/";
		LOG.info("testSubPath - call path {}", path);
		testClient.testWebPath(path, "");

	}
	
	@Test
	public void testServletPath() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8282/lall/blubb", "Servlet Path: ");
		testClient.testWebPath("http://127.0.0.1:8282/lall/blubb", "Path Info: /lall/blubb");

	}
	
	@Test
	public void testServletDeRegistration() throws Exception {
		
		if (installWarBundle != null) {
			installWarBundle.stop();
		}
	}
	
	@Test
	public void testNCSALogger() throws Exception {
		testSubPath();

		SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd");
		String date = formater.format(new Date());
		//access_log.2013-06-13.log
		final File logFile = new File("target/logs/access_log."+date+".log");

		LOG.info("Log-File: {}", logFile.getAbsoluteFile());
		
		new WaitCondition("logfile") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return logFile != null && logFile.exists();
			}
		}.waitForCondition();

		assertNotNull(logFile);

		boolean exists = logFile.getAbsoluteFile().exists();

		assertTrue(exists);

		FileInputStream fstream = new FileInputStream(logFile.getAbsoluteFile());
		DataInputStream in = new DataInputStream(fstream);
        final BufferedReader brCheck = new BufferedReader(new InputStreamReader(in));
		
		new WaitCondition("logfile content") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return brCheck.readLine() != null;
			}
		}.waitForCondition();
		
		brCheck.close();
		in.close();
		fstream.close();
		
		fstream = new FileInputStream(logFile.getAbsoluteFile());
		in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		
		String strLine = br.readLine();
		
		assertNotNull(strLine);
		in.close();
		fstream.close();
	}
}