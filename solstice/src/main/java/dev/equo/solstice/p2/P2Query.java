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
package dev.equo.solstice.p2;

import com.diffplug.common.swt.os.SwtPlatform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import javax.annotation.Nullable;

/**
 * Follows the dependency information of a set of {@link dev.equo.solstice.p2.P2Unit} so that they
 * can be resolved from maven or directly from p2 if necessary.
 */
public class P2Query {
	private P2Session session;

	P2Query(P2Session session) {
		this.session = session;
	}

	private TreeSet<String> exclude = new TreeSet<>();
	private List<String> excludePrefix = new ArrayList<>();
	private TreeSet<P2Unit> resolved = new TreeSet<>();
	private List<UnmetRequirement> unmetRequirements = new ArrayList<>();
	private List<ResolvedWithFirst> resolvedWithFirst = new ArrayList<>();
	private Map<String, String> filterProps = new HashMap<String, String>();

	public P2Session getSession() {
		return session;
	}

	public void exclude(String toExclude) {
		exclude.add(toExclude);
	}

	public void excludePrefix(String prefix) {
		excludePrefix.add(prefix);
	}

	public void setPlatform(@Nullable SwtPlatform platform) {
		if (platform == null) {
			filterProps.clear();
		} else {
			filterProps.put("osgi.os", platform.getOs());
			filterProps.put("osgi.ws", platform.getWs());
			filterProps.put("osgi.arch", platform.getArch());
		}
	}

	public void resolve(String idToResolve) {
		resolve(session.getUnitById(idToResolve));
	}

	private boolean addUnlessExcludedOrAlreadyPresent(P2Unit unit) {
		for (var prefix : excludePrefix) {
			if (unit.id.startsWith(prefix)) {
				return false;
			}
		}
		if (exclude.contains(unit.id)) {
			return false;
		}
		if (!filterProps.isEmpty() && unit.filter != null && !unit.filter.matches(filterProps)) {
			return false;
		}
		return resolved.add(unit);
	}

	private void resolve(P2Unit toResolve) {
		if (!addUnlessExcludedOrAlreadyPresent(toResolve)) {
			return;
		}
		for (var requirement : toResolve.requires) {
			if (requirement.hasOnlyOne()) {
				resolve(requirement.getOnlyOne());
			} else {
				var units = requirement.get();
				if (units.isEmpty()) {
					unmetRequirements.add(new UnmetRequirement(toResolve, requirement));
				} else {
					resolve(units.get(0));
					resolvedWithFirst.add(new ResolvedWithFirst(toResolve, requirement));
				}
			}
		}
	}

	public List<P2Unit> getJars() {
		var jars = new ArrayList<P2Unit>();
		for (var unit : resolved) {
			if (unit.id.endsWith("feature.jar")
					|| "true".equals(unit.properties.get(P2Unit.P2_TYPE_FEATURE))
					|| "true".equals(unit.properties.get(P2Unit.P2_TYPE_CATEGORY))) {
				continue;
			}
			jars.add(unit);
		}
		return jars;
	}

	public List<P2Unit> getFeatures() {
		return getUnitsWithProperty(P2Unit.P2_TYPE_CATEGORY, "true");
	}

	public List<P2Unit> getCategories() {
		return getUnitsWithProperty(P2Unit.P2_TYPE_CATEGORY, "true");
	}

	public List<P2Unit> getUnitsWithProperty(String key, String value) {
		List<P2Unit> matches = new ArrayList<>();
		for (var unit : resolved) {
			if (Objects.equals(value, unit.properties.get(key))) {
				matches.add(unit);
			}
		}
		return matches;
	}

	public List<String> getJarsOnMavenCentral() {
		var mavenCoords = new ArrayList<String>();
		for (var unit : getJars()) {
			var mavenState = MavenStatus.forUnit(unit);
			if (mavenState.isOnMavenCentral()) {
				mavenCoords.add(mavenState.coordinate());
			}
		}
		return mavenCoords;
	}

	public List<P2Unit> getjarsNotOnMavenCentral() {
		var notOnMaven = new ArrayList<P2Unit>();
		for (var unit : getJars()) {
			var mavenState = MavenStatus.forUnit(unit);
			if (!mavenState.isOnMavenCentral()) {
				notOnMaven.add(unit);
			}
		}
		return notOnMaven;
	}

	/** Adds every unit in the session, subject to the query filters. */
	public void addAllUnits() {
		session.units.forEach(this::addUnlessExcludedOrAlreadyPresent);
	}

	static class UnmetRequirement {
		final P2Unit target;
		final P2Session.Providers unmet;

		UnmetRequirement(P2Unit target, P2Session.Providers unmet) {
			this.target = target;
			this.unmet = unmet;
		}
	}

	static class ResolvedWithFirst {
		final P2Unit target;
		final P2Session.Providers withFirst;

		ResolvedWithFirst(P2Unit target, P2Session.Providers withFirst) {
			this.target = target;
			this.withFirst = withFirst;
		}
	}
}
