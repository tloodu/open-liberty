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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import io.openliberty.data.internal.persistence.Util;
import io.openliberty.data.internal.persistence.orm.Models.AccessType;
import io.openliberty.data.internal.persistence.orm.Models.Attribute;
import io.openliberty.data.internal.persistence.orm.Models.AttributeKind;
import io.openliberty.data.internal.persistence.orm.Models.Converter;
import io.openliberty.data.internal.persistence.orm.Models.EmbeddableRecord;
import io.openliberty.data.internal.persistence.orm.Models.EntityRecord;
import io.openliberty.data.internal.persistence.orm.Models.IncompleteAttribute;
import io.openliberty.data.internal.persistence.orm.Models.MappedSuperclass;
import jakarta.data.exceptions.MappingException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;

/**
 * TODO find converters in annotated entities but do not record the entity itself
 * TODO handle records
 * TODO verify that every entity has an id attribute in itself or a mapped superclass
 */
public class EntityParser {

    // ORM sets
    private final SortedSet<MappedSuperclass> mappedSuperclasses;
    private final SortedSet<EntityRecord> entities;
    private final SortedSet<EmbeddableRecord> embeddables;
    private final SortedSet<Converter> converters;

    // Relationships
    private final Relationships relate;

    // Global configurations
    private final String tablePrefix;

    // State
    private boolean finished = false;

    public EntityParser(String tablePrefix) {
        this.mappedSuperclasses = new TreeSet<>();
        this.entities = new TreeSet<>();
        this.embeddables = new TreeSet<>();
        this.converters = new TreeSet<>();

        this.relate = new Relationships();

        this.tablePrefix = tablePrefix;
    }

    public void parse(Class<?> entity) {
        if (finished) {
            return; // fail?
        }

        // If the entity was annotated with @Entity
        // only search for converters.
        if (entity.isAnnotationPresent(Entity.class)) {
            AnnotatedUtilities.findConvertersInEntity(entity, converters);
            return;
        }

        // If entity was unannotated then we must
        // construct an object relational mapping
        String tableName = tablePrefix + entity.getSimpleName();

        for (Class<?> superclass = entity; //
                        superclass != null && superclass != Object.class; //
                        superclass = superclass.getSuperclass()) {

            for (Convert convert : superclass.getAnnotationsByType(Convert.class)) {
                recordConverter(convert);
            }

            if (superclass == entity) {
                // Record entity and any embeddables found along the way
                entities.add(new EntityRecord(superclass, tableName, finalizeAttributes(superclass, findAttributes(superclass))));
                continue;
            }

            // Record all mapped superclasses and any embeddables found along the way
            mappedSuperclasses.add(new MappedSuperclass(superclass, finalizeAttributes(superclass, findAttributes(superclass))));
        }
    }

    private Set<IncompleteAttribute> findAttributes(Class<?> c) {
        SortedSet<IncompleteAttribute> attributes = new TreeSet<>();

        if (c.isRecord()) {
            for (RecordComponent r : c.getRecordComponents())
                attributes.add(new IncompleteAttribute(r.getType(), r.getName(), AccessType.FIELD));
        } else {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isPublic(f.getModifiers())) {
                    attributes.add(new IncompleteAttribute(f.getType(), f.getName(), AccessType.FIELD));

                    for (Convert convert : f.getAnnotationsByType(Convert.class))
                        recordConverter(convert);
                }
            }

            try {
                PropertyDescriptor[] propertyDescriptors = Introspector //
                                .getBeanInfo(c).getPropertyDescriptors();
                if (propertyDescriptors != null)
                    for (PropertyDescriptor p : propertyDescriptors) {
                        if (p.getWriteMethod() != null) {
                            //Note: p.getName() utilizes Introspector.decapitalize method
                            //      which honors acryonyms like getURL/setURL -> URL (instead of uRL)
                            attributes.add(new IncompleteAttribute(p.getPropertyType(), p.getName(), AccessType.PROPERTY));
                        }

                        if (p.getReadMethod() != null)
                            for (Convert convert : p.getReadMethod().getAnnotationsByType(Convert.class))
                                recordConverter(convert);
                    }

            } catch (IntrospectionException x) {
                throw new MappingException(x);
            }
        }
        return attributes;
    }

    private Set<Attribute> finalizeAttributes(Class<?> c, Set<IncompleteAttribute> incompletes) {
        SortedSet<Attribute> attributes = new TreeSet<>();

        IncompleteAttribute id = null;
        IncompleteAttribute version = null;

        // Determine which attribute is the id and version (optional).
        // Id precedence:
        // (1) name is id, ignoring case.
        // (2) name ends with _id, ignoring case.
        // (3) name ends with Id or ID.
        // (4) type is UUID.
        // Version precedence (if also a valid version type):
        // (1) name is version, ignoring case.
        // (2) name is _version, ignoring case.
        int idPrecedence = 10;
        int vPrecedence = 10;
        for (IncompleteAttribute attr : incompletes) {
            String name = attr.name();
            Class<?> type = attr.type();
            int len = name.length();

            if (idPrecedence > 1 &&
                len >= 2 &&
                name.regionMatches(true, len - 2, "id", 0, 2)) {
                if (name.length() == 2) {
                    id = attr;
                    idPrecedence = 1;
                } else if (idPrecedence > 2 &&
                           name.charAt(len - 3) == '_') {
                    id = attr;
                    idPrecedence = 2;
                } else if (idPrecedence > 3 &&
                           name.charAt(len - 2) == 'I') {
                    id = attr;
                    idPrecedence = 3;
                }
            } else if (idPrecedence > 4 && UUID.class.equals(type)) {
                id = attr;
                idPrecedence = 4;
            }

            if (vPrecedence > 1 &&
                len == 7 &&
                Util.VERSION_TYPES.contains(type) &&
                "version".equalsIgnoreCase(name)) {
                version = attr;
                vPrecedence = 1;
            } else if (vPrecedence > 2 &&
                       len == 8 &&
                       Util.VERSION_TYPES.contains(type) &&
                       "_version".equalsIgnoreCase(name)) {
                version = attr;
                vPrecedence = 2;
            }
        }

        for (IncompleteAttribute attr : incompletes) {
            Class<?> type = attr.type();

            boolean isId = attr == id;
            boolean isVersion = attr == version;
            boolean isPrimitive = type.isPrimitive();
            boolean isCollection = Collection.class.isAssignableFrom(type);

            AttributeKind kind;
            if (isPrimitive || type.isInterface() || //
                Serializable.class.isAssignableFrom(type)) {
                if (isId)
                    kind = AttributeKind.ID;
                else if (isCollection)
                    kind = AttributeKind.ELEMENT_COLLECTION;
                else if (isVersion)
                    kind = AttributeKind.VERSION;
                else
                    kind = AttributeKind.BASIC;
            } else {
                if (isId)
                    kind = AttributeKind.EMBEDDED_ID;
                else
                    kind = AttributeKind.EMBEDDED;
            }

            if (kind == AttributeKind.EMBEDDED || kind == AttributeKind.EMBEDDED_ID) {
                Set<Attribute> embedAttributes = finalizeAttributes(c, findAttributes(type));

                attributes.add(new Attribute(attr, kind, embedAttributes));
                embeddables.add(new EmbeddableRecord(type, embedAttributes));

                relate.entityHasEmbed(c, type);
            } else {
                attributes.add(new Attribute(attr, kind, Set.of()));
            }
        }

        return attributes;
    }

    private void recordConverter(Convert convert) {
        if (convert.converter() != null && convert.converter() != AttributeConverter.class)
            converters.add(new Converter(convert.converter()));
    }

    // Termination point
    public List<String> generateView() {
        View view = new View(mappedSuperclasses.size() + entities.size() + embeddables.size() + converters.size());

        for (MappedSuperclass sc : mappedSuperclasses) {
            view.mappedSuperclass(sc);
        }

        for (EntityRecord er : entities) {
            view.entity(er);
        }

        for (EmbeddableRecord emb : embeddables) {
            view.embedable(emb);
        }

        for (Converter con : converters) {
            view.converter(con);
        }

        finished = true;
        return view.get();
    }
}
