/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle.plugins;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = true)
public class AddSettingsPlugin extends Recipe {
    @Option(displayName = "Plugin id",
            description = "The plugin id to apply.",
            example = "com.jfrog.bintray")
    String pluginId;

    @Option(displayName = "Plugin version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "3.x")
    String version;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Override
    public String getDisplayName() {
        return "Add a Gradle settings plugin";
    }

    @Override
    public String getDescription() {
        return "Add a Gradle settings plugin to `settings.gradle(.kts)`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new IsSettingsGradle<>(),
                new GroovyIsoVisitor<ExecutionContext>() {
                    @Override
                    public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                        Optional<GradleSettings> maybeGradleSettings = cu.getMarkers().findFirst(GradleSettings.class);
                        if (!maybeGradleSettings.isPresent()) {
                            return cu;
                        }

                        GradleSettings gradleSettings = maybeGradleSettings.get();
                        return (G.CompilationUnit) new AddPluginVisitor(pluginId, version, versionPattern, gradleSettings.getPluginRepositories()).visitNonNull(cu, ctx);
                    }
                });
    }
}
