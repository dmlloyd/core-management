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

package org.wildfly.core.management.processor.apt;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.wildfly.core.management.Access;
import org.wildfly.core.management.Node;
import org.wildfly.core.management.ResourceLink;
import org.wildfly.core.management.ResourceNode;
import org.wildfly.core.management.annotation.Schema;
import org.wildfly.core.management.annotation.XmlName;
import org.wildfly.core.management.processor.NameUtils;
import org.wildfly.core.management.processor.model.AbstractNamedDescription;
import org.wildfly.core.management.processor.model.AttributeDescription;
import org.wildfly.core.management.processor.model.AttributeGroupDescription;
import org.wildfly.core.management.processor.model.NodeClassDescription;
import org.wildfly.core.management.processor.model.ResourceDescription;
import org.wildfly.core.management.processor.model.RootResourceDescription;
import org.wildfly.core.management.processor.model.SchemaDescription;
import org.wildfly.core.management.processor.model.SubResourceDescription;
import org.wildfly.core.management.processor.model.SystemDescription;
import org.wildfly.core.management.processor.model.value.ArrayAttributeValueTypeDescription;
import org.wildfly.core.management.processor.model.value.AttributeValueTypeDescription;
import org.wildfly.core.management.processor.model.value.ListAttributeValueTypeDescription;
import org.wildfly.core.management.processor.model.value.MapAttributeValueTypeDescription;
import org.wildfly.core.management.processor.model.value.PrimitiveAttributeValueTypeDescription;
import org.wildfly.core.management.processor.model.value.ReferenceAttributeValueTypeDescription;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class SchemaProcessor {

    static final List<Modifier> EXPECTED_FIELD_MODS = Arrays.asList(Modifier.STATIC, Modifier.FINAL, Modifier.PUBLIC);
    static final List<Modifier> EXPECTED_METHOD_MODS = Arrays.asList(Modifier.PUBLIC, Modifier.ABSTRACT);
    private final ProcessingEnvironment env;
    private final RoundEnvironment roundEnv;
    private final MessagerPlus msg;

    private final SystemDescription.Builder systemDescriptionBuilder;

    public SchemaProcessor(final ProcessingEnvironment env, final RoundEnvironment roundEnv, final MessagerPlus msg, final SystemDescription.Builder systemDescriptionBuilder) {
        this.env = env;
        this.roundEnv = roundEnv;
        this.msg = msg;
        this.systemDescriptionBuilder = systemDescriptionBuilder;
    }

    public ProcessingEnvironment getEnv() {
        return env;
    }

    public RoundEnvironment getRoundEnv() {
        return roundEnv;
    }

    /**
     * Process an element annotated with the {@link Schema @Schema} annotation.
     *
     * @param schemaAnnotatedElement the annotation interface element annotated with {@code @Schema}
     */
    public void processSchema(final TypeElement schemaAnnotatedElement) {
        AnnotationMirror retentionAnnotation = null;
        AnnotationMirror targetAnnotation = null;
        AnnotationMirror schemaAnnotation = null;
        AnnotationMirror deprecatedAnnotation = null;
        AnnotationMirror documentedAnnotation = null;
        AnnotationMirror inheritedAnnotation = null;
        for (AnnotationMirror annotationMirror : schemaAnnotatedElement.getAnnotationMirrors()) {
            final TypeElement annotationTypeElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
            final String annotationClassName = annotationTypeElement.getQualifiedName().toString();
            switch (annotationClassName) {
                case "org.wildfly.core.management.annotation.Schema": schemaAnnotation = annotationMirror; break;
                case "java.lang.annotation.Target": targetAnnotation = annotationMirror; break;
                case "java.lang.annotation.Retention": retentionAnnotation = annotationMirror; break;
                case "java.lang.annotation.Documented": documentedAnnotation = annotationMirror; break;
                case "java.lang.annotation.Inherited": inheritedAnnotation = annotationMirror; break;
                case "java.lang.Deprecated": deprecatedAnnotation = annotationMirror; break;

                default: {
                    if (annotationClassName.startsWith("org.wildfly.core.management.annotation")) {
                        msg.error(schemaAnnotatedElement, annotationMirror, "Annotation is not allowed on schema element");
                        break;
                    }
                    // ignore
                    break;
                }
            }
        }
        if (inheritedAnnotation != null) {
            msg.error(schemaAnnotatedElement, inheritedAnnotation, "Schema annotations may not be @Inherited");
        }
        if (documentedAnnotation == null) {
            msg.reqWarn(schemaAnnotatedElement, "Schema annotations should be @Documented");
        }
        if (retentionAnnotation != null) {
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : retentionAnnotation.getElementValues().entrySet()) {
                final AnnotationValue annotationValue = entry.getValue();
                final Object value = annotationValue.getValue();
                final String argName = entry.getKey().getSimpleName().toString();
                switch (argName) {
                    case "value": {
                        if (! (value instanceof VariableElement)) {
                            msg.errorf(schemaAnnotatedElement, schemaAnnotation, annotationValue, "Expected an enum RetentionPolicy value for value");
                            break;
                        }
                        RetentionPolicy retentionPolicy = RetentionPolicy.valueOf(((VariableElement) value).getSimpleName().toString());
                        if (retentionPolicy != RetentionPolicy.SOURCE) {
                            msg.reqWarn(schemaAnnotatedElement, retentionAnnotation, annotationValue, "Schema member should be annotated with a retention policy of SOURCE");
                        }
                        break;
                    }
                    default: {
                        msg.errorf(schemaAnnotatedElement, schemaAnnotation, annotationValue, "Unknown annotation argument %s", argName);
                        break;
                    }
                }
            }
        } else {
            msg.reqWarn(schemaAnnotatedElement, "Schema member should be annotated with a retention policy of SOURCE");
        }
        if (targetAnnotation != null) {
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : targetAnnotation.getElementValues().entrySet()) {
                final AnnotationValue annotationValue = entry.getValue();
                final Object value = annotationValue.getValue();
                final String argName = entry.getKey().getSimpleName().toString();
                switch (argName) {
                    case "value": {
                        if (value instanceof VariableElement) {
                            ElementType elementType;
                            elementType = ElementType.valueOf(((VariableElement) value).getSimpleName().toString());
                            if (elementType != ElementType.TYPE) {
                                msg.reqWarn(schemaAnnotatedElement, retentionAnnotation, annotationValue, "Schema member should be annotated with a single target of TYPE");
                            }
                        } else if (value instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<? extends AnnotationValue> values = (List<? extends AnnotationValue>) value;
                            if (values.size() == 1) {
                                ElementType elementType;
                                elementType = ElementType.valueOf(((VariableElement) values.get(0).getValue()).getSimpleName().toString());
                                if (elementType != ElementType.TYPE) {
                                    msg.reqWarn(schemaAnnotatedElement, retentionAnnotation, annotationValue, "Schema member should be annotated with a single target of TYPE");
                                }
                            } else {
                                msg.reqWarn(schemaAnnotatedElement, retentionAnnotation, annotationValue, "Schema member should be annotated with a single target of TYPE");
                            }
                        } else {
                            msg.errorf(schemaAnnotatedElement, schemaAnnotation, annotationValue, "Expected an enum TargetType value for value");
                            break;
                        }
                        if (! (value instanceof VariableElement)) {
                            break;
                        }
                        break;
                    }
                    default: {
                        msg.errorf(schemaAnnotatedElement, schemaAnnotation, annotationValue, "Unknown annotation argument %s", argName);
                        break;
                    }
                }
            }
        }

        if (schemaAnnotation != null) {
            if (schemaAnnotatedElement.getKind() != ElementKind.ANNOTATION_TYPE) {
                msg.error(schemaAnnotatedElement, schemaAnnotation, "Only an annotation type may be annotated as a Schema");
                return;
            }
            final SchemaDescription.Builder builder = SchemaDescription.Builder.create();
            if (deprecatedAnnotation != null) {
                // todo set deprecated?
            }
            processSchemaAnnotation(schemaAnnotatedElement, schemaAnnotation, builder);
            // now, find all elements for this one
            processAllSchemaMembers(schemaAnnotatedElement, schemaAnnotation, builder);
            if (! msg.isError()) {
                systemDescriptionBuilder.addSchema(builder.build());
            }
            return;
        }
        // no schema found - weird but not really a problem
        return;
    }

    private void processSchemaAnnotation(TypeElement schemaAnnotatedElement, AnnotationMirror schemaAnnotation, SchemaDescription.Builder builder) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : schemaAnnotation.getElementValues().entrySet()) {
            final AnnotationValue annotationValue = entry.getValue();
            final Object value = annotationValue.getValue();
            final String argName = entry.getKey().getSimpleName().toString();
            switch (argName) {
                case "schemaLocation": {
                    if (! (value instanceof String)) {
                        msg.errorf(schemaAnnotatedElement, schemaAnnotation, annotationValue, "Expected a String value for schemaLocation");
                        break;
                    }
                    final URI uri;
                    try {
                        uri = new URI(value.toString());
                    } catch (URISyntaxException e) {
                        msg.errorf(schemaAnnotatedElement, schemaAnnotation, annotationValue, "Namespace schema location '%s' is not valid: %s", value, e);
                        break;
                    }
                    final String path = uri.getPath();
                    if (path == null) {
                        msg.errorf(schemaAnnotatedElement, schemaAnnotation, annotationValue, "Namespace schema location '%s' does not have a path component", value);
                        break;
                    }
                    final String fileName = path.substring(path.lastIndexOf('/') + 1);
                    if (! fileName.endsWith(".xsd")) {
                        msg.errorf(schemaAnnotatedElement, schemaAnnotation, annotationValue, "Namespace schema location '%s' must specify a file name ending in \".xsd\"", value);
                        break;
                    }
                    builder.setSchemaLocation(uri);
                    builder.setSchemaFileName(fileName);
                    break;
                }
                case "version": {
                    if (! (value instanceof String)) {
                        msg.errorf(schemaAnnotatedElement, schemaAnnotation, annotationValue, "Expected a String value for version");
                        break;
                    }
                    builder.setVersion(value.toString());
                    break;
                }
                case "kind": {
                    if (! (value instanceof VariableElement)) {
                        msg.errorf(schemaAnnotatedElement, schemaAnnotation, annotationValue, "Expected an enum Kind value for kind");
                        break;
                    }
                    builder.setKind(Schema.Kind.valueOf(((VariableElement) value).getSimpleName().toString()));
                    break;
                }
                case "namespace": {
                    if (! (value instanceof String)) {
                        msg.errorf(schemaAnnotatedElement, schemaAnnotation, annotationValue, "Expected a String value for namespace");
                        break;
                    }
                    builder.setNamespace(value.toString());
                    break;
                }
                case "compatibilityNamespaces": {
                    if (! (value instanceof List)) {
                        msg.errorf(schemaAnnotatedElement, schemaAnnotation, annotationValue, "Expected an array of Strings value for compatibilityNamespaces");
                        break;
                    }
                    @SuppressWarnings("unchecked")
                    final List<? extends AnnotationValue> list = (List<? extends AnnotationValue>) value;
                    for (int i = 0; i < list.size(); i++) {
                        final AnnotationValue val = list.get(i);
                        final Object value1 = val.getValue();
                        if (! (value1 instanceof String)) {
                            msg.errorf(schemaAnnotatedElement, schemaAnnotation, annotationValue, "Element %d of compatibilityNamespaces list is not a String", Integer.valueOf(i));
                            continue;
                        }
                        builder.addCompatNamespace((String) value1);
                    }
                    break;
                }
                default: {
                    msg.errorf(schemaAnnotatedElement, schemaAnnotation, annotationValue, "Unknown annotation argument %s", argName);
                    break;
                }
            }
        }
        builder.setSchemaElement(schemaAnnotatedElement);
        builder.setXmlNamespace((builder.getKind() == Schema.Kind.SYSTEM ? "sys:" : "ext:") + builder.getNamespace() + ":" + builder.getVersion());
    }

    private void processAllSchemaMembers(final TypeElement schemaAnnotatedElement, final AnnotationMirror schemaAnnotation, final SchemaDescription.Builder schemaBuilder) {
        Set<? extends Element> schemaElements = roundEnv.getElementsAnnotatedWith(schemaAnnotatedElement);
        for (Element itemElement : schemaElements) {
            if (! (itemElement instanceof TypeElement) || itemElement.getKind() != ElementKind.INTERFACE) {
                msg.errorf(itemElement, "Schema member items must be interfaces");
                continue;
            }
            TypeElement itemTypeElement = (TypeElement) itemElement;

            AnnotationMirror rootResourceAnnotation = null;
            AnnotationMirror xmlNameAnnotation = null;

            for (AnnotationMirror annotationMirror : itemTypeElement.getAnnotationMirrors()) {
                final TypeElement annotationTypeElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
                final String annotationClassName = annotationTypeElement.getQualifiedName().toString();
                switch (annotationClassName) {
                    case "org.wildfly.core.management.annotation.RootResource": rootResourceAnnotation = annotationMirror; break;
                    case "org.wildfly.core.management.annotation.XmlName": xmlNameAnnotation = annotationMirror; break;
                    case "org.wildfly.core.management.annotation.Provides": /* todo */ break;
                    default: {
                        if (annotationClassName.startsWith("org.wildfly.core.management.annotation")) {
                            msg.error(itemElement, annotationMirror, "Annotation " + annotationClassName + " is not allowed on root resource");
                            break;
                        }
                        // ignore
                        break;
                    }
                }
            }

            if (rootResourceAnnotation != null) {
                RootResourceDescription.Builder builder = RootResourceDescription.Builder.create();
                processRootResource(itemTypeElement, rootResourceAnnotation, xmlNameAnnotation, builder);
                if (! msg.isError()) {
                    final RootResourceDescription rootResourceDescription = builder.build();
                    schemaBuilder.addRootResourceDescription(rootResourceDescription);
                }
            } else {
                msg.errorf(schemaAnnotatedElement, schemaAnnotation, "Element is part of schema %s but is not a valid schema member type", schemaAnnotation);

            }
        }
    }

    private void processRootResource(final TypeElement resourceElement, final AnnotationMirror rootResourceAnnotation, final AnnotationMirror xmlNameAnnotation, final RootResourceDescription.Builder builder) {
        processResource(resourceElement, xmlNameAnnotation, builder);
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : rootResourceAnnotation.getElementValues().entrySet()) {
            final AnnotationValue annotationValue = entry.getValue();
            final Object value = annotationValue.getValue();
            final String argName = entry.getKey().getSimpleName().toString();
            switch (argName) {
                case "type": {
                    if (! (value instanceof String)) {
                        msg.error(resourceElement, rootResourceAnnotation, annotationValue, "Expected String value for type");
                        break;
                    }
                    // todo not sure...
                    break;
                }
                case "name": {
                    if (! (value instanceof String)) {
                        msg.error(resourceElement, rootResourceAnnotation, annotationValue, "Expected String value for name");
                        break;
                    }
                    // todo not sure...
                    break;
                }
                default: {
                    msg.errorf(resourceElement, rootResourceAnnotation, annotationValue, "Unknown annotation argument %s", argName);
                    break;
                }
            }
        }
    }

    private void processResource(final TypeElement resourceElement, final AnnotationMirror xmlNameAnnotation, final ResourceDescription.Builder builder) {
        if (resourceElement.getKind() != ElementKind.INTERFACE) {
            msg.errorf(resourceElement, "Only interfaces may be resources");
            return;
        }
        final Elements elements = env.getElementUtils();
        final TypeMirror nodeType = elements.getTypeElement(ResourceNode.class.getName()).asType();
        if (! env.getTypeUtils().isAssignable(resourceElement.asType(), nodeType)) {
            msg.errorf(resourceElement, "Type '%s' must extend %s", resourceElement.getQualifiedName(), nodeType);
        }
        if (xmlNameAnnotation != null) {
            processXmlNameAnnotation(resourceElement, xmlNameAnnotation, builder);
        }
        String resourceInterfaceName = resourceElement.getSimpleName().toString();
        if (resourceInterfaceName.endsWith("Resource")) {
            builder.setJavaName(resourceInterfaceName.substring(0, resourceInterfaceName.length() - 8));
        } else {
            builder.setJavaName(resourceInterfaceName);
        }
        builder.setNodeClassDescription(getOrCreateNodeClass(resourceElement));
    }

    private void processNodeClass(final TypeElement nodeClassElement, final NodeClassDescription.Builder builder) {
        processNamedItem(nodeClassElement, builder);
        final Elements elements = env.getElementUtils();
        final TypeMirror nodeType = elements.getTypeElement(Node.class.getName()).asType();
        if (! env.getTypeUtils().isAssignable(nodeClassElement.asType(), nodeType)) {
            msg.errorf(nodeClassElement, "Type '%s' must extend %s", nodeClassElement.getQualifiedName(), nodeType);
        }
        if (nodeClassElement.getTypeParameters().size() > 0) {
            msg.errorf(nodeClassElement, "Node interface '%s' cannot have type parameters", nodeClassElement.getQualifiedName());
        }
        builder.setTypeElement(nodeClassElement);
        final TypeMirror rawSuperclassType = nodeClassElement.getSuperclass();
        if (rawSuperclassType instanceof DeclaredType) {
            final DeclaredType superclassDeclaredType;
            superclassDeclaredType = (DeclaredType) rawSuperclassType;
            final TypeElement superclass = (TypeElement) superclassDeclaredType.asElement();
            builder.setSuperClass(getOrCreateNodeClass(superclass));
        }
        for (Element element : nodeClassElement.getEnclosedElements()) {
            if (element instanceof VariableElement) {
                if (element.getKind() != ElementKind.FIELD) {
                    msg.errorf(element, "Expected field, found element of type %s", element.getKind());
                    continue;
                } else if (!element.getModifiers().containsAll(EXPECTED_FIELD_MODS)) {
                    msg.errorf(element, "Expected public static final field, found modifiers %s", element.getModifiers());
                    continue;
                }
                // fields are unused right now
                continue;
            } else if (element instanceof ExecutableElement) {
                if (element.getKind() != ElementKind.METHOD) {
                    msg.errorf(element, "Expected method, found element of type %s", element.getKind());
                    continue;
                } else if (! element.getModifiers().containsAll(EXPECTED_METHOD_MODS)) {
                    msg.errorf(element, "Expected public abstract method, found modifiers %s", element.getModifiers());
                    continue;
                }
                processNodeClassElement((ExecutableElement) element, builder);
            } else if (element instanceof TypeElement) {
                // unused right now
                continue;
            } else {
                msg.error(element, "Encountered unexpected/unknown element");
                continue;
            }
        }
    }

    private NodeClassDescription getOrCreateNodeClass(final TypeElement nodeClassElement) {
        NodeClassDescription description = systemDescriptionBuilder.getNodeClass(nodeClassElement);
        if (description == null) {
            final NodeClassDescription.Builder builder = NodeClassDescription.Builder.create();
            processNodeClass(nodeClassElement, builder);
            if (msg.isError()) {
                description = null;
            } else {
                description = builder.build();
                systemDescriptionBuilder.addNodeClass(description);
            }
        }
        return description;
    }

    private void processNodeClassElement(final ExecutableElement element, final NodeClassDescription.Builder builder) {
        final String elementName = element.getSimpleName().toString();
        String javaName;
        if (elementName.startsWith("is")) {
            javaName = elementName.substring(2);
        } else if (elementName.startsWith("get")) {
            javaName = elementName.substring(3);
        } else {
            msg.errorf(element, "Method name \"%s\" is not valid for a management node interface property (expected getXxx or isXxx)", elementName);
            return;
        }
        // set a default singular name; it may be overridden via a name="" annotation property
        // this value won't be used except for sub-resources and collections/maps/etc.
        final String[] words = NameUtils.camelHumpsToWords(javaName);
        if (words.length == 0) {
            msg.errorf(element, "Invalid empty name given for node class element");
            return;
        }
        words[words.length - 1] = NameUtils.singularWord(words[words.length - 1]);
        String singularJavaName = NameUtils.wordsToVarCamelHumps(words);

        // analyze return type
        final TypeMirror returnType = element.getReturnType();
        if (returnType instanceof PrimitiveType) {
            // simple type
        } else if (returnType instanceof DeclaredType) {
            // validate generics
            final DeclaredType declaredType = (DeclaredType) returnType;
            final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            final TypeElement typeElement = (TypeElement) declaredType.asElement();
            if (typeElement.getTypeParameters().size() != typeArguments.size()) {
                msg.error(element, "Type argument count must match declared type's type parameter count");
                return;
            }
        } else if (returnType instanceof ArrayType) {
            msg.errorf(element, "Array types are not allowed; use a List<%s> instead", ((ArrayType) returnType).getComponentType());
            return;
        } else {
            msg.errorf(element, "Unsupported management node interface property return type %s", returnType);
            return;
        }

        // examine annotations
        AnnotationMirror subResourceAnnotation = null;
        AnnotationMirror attributeGroupAnnotation = null;
        AnnotationMirror attributeAnnotation = null;
        AnnotationMirror xmlNameAnnotation = null;
        AnnotationMirror virtualAnnotation = null;
        AnnotationMirror defaultAnnotation = null;
        AnnotationMirror defaultBooleanAnnotation = null;
        AnnotationMirror defaultIntAnnotation = null;
        AnnotationMirror defaultLongAnnotation = null;
        AnnotationMirror xmlRenderAnnotation = null;
        AnnotationMirror enumeratedAnnotation = null;
        AnnotationMirror requiredAnnotation = null;

        for (final AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            final TypeElement annotationTypeElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
            final String annotationTypeName = annotationTypeElement.getQualifiedName().toString();
            switch (annotationTypeName) {
                // allowed
                case "org.wildfly.core.management.annotation.SubResource": subResourceAnnotation = annotationMirror; break;
                case "org.wildfly.core.management.annotation.AttributeGroup": attributeGroupAnnotation = annotationMirror; break;
                case "org.wildfly.core.management.annotation.Attribute": attributeAnnotation = annotationMirror; break;
                case "org.wildfly.core.management.annotation.XmlName": xmlNameAnnotation = annotationMirror; break;
                case "org.wildfly.core.management.annotation.Virtual": virtualAnnotation = annotationMirror; break;
                case "org.wildfly.core.management.annotation.Default": defaultAnnotation = annotationMirror; break;
                case "org.wildfly.core.management.annotation.DefaultBoolean": defaultBooleanAnnotation = annotationMirror; break;
                case "org.wildfly.core.management.annotation.DefaultInt": defaultIntAnnotation = annotationMirror; break;
                case "org.wildfly.core.management.annotation.DefaultLong": defaultLongAnnotation = annotationMirror; break;
                case "org.wildfly.core.management.annotation.XmlRender": xmlRenderAnnotation = annotationMirror; break;
                case "org.wildfly.core.management.annotation.Enumerated": enumeratedAnnotation = annotationMirror; break;
                case "org.wildfly.core.management.annotation.Required": requiredAnnotation = annotationMirror; break;

                // forbidden
                default: {
                    if (annotationTypeName.startsWith("org.wildfly.core.management.annotation")) {
                        msg.error(element, annotationMirror, "Annotation is not allowed on attribute property");
                    }
                    break;
                }
            }
        }

        // report invalid annotation combinations

        if (subResourceAnnotation != null) {
            if (attributeGroupAnnotation != null) {
                msg.errorf(element, attributeGroupAnnotation, "%s is not allowed with %s", attributeGroupAnnotation, subResourceAnnotation);
            }
            if (attributeAnnotation != null) {
                msg.errorf(element, attributeAnnotation, "%s is not allowed with %s", attributeAnnotation, subResourceAnnotation);
            }
            if (virtualAnnotation != null) {
                msg.errorf(element, virtualAnnotation, "%s is not allowed with %s", virtualAnnotation, subResourceAnnotation);
            }
            if (defaultAnnotation != null) {
                msg.errorf(element, defaultAnnotation, "%s is not allowed with %s", defaultAnnotation, subResourceAnnotation);
            }
            if (defaultBooleanAnnotation != null) {
                msg.errorf(element, defaultBooleanAnnotation, "%s is not allowed with %s", defaultBooleanAnnotation, subResourceAnnotation);
            }
            if (defaultIntAnnotation != null) {
                msg.errorf(element, defaultIntAnnotation, "%s is not allowed with %s", defaultIntAnnotation, subResourceAnnotation);
            }
            if (defaultLongAnnotation != null) {
                msg.errorf(element, defaultLongAnnotation, "%s is not allowed with %s", defaultLongAnnotation, subResourceAnnotation);
            }
        } else if (attributeGroupAnnotation != null) {
            if (virtualAnnotation != null) {
                msg.errorf(element, virtualAnnotation, "%s is not allowed with %s", virtualAnnotation, subResourceAnnotation);
            }
            if (defaultAnnotation != null) {
                msg.errorf(element, defaultAnnotation, "%s is not allowed with %s", defaultAnnotation, subResourceAnnotation);
            }
            if (defaultBooleanAnnotation != null) {
                msg.errorf(element, defaultBooleanAnnotation, "%s is not allowed with %s", defaultBooleanAnnotation, subResourceAnnotation);
            }
            if (defaultIntAnnotation != null) {
                msg.errorf(element, defaultIntAnnotation, "%s is not allowed with %s", defaultIntAnnotation, subResourceAnnotation);
            }
            if (defaultLongAnnotation != null) {
                msg.errorf(element, defaultLongAnnotation, "%s is not allowed with %s", defaultLongAnnotation, subResourceAnnotation);
            }
        }

        // now, decide the right action to take

        if (subResourceAnnotation != null) {
            // it's a sub-resource
            // perform more specific return type validation
            if (!(returnType instanceof DeclaredType)) {
                msg.errorf(element, "Unsupported sub-resource type %s", returnType);
                return;
            }

            final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = subResourceAnnotation.getElementValues();
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                final AnnotationValue annotationValue = entry.getValue();
                final Object value = annotationValue.getValue();
                final String argName = entry.getKey().getSimpleName().toString();
                switch (argName) {
                    case "requiresUniqueProvider": {
                        // todo
                        break;
                    }
                    case "type": {
                        // todo
                        break;
                    }
                    case "addLevel": {
                        // todo
                        break;
                    }
                    default: {
                        msg.errorf(element, subResourceAnnotation, annotationValue, "Unknown annotation argument %s", argName);
                        break;
                    }
                }
            }

            // determine if it's a singleton "squatter" or a map
            final DeclaredType childDeclaredType;
            final TypeElement childTypeElement;

            final DeclaredType returnDeclaredType = (DeclaredType) returnType;
            final TypeElement returnTypeElement = (TypeElement) returnDeclaredType.asElement();
            if (!returnTypeElement.getQualifiedName().toString().equals(Map.class.getName())) {
                // it's a singleton "squatter"
                childTypeElement = returnTypeElement;
            } else {
                // it's a normal map
                final List<? extends TypeMirror> typeArgs = returnDeclaredType.getTypeArguments();
                if (typeArgs.size() != 2) {
                    msg.error(returnTypeElement, "Map return type must have two type arguments");
                    return;
                }

                final TypeMirror keyTypeMirror = typeArgs.get(0);
                final TypeMirror valueTypeMirror = typeArgs.get(1);
                if (! (keyTypeMirror instanceof DeclaredType) || ! ((TypeElement) ((DeclaredType) keyTypeMirror).asElement()).getQualifiedName().toString().equals(String.class.getName())) {
                    msg.error(returnTypeElement, "Map return type first type argument must be String");
                    // can still proceed to report more errors though, just pretend it was String all along
                }
                if (! (valueTypeMirror instanceof DeclaredType)) {
                    msg.error(returnTypeElement, "Map return type second type argument must be a declared type");
                    return;
                }
                childDeclaredType = (DeclaredType) valueTypeMirror;
                childTypeElement = (TypeElement) childDeclaredType.asElement();

                // now determine if the child type is a concrete resource or a general superclass

            }

            final SubResourceDescription.Builder srBuilder = SubResourceDescription.Builder.create();
            srBuilder.setJavaName(javaName);
            srBuilder.setDmrName(NameUtils.xmlify(javaName));
            srBuilder.setXmlName(NameUtils.xmlify(javaName));
            srBuilder.setExecutableElement(element);

            processResource(childTypeElement, xmlNameAnnotation, srBuilder);

            if (xmlNameAnnotation != null) {
                processXmlNameAnnotation(element, xmlNameAnnotation, srBuilder);
            }

            if (! msg.isError()) {
                builder.addMember(srBuilder.build());
            }

        } else if (attributeGroupAnnotation != null) {
            // it's an attribute group
            // perform more specific return type validation
            if (!(returnType instanceof DeclaredType)) {
                msg.errorf(element, "Unsupported attribute group type %s", returnType);
                return;
            }
            final TypeElement returnTypeElement = (TypeElement) ((DeclaredType) returnType).asElement();
            if (returnTypeElement.getQualifiedName().toString().startsWith("java.")) {
                // indication that the type is probably invalid
                msg.error(element, "JDK types are not supported as attribute groups");
                return;
            }
            if (returnTypeElement.getKind() != ElementKind.INTERFACE) {
                msg.error(element, "Attribute group types must be interfaces");
                return;
            }

            AttributeGroupDescription.Builder agBuilder = AttributeGroupDescription.Builder.create();
            // start with Java name
            agBuilder.setJavaName(javaName);
            agBuilder.setDmrName(NameUtils.xmlify(javaName));
            agBuilder.setXmlName(NameUtils.xmlify(javaName));
            agBuilder.setExecutableElement(element);

            agBuilder.setNodeClassDescription(getOrCreateNodeClass((TypeElement) ((DeclaredType) returnType).asElement()));

            final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = attributeGroupAnnotation.getElementValues();
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                final AnnotationValue annotationValue = entry.getValue();
                final Object value = annotationValue.getValue();
                final String argName = entry.getKey().getSimpleName().toString();
                switch (argName) {
                    case "name": {
                        if (! (value instanceof String)) {
                            msg.errorf(element, attributeGroupAnnotation, annotationValue, "Expected a String value for name");
                            break;
                        }
                        agBuilder.setDmrName((String) value);
                        break;
                    }
                    case "required": {
                        if (! (value instanceof Boolean)) {
                            msg.errorf(element, attributeGroupAnnotation, annotationValue, "Expected a boolean value for required");
                            break;
                        }
                        agBuilder.setRequired(((Boolean) value).booleanValue());
                        break;
                    }
                    case "anonymous": {
                        if (! (value instanceof Boolean)) {
                            msg.errorf(element, attributeGroupAnnotation, annotationValue, "Expected a boolean value for anonymous");
                            break;
                        }
                        agBuilder.setPrefixAddress(! ((Boolean) value).booleanValue());
                        break;
                    }
                    default: {
                        msg.errorf(element, attributeGroupAnnotation, annotationValue, "Unknown annotation argument %s", argName);
                        break;
                    }
                }
            }
            if (xmlNameAnnotation != null) {
                processXmlNameAnnotation(element, xmlNameAnnotation, agBuilder);
            }

            if (! msg.isError()) {
                builder.addMember(agBuilder.build());
            }
        } else if (attributeAnnotation != null) {
            AttributeDescription.Builder aBuilder = AttributeDescription.Builder.create();
            aBuilder.setExecutableElement(element);
            aBuilder.setJavaName(javaName);
            aBuilder.setDmrName(NameUtils.xmlify(javaName));
            aBuilder.setXmlName(NameUtils.xmlify(javaName));

            final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = attributeAnnotation.getElementValues();
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                final AnnotationValue annotationValue = entry.getValue();
                final Object value = annotationValue.getValue();
                final String argName = entry.getKey().getSimpleName().toString();
                switch (argName) {
                    case "access": {
                        aBuilder.setAccess(Access.valueOf(((VariableElement) value).getSimpleName().toString()));
                        break;
                    }
                    case "name": {
                        javaName = value.toString();
                        aBuilder.setJavaName(javaName);
                        aBuilder.setDmrName(NameUtils.xmlify(javaName));
                        aBuilder.setXmlName(NameUtils.xmlify(javaName));
                        break;
                    }
                    case "changeRunLevel": {
                        break;
                    }
                    case "expr": {
                        break;
                    }
                    case "validators": {
                        break;
                    }
                    default: {
                        msg.errorf(element, attributeAnnotation, annotationValue, "Unknown annotation argument %s", argName);
                        break;
                    }
                }
            }

            if (xmlNameAnnotation != null) {
                processXmlNameAnnotation(element, xmlNameAnnotation, aBuilder);
            }

            final AttributeValueTypeDescription valueTypeDescription = attributeValueTypeOf(element, returnType);

            if (! msg.isError()) {
                aBuilder.setValueType(valueTypeDescription);
                builder.addMember(aBuilder.build());
            }
        } else {
            msg.error(element, "Superfluous attribute property");
        }

    }

    private AttributeValueTypeDescription attributeValueTypeOf(Element element, TypeMirror typeMirror) {
        switch (typeMirror.getKind()) {
            case BOOLEAN: return PrimitiveAttributeValueTypeDescription.BOOLEAN;
            case BYTE:    return PrimitiveAttributeValueTypeDescription.BYTE;
            case SHORT:   return PrimitiveAttributeValueTypeDescription.SHORT;
            case INT:     return PrimitiveAttributeValueTypeDescription.INT;
            case LONG:    return PrimitiveAttributeValueTypeDescription.LONG;
            case CHAR:    return PrimitiveAttributeValueTypeDescription.CHAR;
            case FLOAT:   return PrimitiveAttributeValueTypeDescription.FLOAT;
            case DOUBLE:  return PrimitiveAttributeValueTypeDescription.DOUBLE;
            case ARRAY: {
                return new ArrayAttributeValueTypeDescription("FIXME", attributeValueTypeOf(element, ((ArrayType) typeMirror).getComponentType()));
            }
            case DECLARED: {
                final DeclaredType declaredType = (DeclaredType) typeMirror;
                final TypeElement typeElement = (TypeElement) declaredType.asElement();
                final String typeName = typeElement.getQualifiedName().toString();
                if (typeName.equals(String.class.getName())) {
                    return PrimitiveAttributeValueTypeDescription.STRING;
                } else if (typeName.equals(BigInteger.class.getName())) {
                    return PrimitiveAttributeValueTypeDescription.BIG_INTEGER;
                } else if (typeName.equals(BigDecimal.class.getName())) {
                    return PrimitiveAttributeValueTypeDescription.BIG_DECIMAL;
                } else if (typeName.equals(ResourceLink.class.getName())) {
                    // resource link
                    final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                    if (typeArguments.size() != 1) {
                        msg.errorf(element, "Wrong number of type arguments for %s (expected 1)", ResourceLink.class);
                        return null;
                    }
                    // Determine type
                    final TypeMirror valueType = typeArguments.get(0);
                    if (valueType instanceof WildcardType) {
                        // nonspecific

                    }
                    return new ReferenceAttributeValueTypeDescription("FIXME", (ExecutableElement) element, true, declaredType, false, false, declaredType);
                } else if (typeName.equals(Map.class.getName())) {
                    // it's a map
                    final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                    if (typeArguments.size() != 2) {
                        msg.errorf(element, "Wrong number of type arguments for %s (expected 2)", Map.class);
                        return null;
                    }
                    // Determine type
                    final Types types = env.getTypeUtils();
                    final Elements elements = env.getElementUtils();

                    if (! types.isSameType(elements.getTypeElement(String.class.getName()).asType(), typeArguments.get(0))) {
                        msg.errorf(element, "First type argument must be %s for %s", String.class, Map.class);
                        return null;
                    }
                    final TypeMirror valueType = typeArguments.get(1);
                    return new MapAttributeValueTypeDescription("FIXME", attributeValueTypeOf(element, valueType));
                } else if (typeName.equals(List.class.getName())) {
                    // it's a list
                    final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                    if (typeArguments.size() != 1) {
                        msg.errorf(element, "Wrong number of type arguments for %s (expected 1)", List.class);
                        return null;
                    }
                    // Determine type
                    final TypeMirror valueType = typeArguments.get(0);
                    return new ListAttributeValueTypeDescription("FIXME", attributeValueTypeOf(element, valueType));
                } else {
                    msg.errorf(element, "Unsupported type %s for attribute", typeMirror);
                    return null;
                }
            }

            default: {
                msg.error(element, "Invalid value type");
                return null;
            }
        }
    }

    private void processNamedItem(final TypeElement namedItemElement, final AbstractNamedDescription.Builder builder) {
        String javaName = builder.getJavaName();
        if (javaName == null) {
            javaName = namedItemElement.getSimpleName().toString();
        }
        builder.setJavaName(javaName);
        String dmrName = builder.getDmrName();
        if (dmrName == null) {
            dmrName = NameUtils.xmlify(javaName);
        }
        builder.setDmrName(dmrName);
        getXmlName(namedItemElement, builder);
    }

    private void processXmlNameAnnotation(final Element element, final AnnotationMirror annotationMirror, final AbstractNamedDescription.Builder builder) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
            final AnnotationValue annotationValue = entry.getValue();
            final Object value = annotationValue.getValue();
            switch (entry.getKey().getSimpleName().toString()) {
                case "value": {
                    if (! (value instanceof String)) {
                        msg.error(element, annotationMirror, annotationValue, "Expected String value for value");
                        break;
                    }
                    builder.setXmlName((String) value);
                    builder.setDmrName((String) value);
                    break;
                }
                default: {
                    msg.error(element, annotationMirror, annotationValue, "Unknown annotation argument");
                    break;
                }
            }
        }
    }

    private void getXmlName(final TypeElement element, final AbstractNamedDescription.Builder builder) {
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            final TypeElement annotationTypeElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
            if (annotationTypeElement.getQualifiedName().toString().equals(XmlName.class.getName())) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
                    final AnnotationValue annotationValue = entry.getValue();
                    final Object value = annotationValue.getValue();
                    switch (entry.getKey().getSimpleName().toString()) {
                        case "value": {
                            if (! (value instanceof String)) {
                                msg.error(element, annotationMirror, annotationValue, "Expected String value for value");
                                break;
                            }
                            builder.setXmlName((String) value);
                            break;
                        }
                        default: {
                            msg.error(element, annotationMirror, annotationValue, "Unknown annotation argument");
                            break;
                        }
                    }
                }
            }
        }
    }
}
