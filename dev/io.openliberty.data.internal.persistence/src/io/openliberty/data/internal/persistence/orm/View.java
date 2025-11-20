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

import static io.openliberty.data.internal.persistence.Util.EOLN;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.openliberty.data.internal.persistence.orm.Models.Attribute;
import io.openliberty.data.internal.persistence.orm.Models.Converter;
import io.openliberty.data.internal.persistence.orm.Models.EmbeddableRecord;
import io.openliberty.data.internal.persistence.orm.Models.EntityRecord;
import io.openliberty.data.internal.persistence.orm.Models.MappedSuperclass;

public class View {
    private static final String indent(int level) {
        return "  ".repeat(level);
    }

    private final List<String> info;

    public View(int size) {
        info = new ArrayList<>(size);
    }

    public List<String> get() {
        return info;
    }

    public void mappedSuperclass(MappedSuperclass model) {
        StringBuilder xml = new StringBuilder(500);

        xml.append(indent(1))//
                        .append("<mapped-superclass class=\"").append(model.type().getName()).append("\">")//
                        .append(EOLN);

        attributes(xml, model.type(), model.attributes());

        xml.append(indent(1))//
                        .append("</mapped-superclass>").append(EOLN);

        info.add(xml.toString());
    }

    public void entity(EntityRecord model) {
        StringBuilder xml = new StringBuilder(500);

        xml.append(indent(1))//
                        .append("<entity class=\"").append(model.type().getName()).append("\">")//
                        .append(EOLN);

        xml.append(indent(2))//
                        .append("<table name=\"").append(model.tableName()).append("\"/>").append(EOLN);

        attributes(xml, model.type(), model.attributes());

        xml.append(indent(1))//
                        .append("</entity>").append(EOLN);

        info.add(xml.toString());
    }

    public void embedable(EmbeddableRecord model) {
        StringBuilder xml = new StringBuilder(500);

        xml.append(indent(1))//
                        .append("<embeddable class=\"").append(model.type().getName()).append("\">")//
                        .append(EOLN);

        attributes(xml, model.type(), model.attributes());

        xml.append(indent(1))//
                        .append("</embeddable>").append(EOLN);

        info.add(xml.toString());
    }

    public void converter(Converter model) {
        StringBuilder xml = new StringBuilder(500);

        xml.append(indent(1))//
                        .append("<converter class=\"").append(model.type().getName()).append("\">")//
                        .append(EOLN);

        xml.append(indent(1))//
                        .append("</converter>").append(EOLN);

        info.add(xml.toString());
    }

    private void attributes(StringBuilder xml, Class<?> entity, Set<Attribute> attrs) {
        xml.append(indent(2)).append("<attributes>").append(EOLN);

        for (Attribute attr : attrs) {
            attribute(xml, entity, attr);
        }

        xml.append(indent(2)).append("</attributes>").append(EOLN);

    }

    private void attribute(StringBuilder xml, Class<?> entity, Attribute attr) {
        xml.append(indent(3))//
                        .append('<').append(attr.kind().toElementName())//
                        .append(" name=\"").append(attr.name()).append('"')//
                        .append(" access=\"").append(attr.access()).append('"');//

        if (attr.isCollection()) {
            xml.append(" fetch=\"EAGER\"");
        }

        xml.append('>').append(EOLN);

        if (attr.rejectsNull()) {
            xml.append(indent(4))//
                            .append("<column nullable=\"false\"/>")//
                            .append(EOLN);
        }

        if (attr.isEmbeddedCollection() && attr.collectionId() != null) {
            collectionTable(xml, entity, attr, attr.collectionId());
        }

        for (Attribute override : attr.overrides()) {
            attributeOverride(xml, attr, override);
        }

        xml.append(indent(3))//
                        .append("</").append(attr.kind().toElementName()).append('>')//
                        .append(EOLN);

    }

    private void attributeOverride(StringBuilder xml, Attribute attr, Attribute override) {
        xml.append(indent(4))//
                        .append("<attribute-override name=\"").append(override.name())//
                        .append("\">").append(EOLN);

        xml.append(indent(5)) //
                        .append("<column name=\"").append(attr.name().toUpperCase())//
                        .append('_').append(override.name().toUpperCase()) //
                        .append("\"/>").append(EOLN);

        xml.append(indent(4))//
                        .append("</attribute-override>").append(EOLN);
    }

    private void collectionTable(StringBuilder xml, Class<?> entity, Attribute attr, Attribute collectionId) {
        String collectionTable = entity.getSimpleName().toUpperCase() + "_" + attr.name().toUpperCase();

        xml.append(indent(4))//
                        .append("<collection-table name=\"").append(collectionTable)//
                        .append("\">").append(EOLN);

        xml.append(indent(5)) //
                        .append("<join-column name=\"").append(collectionId.name())//
                        .append("\"/>").append(EOLN);

        xml.append(indent(4))//
                        .append("</collection-table>").append(EOLN);
    }

}
