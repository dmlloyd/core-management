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

import org.wildfly.core.management.processor.NameUtils;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class PolyAttributeValueTypeDescription extends AttributeValueTypeDescription {
    private final AttributeValueTypeDescription memberType;
    private final String singularPropertyName;
    private final String singularXmlName;
    private final String singularDmrName;

    PolyAttributeValueTypeDescription(final String name, final AttributeValueTypeDescription memberType, final String singularPropertyName, final String singularXmlName, final String singularDmrName) {
        super(name);
        this.memberType = memberType;
        this.singularPropertyName = singularPropertyName;
        this.singularXmlName = singularXmlName;
        this.singularDmrName = singularDmrName;
    }

    PolyAttributeValueTypeDescription(final String name, final AttributeValueTypeDescription memberType) {
        super(name);
        this.memberType = memberType;
        final String[] words = NameUtils.camelHumpsToWords(name);
        words[words.length - 1] = NameUtils.singularWord(words[words.length - 1]);
        singularPropertyName = NameUtils.wordsToVarCamelHumps(words);
        singularXmlName = singularDmrName = NameUtils.wordsToSeparatedLower(words, '-');
    }

    public AttributeValueTypeDescription getMemberType() {
        return memberType;
    }

    public String getSingularPropertyName() {
        return singularPropertyName;
    }

    public String getSingularXmlName() {
        return singularXmlName;
    }

    public String getSingularDmrName() {
        return singularDmrName;
    }
}
