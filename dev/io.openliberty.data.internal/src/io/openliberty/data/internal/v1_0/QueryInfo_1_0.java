/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal.v1_0;

import java.lang.reflect.Method;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.QueryInfo;
import io.openliberty.data.internal.QueryType;
import io.openliberty.data.internal.cdi.RepositoryProducer;

/**
 * QueryInfo implementation for Jakarta Data 1.0.
 */
public class QueryInfo_1_0 extends QueryInfo {

    /**
     * Construct partially complete query information.
     *
     * @param repositoryProducer    producer of the repository bean instance.
     * @param repositoryInterface   interface annotated with @Repository.
     * @param method                repository method.
     * @param entityParamType       type of the first parameter if a life cycle method,
     *                                  otherwise null.
     * @param methodType            type of repository method, if known in advance.
     * @param isOptional            indicates if the return type is an Optional.
     * @param returnArrayType       array element type if the repository method returns
     *                                  an array, otherwise null.
     * @param multiType             Data structure type that allows multiple
     *                                  results for this query. Null if the query
     *                                  return type limits to single results.
     * @param singleType            Type of a single result obtained by the query.
     * @param singleTypeElementType Element type of singleType when singleType is an
     *                                  array or collection. Otherwise null.
     */
    @Trivial
    QueryInfo_1_0(RepositoryProducer<?> repositoryProducer,
                  Class<?> repositoryInterface,
                  Method method,
                  QueryType methodType,
                  Class<?> entityParamType,
                  boolean isOptional,
                  Class<?> returnArrayType,
                  Class<?> multiType,
                  Class<?> singleType,
                  Class<?> singleTypeElementType) {
        super(repositoryProducer, //
              repositoryInterface, //
              method, //
              methodType, //
              entityParamType, //
              isOptional, //
              multiType, //
              returnArrayType, //
              singleType, //
              singleTypeElementType);
    }

}