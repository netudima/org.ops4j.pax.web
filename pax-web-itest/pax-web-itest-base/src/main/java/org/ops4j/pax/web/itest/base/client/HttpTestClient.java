package org.ops4j.pax.web.itest.base.client;

import java.util.function.Predicate;

public interface HttpTestClient {
	
	/**
	 * Configures a keystore used for SSL
	 * @param keystoreLocation path to keystorefile
	 * @param user keystore username
	 * @param password keystore password
	 * @return the HttpTestClient-instance
	 */
	public HttpTestClient withKeystore(String keystoreLocation, String user, String password);
	
	/**
	 * Configures the pending execution with additional request-headers which must be set
	 * opon execution
	 * @param header header-name
	 * @param value header-value
	 * @return the HttpTestClient-instance
	 */
	public HttpTestClient addRequestHeader(String header, String value);
	
	/**
	 * Sets the expected return-code that is expected after execution
	 * @param returnCode the expected HTTP return-code
	 * @return the HttpTestClient-instance
	 */
	public HttpTestClient withReturnCode(int returnCode);
	
	/**
	 * 
	 * @param message description what the response should contain
	 * @param assertion the assertion-predicate gets the response-body as parameter
	 * @return the HttpTestClient-instance
	 */
	public HttpTestClient prepareResponseAssertion(final String message, final Predicate<String> assertion);
	
	/**
	 * Executes the this test-client-configuration against the given url.
	 * Note: this method is a terminal-call and ends the fluent-method-chaining.
	 * @param url Destination-url that should be tested
	 * @throws Exception exception propagated from underlying client
	 */
	public void executeTest (String url) throws Exception;
	
}
