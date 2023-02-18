/*******************************************************************************
 * Copyright (c) 2022 EquoTech, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EquoTech, Inc. - initial API and implementation
 *******************************************************************************/
package dev.equo.ide.maven;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({SnapshotExtension.class})
public class MavenCatalogTest extends MavenHarness {
	private void test(String content, Expect expect) throws IOException, InterruptedException {
		setPom(
				content
						+ "\n"
						+ "<filters>\n"
						+ "  <filter><platformNone>true</platformNone></filter>\n"
						+ "</filters>\n");
		mvnw("equo-ide:list -Drequest")
				.snapshotBetween(
						"(default-cli) @ equo-maven-test-harness ---", "[INFO] BUILD SUCCESS", expect);
	}

	@Test
	public void simple(Expect expect) throws Exception {
		test("<jdt/>", expect.scenario("jdt"));
		test("<gradleBuildship/>", expect.scenario("gradleBuildship"));
	}
}
