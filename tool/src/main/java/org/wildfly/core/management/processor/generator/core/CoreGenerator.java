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

package org.wildfly.core.management.processor.generator.core;

import static org.jboss.jdeparser.JExpr.THIS;
import static org.jboss.jdeparser.JExprs.$v;
import static org.jboss.jdeparser.JMod.FINAL;
import static org.jboss.jdeparser.JMod.PRIVATE;
import static org.jboss.jdeparser.JMod.PUBLIC;
import static org.jboss.jdeparser.JTypes.$t;

import java.io.IOException;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.type.TypeMirror;

import org.jboss.jdeparser.FormatPreferences;
import org.jboss.jdeparser.JBlock;
import org.jboss.jdeparser.JClassDef;
import org.jboss.jdeparser.JClassDefSection;
import org.jboss.jdeparser.JDeparser;
import org.jboss.jdeparser.JFiler;
import org.jboss.jdeparser.JMethodDef;
import org.jboss.jdeparser.JSourceFile;
import org.jboss.jdeparser.JSources;
import org.jboss.jdeparser.JTypes;
import org.kohsuke.MetaInfServices;
import org.wildfly.core.management.AbstractNode;
import org.wildfly.core.management.AbstractResourceNode;
import org.wildfly.core.management.Node;
import org.wildfly.core.management.NodeBuilder;
import org.wildfly.core.management.Unresolved;
import org.wildfly.core.management.processor.NameUtils;
import org.wildfly.core.management.processor.apt.MessagerPlus;
import org.wildfly.core.management.processor.apt.ModelGenerator;
import org.wildfly.core.management.processor.model.AttributeDescription;
import org.wildfly.core.management.processor.model.NodeClassDescription;
import org.wildfly.core.management.processor.model.NodeMemberDescription;
import org.wildfly.core.management.processor.model.SystemDescription;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MetaInfServices(ModelGenerator.class)
public final class CoreGenerator implements ModelGenerator {

    public void generate(final ProcessingEnvironment env, final RoundEnvironment roundEnv, final MessagerPlus msg, final SystemDescription systemDescription) {
        msg.info("Generating model source files...");
        final JSources sources = JDeparser.createSources(JFiler.newInstance(env.getFiler()), new FormatPreferences());

        // generate all node classes
        for (Map.Entry<String, NodeClassDescription> entry : systemDescription.getNodeClassesByName().entrySet()) {
            final NodeClassDescription nodeClassDescription = entry.getValue();
            final String nodeClassName = entry.getKey();

            // unresolved interface
            generateUnresolvedInterface(env, sources, nodeClassDescription, nodeClassName);

            // main builder interface
            generateBuilderInterface(env, sources, nodeClassDescription, nodeClassName);

            // main builder implementation
            generateBuilderClass(env, sources, nodeClassDescription, nodeClassName);

            // resolved class
            generateResolvedClass(env, sources, nodeClassDescription, nodeClassName);

            // resolved class proxy

            // unresolved class

            // unresolved class proxy
        }

        if (! msg.isError()) try {
            sources.writeSources();
        } catch (IOException e) {
            msg.errorf("Failed to write sources: %s", e);
        }
    }

    private void generateUnresolvedInterface(final ProcessingEnvironment env, final JSources sources, final NodeClassDescription nodeClassDescription, final String nodeClassName) {
        final int dotIdx = nodeClassName.lastIndexOf('.');
        final String nodeClassPackage;
        if (dotIdx == -1) {
            nodeClassPackage = "";
        } else {
            nodeClassPackage = nodeClassName.substring(0, dotIdx);
        }
        final String unresolvedName = nodeClassDescription.getJavaName() + "Unresolved";

        final JSourceFile unresolvedFile = sources.createSourceFile(nodeClassPackage, unresolvedName);
        final JClassDef unresolvedInterface = unresolvedFile._interface(PUBLIC, unresolvedName);

        unresolvedFile._import($t(Unresolved.class));
        unresolvedInterface._extends($t(Unresolved.class).typeArg(nodeClassName));
        unresolvedInterface._extends($t(Node.class));

        for (NodeMemberDescription memberDescription : nodeClassDescription.getMembers()) {
            unresolvedInterface.blankLine();
            final String methodName = memberDescription.getExecutableElement().getSimpleName().toString();
            if (memberDescription instanceof AttributeDescription) {
                final AttributeDescription attributeDescription = (AttributeDescription) memberDescription;
                if (attributeDescription.isExpr()) {
                    unresolvedInterface.method(0, String.class, methodName);
                } else {
                    unresolvedInterface.method(0, JTypes.typeOf(memberDescription.getExecutableElement().getReturnType()), methodName);
                }
            } else {
                unresolvedInterface.method(0, JTypes.typeOf(memberDescription.getExecutableElement().getReturnType()), methodName);
            }
        }
    }

    private void generateBuilderInterface(final ProcessingEnvironment env, final JSources sources, final NodeClassDescription nodeClassDescription, final String nodeClassName) {
        final int dotIdx = nodeClassName.lastIndexOf('.');
        final String nodeClassPackage;
        if (dotIdx == -1) {
            nodeClassPackage = "";
        } else {
            nodeClassPackage = nodeClassName.substring(0, dotIdx);
        }
        final String builderName = nodeClassDescription.getJavaName() + "Builder";

        final JSourceFile builderFile = sources.createSourceFile(nodeClassPackage, builderName);
        final JClassDef builderInterface = builderFile._interface(PUBLIC, builderName);

        builderFile._import($t(NodeBuilder.class));
        builderInterface._extends($t(NodeBuilder.class));

        for (NodeMemberDescription memberDescription : nodeClassDescription.getMembers()) {
            builderInterface.blankLine();
            final String methodName = memberDescription.getExecutableElement().getSimpleName().toString();
            final String setterName;
            if (methodName.startsWith("is")) {
                setterName = "set" + methodName.substring(2);
            } else {
                setterName = "set" + methodName.substring(3);
            }
            if (memberDescription instanceof AttributeDescription) {
                final AttributeDescription attributeDescription = (AttributeDescription) memberDescription;
                if (attributeDescription.isExpr()) {
                    builderInterface.method(0, builderInterface.genericType(), setterName + "Expression").param(String.class, "value");
                }
                builderInterface.method(0, builderInterface.genericType(), setterName).param(JTypes.typeOf(memberDescription.getExecutableElement().getReturnType()), "value");
            } else {
                // uh
            }
        }
    }

    private void generateResolvedClass(final ProcessingEnvironment env, final JSources sources, final NodeClassDescription nodeClassDescription, final String nodeClassName) {
        final int dotIdx = nodeClassName.lastIndexOf('.');
        final String nodeClassPackage;
        if (dotIdx == -1) {
            nodeClassPackage = "";
        } else {
            nodeClassPackage = nodeClassName.substring(0, dotIdx);
        }
        final String resolvedName = nodeClassDescription.getJavaName() + "Impl";

        final JSourceFile resolvedFile = sources.createSourceFile(nodeClassPackage, resolvedName);
        resolvedFile.blankLine();
        final JClassDef resolvedClass = resolvedFile._class(PUBLIC, resolvedName);
        resolvedClass._extends($t(AbstractResourceNode.class));
        resolvedClass._implements($t(nodeClassName));
        final JClassDefSection fieldsSection = resolvedClass.section();
        fieldsSection.blankLine();
        fieldsSection.lineComment().text("=================");
        fieldsSection.lineComment().text("Field definitions");
        fieldsSection.lineComment().text("=================");
        final JClassDefSection constructorSection = resolvedClass.section();
        constructorSection.blankLine();
        constructorSection.lineComment().text("============");
        constructorSection.lineComment().text("Constructors");
        constructorSection.lineComment().text("============");
        final JClassDefSection getterSection = resolvedClass.section();
        getterSection.blankLine();
        getterSection.lineComment().text("==============");
        getterSection.lineComment().text("Getter methods");
        getterSection.lineComment().text("==============");

        constructorSection.blankLine();
        final JMethodDef constructor = constructorSection.constructor(0);
        resolvedFile._import($t(AbstractNode.class));
        resolvedFile._import($t(AbstractResourceNode.class));
        constructor.param(FINAL, $t(AbstractNode.class), "parent");
        constructor.param(FINAL, $t(String.class), "name");
        constructor.param(FINAL, $t(nodeClassDescription.getJavaName() + "BuilderImpl"), "builder");
        final JBlock constructorBody = constructor.body();
        // todo: name may be fixed or variable
        constructorBody.callSuper().arg($v("parent")).arg($v("name"));

        for (NodeMemberDescription memberDescription : nodeClassDescription.getMembers()) {
            final String methodName = memberDescription.getExecutableElement().getSimpleName().toString();
            final TypeMirror memberType = memberDescription.getExecutableElement().getReturnType();
            if (memberDescription instanceof AttributeDescription) {
                fieldsSection.blankLine();
                final AttributeDescription attributeDescription = (AttributeDescription) memberDescription;
                final String fieldName = NameUtils.fieldify(attributeDescription.getJavaName());
                fieldsSection.field(PRIVATE | FINAL, JTypes.typeOf(memberType), fieldName);

                getterSection.blankLine();
                final JMethodDef method = getterSection.method(PUBLIC | FINAL, JTypes.typeOf(memberType), methodName);
                final String docComment = env.getElementUtils().getDocComment(memberDescription.getExecutableElement());
                if (docComment != null) method.docComment().text(docComment);
                final JBlock body = method.body();
                body._return(THIS.$v(fieldName));

                constructorBody.assign(THIS.$v(fieldName), $v("builder").call("get" + attributeDescription.getJavaName()));
            } else {
                getterSection.method(0, JTypes.typeOf(memberType), methodName);
            }
        }
    }
}
