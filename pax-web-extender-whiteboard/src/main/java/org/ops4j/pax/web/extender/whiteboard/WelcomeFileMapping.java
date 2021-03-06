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
 package org.ops4j.pax.web.extender.whiteboard;

/**
 * Welcome file mapping
 * 
 * @author dsklyut
 * @since 0.7.0
 */
public interface WelcomeFileMapping {

	/**
	 * Getter.
	 * 
	 * @return id of the http context this jsp belongs to
	 */
	String getHttpContextId();

	/**
	 * Getter
	 * 
	 * @return true if the client should be redirected to welcome file or false
	 *         if forwarded
	 */
	boolean isRedirect();

	/**
	 * Getter
	 * 
	 * @return an array of welcome files paths. Paths must not start or end with
	 *         "/"
	 */
	String[] getWelcomeFiles();

}
