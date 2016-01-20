package org.ops4j.pax.web.itest.base.client;

public class HttpTestClientFactory {

	/**
	 * creates a default HttpTestClient based on Apache HttpComponents with
	 * some default configuration.
	 * <ul>
	 * 	<li>Return-Code: 200 OK</li>
	 * 	<li>Keystore: src/test/resources/keystore</li>
	 * 	<li>Request-Header: Accept-Language=en</li>
	 * </ul>
	 * @return Apache HttpComponents HttpTestClient
	 */
	public static HttpTestClient createHttpComponentsTestClient(){
		return new HttpComponentsTestClient()
			.withKeystore("src/test/resources/keystore", "admin", "admin")
			.addRequestHeader("Acccept-Language", "en")
			.withReturnCode(200);
	}
	
}
