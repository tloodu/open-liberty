/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.persistence.orm;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts that can be placed on different annotation targets
 */
public class TestConverters {

    @Converter
    public static class ClassConverter implements AttributeConverter<String, String> {

        @Override
        public String convertToDatabaseColumn(String attribute) {
            return "C" + attribute;
        }

        @Override
        public String convertToEntityAttribute(String data) {
            return data.substring(1);
        }
    }

    @Converter
    public static class MethodConverter implements AttributeConverter<String, String> {

        @Override
        public String convertToDatabaseColumn(String attribute) {
            return "C" + attribute;
        }

        @Override
        public String convertToEntityAttribute(String data) {
            return data.substring(1);
        }
    }

    @Converter
    public static class FieldConverter implements AttributeConverter<String, String> {

        @Override
        public String convertToDatabaseColumn(String attribute) {
            return "C" + attribute;
        }

        @Override
        public String convertToEntityAttribute(String data) {
            return data.substring(1);
        }
    }
}
