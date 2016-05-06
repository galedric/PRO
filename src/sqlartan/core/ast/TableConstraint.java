package sqlartan.core.ast;

import sqlartan.core.ast.gen.Builder;
import sqlartan.core.ast.parser.ParseException;
import sqlartan.core.ast.parser.ParserContext;
import java.util.List;
import java.util.Optional;
import static sqlartan.core.ast.Keyword.*;
import static sqlartan.core.ast.Operator.LEFT_PAREN;
import static sqlartan.core.ast.Operator.RIGHT_PAREN;
import static sqlartan.util.Matching.match;

@SuppressWarnings({ "OptionalUsedAsFieldOrParameterType", "WeakerAccess" })
public abstract class TableConstraint implements Node {
	public Optional<String> name = Optional.empty();

	public static TableConstraint parse(ParserContext context) {
		String name = null;
		if (context.tryConsume(CONSTRAINT)) {
			name = context.consumeIdentifier();
		}

		TableConstraint constraint = match(context.current(), TableConstraint.class)
			.when(PRIMARY, () -> Index.parse(context))
			.when(UNIQUE, () -> Index.parse(context))
			.when(CHECK, () -> Check.parse(context))
			.when(FOREIGN, () -> ForeignKey.parse(context))
			.orElseThrow(ParseException.UnexpectedCurrentToken);

		constraint.name = Optional.ofNullable(name);
		return constraint;
	}

	@Override
	public void toSQL(Builder sql) {
		name.ifPresent(n -> sql.append(CONSTRAINT).appendIdentifier(n));
	}

	public static class Index extends TableConstraint {
		enum Type {PrimaryKey, Unique}

		public Type type;
		public List<IndexedColumn> columns;
		public ConflictClause onConflict;

		public static Index parse(ParserContext context) {
			Index index = new Index();
			if (context.tryConsume(PRIMARY, KEY)) {
				index.type = Type.PrimaryKey;
			} else {
				context.consume(UNIQUE);
				index.type = Type.Unique;
			}
			context.consume(LEFT_PAREN);
			index.columns = context.parseList(IndexedColumn::parse);
			context.consume(RIGHT_PAREN);
			index.onConflict = ConflictClause.parse(context);
			return index;
		}

		@Override
		public void toSQL(Builder sql) {
			super.toSQL(sql);
			switch (type) {
				case PrimaryKey:
					sql.append(PRIMARY, KEY);
					break;
				case Unique:
					sql.append(UNIQUE);
					break;
			}
			sql.append(LEFT_PAREN).append(columns).append(RIGHT_PAREN)
			   .append(onConflict);
		}
	}

	public static class Check extends TableConstraint {
		public Expression expression;

		public static Check parse(ParserContext context) {
			Check check = new Check();
			context.consume(CHECK, LEFT_PAREN);
			check.expression = Expression.parse(context);
			context.consume(RIGHT_PAREN);
			return check;
		}

		@Override
		public void toSQL(Builder sql) {
			super.toSQL(sql);
			sql.append(LEFT_PAREN).append(expression).append(RIGHT_PAREN);
		}
	}

	public static class ForeignKey extends TableConstraint {
		public List<String> columns;
		public ForeignKeyClause foreignKey;

		public static ForeignKey parse(ParserContext context) {
			ForeignKey fk = new ForeignKey();
			context.consume(FOREIGN, KEY, LEFT_PAREN);
			fk.columns = context.parseList(ParserContext::consumeIdentifier);
			context.consume(RIGHT_PAREN);
			fk.foreignKey = ForeignKeyClause.parse(context);
			return fk;
		}

		@Override
		public void toSQL(Builder sql) {
			super.toSQL(sql);
			sql.append(FOREIGN, KEY, LEFT_PAREN)
			   .append(columns, col -> s -> s.appendIdentifier(col))
			   .append(RIGHT_PAREN)
			   .append(foreignKey);
		}
	}
}
