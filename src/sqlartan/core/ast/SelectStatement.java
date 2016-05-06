package sqlartan.core.ast;

import sqlartan.core.ast.gen.Builder;
import sqlartan.core.ast.parser.ParserContext;
import java.util.ArrayList;
import java.util.List;
import static sqlartan.core.ast.Keyword.*;
import static sqlartan.core.ast.Operator.*;

/**
 * https://www.sqlite.org/lang_select.html
 */
public abstract class SelectStatement implements Statement {
	/**
	 * General SELECT statement parser
	 * Currently delegates to Simple parser.
	 * TODO: handle compound selects here
	 */
	public static SelectStatement parse(ParserContext context) {
		return Simple.parse(context);
	}

	/**
	 * The core a SELECT statement
	 */
	public static class Core extends SelectStatement {
		public boolean distinct;
		public List<ResultColumn> columns;
		public List<SelectSource> from;
		public Expression where;
		public List<Expression> groupBy;
		public Expression having;

		public static Core parse(ParserContext context) {
			Core select = new Core();
			parse(context, select);
			return select;
		}

		static void parse(ParserContext context, Core select) {
			context.consume(SELECT);

			select.distinct = context.tryConsume(DISTINCT) && !context.tryConsume(ALL);
			select.columns = context.parseList(COMMA, ResultColumn::parse);

			if (context.tryConsume(FROM)) {
				select.from = new ArrayList<>();
				if (!context.parseList(select.from, COMMA, TableOrSubquerySource::parse)) {
					select.from.add(JoinClause.parse(context));
				}
			}

			if (context.tryConsume(WHERE)) {
				select.where = Expression.parse(context);
			}

			if (context.tryConsume(GROUP)) {
				context.consume(BY);
				select.groupBy = context.parseList(COMMA, Expression::parse);

				if (context.tryConsume(HAVING)) {
					select.having = Expression.parse(context);
				}
			}
		}

		@Override
		public void toSQL(Builder sql) {
			sql.append(SELECT);
			if (distinct)
				sql.append(DISTINCT);
			sql.append(columns);
			if (from != null)
				sql.append(FROM).append(from);
			if (where != null)
				sql.append(WHERE).append(where);
			if (groupBy != null) {
				sql.append(GROUP, BY).append(groupBy);
				if (having != null)
					sql.append(HAVING).append(having);
			}
		}
	}

	/**
	 * A simple SELECT statement
	 */
	public static class Simple extends Core {
		public List<OrderingTerm> orderBy;
		public Expression limit;
		public Expression offset;

		public static Simple parse(ParserContext context) {
			if (context.tryConsume(WITH)) {
				// With clauses are unsupported
				throw new UnsupportedOperationException();
			}

			// Parse the core
			Simple select = new Simple();
			Core.parse(context, select);

			// ORDER BY
			if (context.tryConsume(ORDER)) {
				context.consume(BY);
				select.orderBy = context.parseList(COMMA, OrderingTerm::parse);
			}

			// LIMIT .. OFFSET
			if (context.tryConsume(LIMIT)) {
				select.limit = Expression.parse(context);
				if (context.tryConsume(OFFSET) || context.tryConsume(COMMA)) {
					select.offset = Expression.parse(context);
				}
			}

			return select;
		}

		@Override
		public void toSQL(Builder sql) {
			super.toSQL(sql);
			if (orderBy != null)
				sql.append(ORDER, BY).append(orderBy);
			if (limit != null) {
				sql.append(LIMIT).append(limit);
				if (offset != null) {
					sql.append(COMMA).append(offset);
				}
			}
		}
	}

	/**
	 * Source of data for SELECT statements
	 */
	public abstract static class SelectSource implements Node {
		public static SelectSource parse(ParserContext context) {
			return context.alternatives(
				TableOrSubquerySource::parse,
				JoinClause::parse
			);
		}
	}

	/**
	 * Either a table or a sub-query sources
	 */
	public static class TableOrSubquerySource extends SelectSource {
		public String as;

		public static TableOrSubquerySource parse(ParserContext context) {
			if (context.current(LEFT_PAREN)) {
				// Sub query
				throw new UnsupportedOperationException();
			} else {
				return TableSource.parse(context);
			}
		}
	}

	/**
	 * A table source
	 */
	public static class TableSource extends TableOrSubquerySource {
		public String schema;
		public String table;
		public String index;
		public boolean notIndexed;

		public static TableSource parse(ParserContext context) {
			TableSource source = new TableSource();

			// Explicit schema name
			if (context.next(DOT)) {
				source.schema = context.consumeIdentifier();
				context.consume(DOT);
			}

			// Table name
			source.table = context.consumeIdentifier();

			// Attempt to consume alias
			context.tryConsume(AS);
			source.as = context.optConsumeIdentifier().orElse(null);

			if (context.tryConsume(INDEXED)) {
				context.consume(BY);
				source.index = context.consumeIdentifier();
			} else if (context.tryConsume(NOT)) {
				context.consume(INDEXED);
				source.notIndexed = true;
			}

			return source;
		}

		@Override
		public void toSQL(Builder sql) {
			if (schema != null)
				sql.appendIdentifier(schema).append(DOT);
			sql.appendIdentifier(table);
			if (as != null)
				sql.append(AS).appendIdentifier(as);
			if (notIndexed)
				sql.append(NOT, INDEXED);
			else if (index != null)
				sql.append(INDEXED, BY).appendIdentifier(index);
		}
	}

	/**
	 * A JOIN clause
	 */
	public static class JoinClause extends SelectSource {
		public static JoinClause parse(ParserContext context) {
			throw new UnsupportedOperationException();
		}
	}

}
