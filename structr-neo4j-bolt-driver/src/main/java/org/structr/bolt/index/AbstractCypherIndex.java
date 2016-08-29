/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.bolt.index;

import java.util.Arrays;
import org.structr.bolt.*;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.api.QueryResult;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.index.Index;
import org.structr.api.search.ArrayQuery;
import org.structr.api.search.EmptyQuery;
import org.structr.api.search.ExactQuery;
import org.structr.api.search.FulltextQuery;
import org.structr.api.search.GroupQuery;
import org.structr.api.search.NotEmptyQuery;
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.RangeQuery;
import org.structr.api.search.SpatialQuery;
import org.structr.api.search.TypeConverter;
import org.structr.api.search.TypeQuery;
import org.structr.bolt.index.converter.BooleanTypeConverter;
import org.structr.bolt.index.converter.DateTypeConverter;
import org.structr.bolt.index.converter.DoubleTypeConverter;
import org.structr.bolt.index.converter.IntTypeConverter;
import org.structr.bolt.index.converter.LongTypeConverter;
import org.structr.bolt.index.converter.StringTypeConverter;
import org.structr.bolt.index.factory.ArrayQueryFactory;
import org.structr.bolt.index.factory.EmptyQueryFactory;
import org.structr.bolt.index.factory.GroupQueryFactory;
import org.structr.bolt.index.factory.KeywordQueryFactory;
import org.structr.bolt.index.factory.NotEmptyQueryFactory;
import org.structr.bolt.index.factory.QueryFactory;
import org.structr.bolt.index.factory.RangeQueryFactory;
import org.structr.bolt.index.factory.SpatialQueryFactory;
import org.structr.bolt.index.factory.TypeQueryFactory;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractCypherIndex<T extends PropertyContainer> implements Index<T>, QueryFactory {

	private static final Logger logger                       = Logger.getLogger(AbstractCypherIndex.class.getName());
	public static final TypeConverter DEFAULT_CONVERTER      = new StringTypeConverter();
	public static final Map<Class, TypeConverter> CONVERTERS = new HashMap<>();
	public static final Map<Class, QueryFactory> FACTORIES   = new HashMap<>();

	private static final Set<Class> INDEXABLE = new HashSet<>(Arrays.asList(new Class[] {
		String.class, Boolean.class, Double.class, Integer.class, Long.class, Character.class, Float.class, Date.class
	}));

	static {

		FACTORIES.put(NotEmptyQuery.class, new NotEmptyQueryFactory());
		FACTORIES.put(FulltextQuery.class, new KeywordQueryFactory());
		FACTORIES.put(SpatialQuery.class,  new SpatialQueryFactory());
		FACTORIES.put(GroupQuery.class,    new GroupQueryFactory());
		FACTORIES.put(RangeQuery.class,    new RangeQueryFactory());
		FACTORIES.put(ExactQuery.class,    new KeywordQueryFactory());
		FACTORIES.put(ArrayQuery.class,    new ArrayQueryFactory());
		FACTORIES.put(EmptyQuery.class,    new EmptyQueryFactory());
		FACTORIES.put(TypeQuery.class,     new TypeQueryFactory());

		CONVERTERS.put(Boolean.class, new BooleanTypeConverter());
		CONVERTERS.put(String.class,  new StringTypeConverter());
		CONVERTERS.put(Date.class,    new DateTypeConverter());
		CONVERTERS.put(Long.class,    new LongTypeConverter());
		CONVERTERS.put(Integer.class, new IntTypeConverter());
		CONVERTERS.put(Double.class,  new DoubleTypeConverter());
	}

	protected BoltDatabaseService db = null;

	public AbstractCypherIndex(final BoltDatabaseService db) {
		this.db = db;
	}

	public abstract QueryResult<T> getResult(final CypherQuery query);
	public abstract String getQueryPrefix(final String typeLabel);
	public abstract String getQuerySuffix();

	@Override
	public void add(final PropertyContainer t, final String key, final Object value, final Class typeHint) {

		if (!t.hasProperty(key)) {

			Object indexValue = value;
			if (value != null) {

				if (value.getClass().isEnum()) {
					indexValue = indexValue.toString();
				}

				if (!INDEXABLE.contains(value.getClass())) {
					return;
				}
			}

			t.setProperty(key, indexValue);
		}
	}

	@Override
	public void remove(final PropertyContainer t) {
	}

	@Override
	public void remove(final PropertyContainer t, final String key) {
	}

	@Override
	public Iterable<T> query(final QueryPredicate predicate) {

		final CypherQuery query = new CypherQuery(this);

		createQuery(this, predicate, query, true);

		final String sortKey = predicate.getSortKey();
		if (sortKey != null) {

			query.sort(predicate.getSortType(), sortKey, predicate.sortDescending());
		}

		if (db.logQueries()) {
			System.out.println(query);
		}

		return getResult(query);
	}

	// ----- interface QueryFactory -----
	@Override
	public boolean createQuery(final QueryFactory parent, final QueryPredicate predicate, final CypherQuery query, final boolean isFirst) {

		final Class type = predicate.getQueryType();
		if (type != null) {

			final QueryFactory factory = FACTORIES.get(type);
			if (factory != null) {

				return factory.createQuery(this, predicate, query, isFirst);

			} else {

				logger.log(Level.WARNING, "No query factory registered for type {0}", type);
			}
		}

		return false;
	}
}