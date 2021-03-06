/* Copyright 2011 Guillaume Nodet.
 * Copyright 2011 Achim Nierbeck.
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
package org.ops4j.pax.web.deployer.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contain various methods that are useful for deploying artifacts
 * 
 * @author gnodet
 */
public final class DeployerUtils {

	private static final String DEFAULT_VERSION = "0.0.0";

	private static final Pattern ARTIFACT_MATCHER = Pattern
			.compile(
					"(.+)(?:-(\\d+)(?:\\.(\\d+)(?:\\.(\\d+))?)?(?:[^a-zA-Z0-9](.*))?)(?:\\.([^\\.]+))",
					Pattern.DOTALL);
	private static final Pattern FUZZY_MODIFIDER = Pattern.compile(
			"(?:\\d+[.-])*(.*)", Pattern.DOTALL);

	/** Private constructors to avoid instantiation */
	private DeployerUtils() {
	}

	/**
	 * Heuristic to compute the name and version of a file given it's name on
	 * disk
	 * 
	 * @param url
	 *            the name of the file
	 * @return the name and version of that file
	 */
	public static String[] extractNameVersionType(String url) {
		Matcher m = ARTIFACT_MATCHER.matcher(url);
		if (!m.matches()) {
			return new String[] { url.split("\\.")[0], DEFAULT_VERSION };
		} else {
			//CHECKSTYLE:OFF
			StringBuilder v = new StringBuilder();
			String d1 = m.group(1);
			String d2 = m.group(2);
			String d3 = m.group(3);
			String d4 = m.group(4);
			String d5 = m.group(5);
			String d6 = m.group(6);
			if (d2 != null) {
				v.append(d2);
				if (d3 != null) {
					v.append('.');
					v.append(d3);
					if (d4 != null) {
						v.append('.');
						v.append(d4);
						if (d5 != null) { 
							v.append(".");
							cleanupModifier(v, d5);
						}
					} else if (d5 != null) {
						v.append(".0.");
						cleanupModifier(v, d5);
					}
				} else if (d5 != null) {
					v.append(".0.0.");
					cleanupModifier(v, d5);
				}
			}
			//CHECKSTYLE:ON
			return new String[] { d1, v.toString(), d6 };
		}
	}

	private static void cleanupModifier(StringBuilder result, String mod) {
		Matcher m = FUZZY_MODIFIDER.matcher(mod);
		String modifier = mod;
		if (m.matches()) {
			modifier = m.group(1);
		}
		for (int i = 0; i < modifier.length(); i++) {
			char c = modifier.charAt(i);
			if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')
					|| (c >= 'A' && c <= 'Z') || c == '_' || c == '-') {
				result.append(c);
			}
		}
	}

}
