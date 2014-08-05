/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.deployment.chain;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DeploymentTaskBuilder {

    private final DeploymentChainBuilder deploymentChainBuilder;
    private final DeploymentTask deploymentTask;
    private boolean committed;

    DeploymentTaskBuilder(final DeploymentChainBuilder deploymentChainBuilder, final DeploymentTask deploymentTask) {
        this.deploymentChainBuilder = deploymentChainBuilder;
        this.deploymentTask = deploymentTask;
    }

    public DeploymentTaskBuilder transforms(DeploymentGoal goal) {

    }

    public DeploymentTaskBuilder produces(ProducibleDeploymentGoal<?> goal) {

    }

    public DeploymentTaskBuilder contributes(ContributableDeploymentGoal<?> goal) {

    }

    public DeploymentTaskBuilder consumes(ProducibleDeploymentGoal<?> goal) {

    }

    public DeploymentTaskBuilder consumes(ContributableDeploymentGoal<?> goal) {

    }

    public DeploymentTaskBuilder before(DeploymentTask task) {

    }

    public DeploymentTaskBuilder after(DeploymentTask task) {

    }

    public void commit() {
        if (committed) {
            throw new IllegalStateException("Deployment task builder was already committed");
        }

    }
}
