package edu.umich.verdict.relation.expr;

import java.util.ArrayList;
import java.util.List;

import edu.umich.verdict.VerdictContext;
import edu.umich.verdict.parser.VerdictSQLBaseVisitor;
import edu.umich.verdict.parser.VerdictSQLParser;
import edu.umich.verdict.util.StringManipulations;

public abstract class Expr {

    private static VerdictContext dummyContext;

    static {
        //		dummyContext = VerdictContext.dummyContext();
        dummyContext = null;
    }

    public static Expr from(String expr) {
        return from(dummyContext, expr);
    }

    public static Expr from(VerdictContext vc, String expr) {
        VerdictSQLParser p = StringManipulations.parserOf(expr);
        return from(vc, p.expression());
    }

    public static Expr from(VerdictSQLParser.ExpressionContext ctx) {
        return from(dummyContext, ctx);
    }

    public static Expr from(VerdictContext vc, VerdictSQLParser.ExpressionContext ctx) {
        if (vc == null) vc = dummyContext;
        ExpressionGen g = new ExpressionGen(vc);
        return g.visit(ctx);
    }

    //	public abstract String toString(VerdictContext vc);

    // to Sql String; use getText() to get the pure string representation.
    @Override
    public String toString() {
        return "Expr Base";
    }

    public String toStringWithoutQuote() {
        return StringManipulations.stripQuote(toString());
    }

    public static String quote(String s) {
        return String.format("`%s`", s);
    }

    public String getText() {
        return toString();
    }

    public Expr accept(ExprModifier v) {
        return v.call(this);
    }

    public abstract <T> T accept(ExprVisitor<T> v);

    public boolean isagg() {
        return false;
    }

    public boolean isCountDistinct() {
        return false;
    }

    public boolean isCount() {
        return false;
    }

    public List<String> extractColNames() {
        ExprVisitor<List<String>> v = new ExprVisitor<List<String>>() {
            private List<String> cols = new ArrayList<String>();

            @Override
            public List<String> call(Expr expr) {
                if (expr instanceof ColNameExpr) {
                    cols.add(((ColNameExpr) expr).getCol());
                }
                return cols;
            }
        };
        List<String> cols = v.visit(this);
        return cols;
    }

    public abstract Expr withTableSubstituted(String newTab);

    public abstract String toSql();

}

class ExpressionGen extends VerdictSQLBaseVisitor<Expr> {

    private VerdictContext vc;

    public ExpressionGen(VerdictContext vc) {
        this.vc = vc;
    }

    @Override
    public Expr visitPrimitive_expression(VerdictSQLParser.Primitive_expressionContext ctx) {
        return ConstantExpr.from(ctx.getText());
    }

    @Override
    public Expr visitColumn_ref_expression(VerdictSQLParser.Column_ref_expressionContext ctx) {
        return ColNameExpr.from(ctx.getText());
    }

    @Override
    public Expr visitBinary_operator_expression(VerdictSQLParser.Binary_operator_expressionContext ctx) {
        return new BinaryOpExpr(visit(ctx.expression(0)), visit(ctx.expression(1)), ctx.op.getText());  
    }

    @Override
    public Expr visitFunction_call_expression(VerdictSQLParser.Function_call_expressionContext ctx) {
        return FuncExpr.from(ctx.function_call());
    }

    @Override
    public Expr visitCase_expr(VerdictSQLParser.Case_exprContext ctx) {
        return CaseExpr.from(ctx);
    }

    @Override
    public Expr visitBracket_expression(VerdictSQLParser.Bracket_expressionContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Expr visitSubquery_expression(VerdictSQLParser.Subquery_expressionContext ctx) {
        return SubqueryExpr.from(vc, ctx);
    }

}
