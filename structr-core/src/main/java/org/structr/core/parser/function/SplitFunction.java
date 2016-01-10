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
package org.structr.core.parser.function;

import java.util.Arrays;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 * @author Christian Morgner
 */
public class SplitFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_SPLIT = "Usage: ${split(value)}. Example: ${split(this.commaSeparatedItems)}";

	@Override
	public String getName() {
		return "split()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

			final String toSplit = sources[0].toString();
			String splitExpr = "[,;]+";

			if (sources.length >= 2) {
				splitExpr = sources[1].toString();
			}

			return Arrays.asList(toSplit.split(splitExpr));
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SPLIT;
	}

	@Override
	public String shortDescription() {
		return "Splits the given string";
	}


}