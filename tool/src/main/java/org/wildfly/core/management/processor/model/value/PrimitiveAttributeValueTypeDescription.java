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

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PrimitiveAttributeValueTypeDescription extends SimpleAttributeValueTypeDescription {

    PrimitiveAttributeValueTypeDescription(final String name) {
        super(name);
    }

    public static final PrimitiveAttributeValueTypeDescription STRING = new PrimitiveAttributeValueTypeDescription("String");
    public static final PrimitiveAttributeValueTypeDescription BOOLEAN = new PrimitiveAttributeValueTypeDescription("boolean");
    public static final PrimitiveAttributeValueTypeDescription BYTE = new PrimitiveAttributeValueTypeDescription("byte");
    public static final PrimitiveAttributeValueTypeDescription SHORT = new PrimitiveAttributeValueTypeDescription("short");
    public static final PrimitiveAttributeValueTypeDescription INT = new PrimitiveAttributeValueTypeDescription("int");
    public static final PrimitiveAttributeValueTypeDescription LONG = new PrimitiveAttributeValueTypeDescription("long");
    public static final PrimitiveAttributeValueTypeDescription CHAR = new PrimitiveAttributeValueTypeDescription("char");
    public static final PrimitiveAttributeValueTypeDescription FLOAT = new PrimitiveAttributeValueTypeDescription("float");
    public static final PrimitiveAttributeValueTypeDescription DOUBLE = new PrimitiveAttributeValueTypeDescription("double");
    public static final PrimitiveAttributeValueTypeDescription BIG_INTEGER = new PrimitiveAttributeValueTypeDescription("BigInteger");
    public static final PrimitiveAttributeValueTypeDescription BIG_DECIMAL = new PrimitiveAttributeValueTypeDescription("BigDecimal");
}
