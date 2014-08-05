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
public final class DeploymentOperation {

    public void achieve(DeploymentGoal goal) {

    }

    /**
     * Cancel any in-progress work for this deployment operation.
     */
    public void cancel() {

    }

    /**
     * Mark this operation as complete, meaning that no more goals will be requested to be achieved.  There may
     * be outstanding goals in progress when this method is called; they will continue to completion. Returns an
     * asynchronous task which can be executed to undo the deployment operation.
     *
     * @return the undeploy task
     */
    public UndeployTask complete() {

    }
}
