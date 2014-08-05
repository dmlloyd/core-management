/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
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

package org.wildfly.core.management.processor.model.value;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ReferenceAttributeValueTypeDescription extends AttributeValueTypeDescription {
    private final ExecutableElement declaringElement;
    private final boolean required;
    private final DeclaredType referenceType;
    private final boolean monitor;
    private final boolean list;
    private final DeclaredType targetType;

    public ReferenceAttributeValueTypeDescription(final String name, final ExecutableElement declaringElement, final boolean required, final DeclaredType referenceType, final boolean monitor, final boolean list, final DeclaredType targetType) {
        super(name);
        this.declaringElement = declaringElement;
        this.required = required;
        this.referenceType = referenceType;
        this.monitor = monitor;
        this.list = list;
        this.targetType = targetType;
    }

    public ExecutableElement getDeclaringElement() {
        return declaringElement;
    }

    public boolean isRequired() {
        return required;
    }

    public DeclaredType getReferenceType() {
        return referenceType;
    }

    public boolean isMonitor() {
        return monitor;
    }

    public boolean isList() {
        return list;
    }

    public DeclaredType getTargetType() {
        return targetType;
    }
}
