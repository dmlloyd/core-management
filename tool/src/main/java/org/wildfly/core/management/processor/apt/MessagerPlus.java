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

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class MessagerPlus {
    private final Messager messager;
    private boolean error;

    public MessagerPlus(final Messager messager) {
        this.messager = messager;
    }

    private static String msgString(String orig) {
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final StackTraceElement element = stackTrace[2];
        return String.format("%s (reported by %s:%d)", orig, element.getFileName(), Integer.valueOf(element.getLineNumber()));
    }

    // info

    public void info(final String msg) {
        messager.printMessage(Diagnostic.Kind.NOTE, msgString(msg));
    }

    public void info(final Element element, final String msg) {
        messager.printMessage(Diagnostic.Kind.NOTE, msgString(msg), element);
    }

    public void info(final Element element, AnnotationMirror annotationMirror, final String msg) {
        messager.printMessage(Diagnostic.Kind.NOTE, msgString(msg), element, annotationMirror);
    }

    public void info(final Element element, AnnotationMirror annotationMirror, AnnotationValue value, final String msg) {
        messager.printMessage(Diagnostic.Kind.NOTE, msgString(msg), element, annotationMirror, value);
    }

    public void infof(final String fmt, final Object... args) {
        messager.printMessage(Diagnostic.Kind.NOTE, msgString(String.format(fmt, args)));
    }

    public void infof(final Element element, final String fmt, final Object... args) {
        messager.printMessage(Diagnostic.Kind.NOTE, msgString(String.format(fmt, args)), element);
    }

    public void infof(final Element element, AnnotationMirror annotationMirror, final String fmt, final Object... args) {
        messager.printMessage(Diagnostic.Kind.NOTE, msgString(String.format(fmt, args)), element, annotationMirror);
    }

    public void infof(final Element element, AnnotationMirror annotationMirror, AnnotationValue value, final String fmt, final Object... args) {
        messager.printMessage(Diagnostic.Kind.NOTE, msgString(String.format(fmt, args)), element, annotationMirror, value);
    }

    // optWarn

    public void optWarn(final String msg) {
        messager.printMessage(Diagnostic.Kind.WARNING, msgString(msg));
    }

    public void optWarn(final Element element, final String msg) {
        messager.printMessage(Diagnostic.Kind.WARNING, msgString(msg), element);
    }

    public void optWarn(final Element element, AnnotationMirror annotationMirror, final String msg) {
        messager.printMessage(Diagnostic.Kind.WARNING, msgString(msg), element, annotationMirror);
    }

    public void optWarn(final Element element, AnnotationMirror annotationMirror, AnnotationValue value, final String msg) {
        messager.printMessage(Diagnostic.Kind.WARNING, msgString(msg), element, annotationMirror, value);
    }

    public void optWarnf(final String fmt, final Object... args) {
        messager.printMessage(Diagnostic.Kind.WARNING, msgString(String.format(fmt, args)));
    }

    public void optWarnf(final Element element, final String fmt, final Object... args) {
        messager.printMessage(Diagnostic.Kind.WARNING, msgString(String.format(fmt, args)), element);
    }

    public void optWarnf(final Element element, AnnotationMirror annotationMirror, final String fmt, final Object... args) {
        messager.printMessage(Diagnostic.Kind.WARNING, msgString(String.format(fmt, args)), element, annotationMirror);
    }

    public void optWarnf(final Element element, AnnotationMirror annotationMirror, AnnotationValue value, final String fmt, final Object... args) {
        messager.printMessage(Diagnostic.Kind.WARNING, msgString(String.format(fmt, args)), element, annotationMirror, value);
    }

    // reqWarn

    public void reqWarn(final String msg) {
        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, msgString(msg));
    }

    public void reqWarn(final Element element, final String msg) {
        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, msgString(msg), element);
    }

    public void reqWarn(final Element element, AnnotationMirror annotationMirror, final String msg) {
        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, msgString(msg), element, annotationMirror);
    }

    public void reqWarn(final Element element, AnnotationMirror annotationMirror, AnnotationValue value, final String msg) {
        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, msgString(msg), element, annotationMirror, value);
    }

    public void reqWarnf(final String fmt, final Object... args) {
        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, msgString(String.format(fmt, args)));
    }

    public void reqWarnf(final Element element, final String fmt, final Object... args) {
        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, msgString(String.format(fmt, args)), element);
    }

    public void reqWarnf(final Element element, AnnotationMirror annotationMirror, final String fmt, final Object... args) {
        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, msgString(String.format(fmt, args)), element, annotationMirror);
    }

    public void reqWarnf(final Element element, AnnotationMirror annotationMirror, AnnotationValue value, final String fmt, final Object... args) {
        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, msgString(String.format(fmt, args)), element, annotationMirror, value);
    }

    // error

    public void error(final String msg) {
        error = true;
        messager.printMessage(Diagnostic.Kind.ERROR, msgString(msg));
    }

    public void error(final Element element, final String msg) {
        error = true;
        messager.printMessage(Diagnostic.Kind.ERROR, msgString(msg), element);
    }

    public void error(final Element element, AnnotationMirror annotationMirror, final String msg) {
        error = true;
        messager.printMessage(Diagnostic.Kind.ERROR, msgString(msg), element, annotationMirror);
    }

    public void error(final Element element, AnnotationMirror annotationMirror, AnnotationValue value, final String msg) {
        error = true;
        messager.printMessage(Diagnostic.Kind.ERROR, msgString(msg), element, annotationMirror, value);
    }

    public void errorf(final String fmt, final Object... args) {
        error = true;
        messager.printMessage(Diagnostic.Kind.ERROR, msgString(String.format(fmt, args)));
    }

    public void errorf(final Element element, final String fmt, final Object... args) {
        error = true;
        messager.printMessage(Diagnostic.Kind.ERROR, msgString(String.format(fmt, args)), element);
    }

    public void errorf(final Element element, AnnotationMirror annotationMirror, final String fmt, final Object... args) {
        error = true;
        messager.printMessage(Diagnostic.Kind.ERROR, msgString(String.format(fmt, args)), element, annotationMirror);
    }

    public void errorf(final Element element, AnnotationMirror annotationMirror, AnnotationValue value, final String fmt, final Object... args) {
        error = true;
        messager.printMessage(Diagnostic.Kind.ERROR, msgString(String.format(fmt, args)), element, annotationMirror, value);
    }

    public boolean isError() {
        return error;
    }

    public void setError() {
        error = true;
    }
}
