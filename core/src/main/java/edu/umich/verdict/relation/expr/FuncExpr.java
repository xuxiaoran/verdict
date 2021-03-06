package edu.umich.verdict.relation.expr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.umich.verdict.VerdictContext;
import edu.umich.verdict.parser.VerdictSQLBaseVisitor;
import edu.umich.verdict.parser.VerdictSQLParser;
import edu.umich.verdict.util.StringManipulations;

public class FuncExpr extends Expr {

    public enum FuncName {
        COUNT, SUM, AVG, COUNT_DISTINCT, IMPALA_APPROX_COUNT_DISTINCT,
        ROUND, MAX, MIN, FLOOR, CEIL, EXP, LN, LOG10, LOG2, SIN, COS, TAN,
        SIGN, RAND, FNV_HASH, ABS, STDDEV, SQRT, MOD, PMOD,
        CAST, CONV, SUBSTR, MD5, CRC32, UNIX_TIMESTAMP, CURRENT_TIMESTAMP,
        UNKNOWN
    }

    protected List<Expr> expressions;

    protected FuncName funcname;

    protected OverClause overClause;

    protected static Map<String, FuncName> string2FunctionType = ImmutableMap.<String, FuncName>builder()
            .put("ROUND", FuncName.ROUND)
            .put("MIN", FuncName.MIN)
            .put("MAX", FuncName.MAX)
            .put("FLOOR", FuncName.FLOOR)
            .put("CEIL", FuncName.CEIL)
            .put("EXP", FuncName.EXP)
            .put("LN", FuncName.LN)
            .put("LOG10", FuncName.LOG10)
            .put("LOG2", FuncName.LOG2)
            .put("SIN", FuncName.SIN)
            .put("COS", FuncName.COS)
            .put("TAN", FuncName.TAN)
            .put("SIGN", FuncName.SIGN)
            .put("RAND", FuncName.RAND)
            .put("UNIX_TIMESTAMP", FuncName.UNIX_TIMESTAMP)
            .put("CURRENT_TIMESTAMP", FuncName.CURRENT_TIMESTAMP)
            .put("FNV_HASH", FuncName.FNV_HASH)
            .put("MD5", FuncName.MD5)
            .put("ABS", FuncName.ABS)
            .put("STDDEV", FuncName.STDDEV)
            .put("SQRT", FuncName.SQRT)
            .put("MOD", FuncName.MOD)
            .put("CRC32", FuncName.CRC32)
            .put("PMOD", FuncName.PMOD)
            .put("CONV", FuncName.CONV)
            .put("SUBSTR", FuncName.SUBSTR)
            .put("CAST", FuncName.CAST)
            .build();

    protected static Map<FuncName, String> functionPattern = ImmutableMap.<FuncName, String>builder()
            .put(FuncName.COUNT,  "count(%s)")
            .put(FuncName.SUM, "sum(%s)")
            .put(FuncName.AVG, "avg(%s)")
            .put(FuncName.COUNT_DISTINCT, "count(distinct %s)")
            .put(FuncName.IMPALA_APPROX_COUNT_DISTINCT, "ndv(%s)")
            .put(FuncName.ROUND, "round(%s)")
            .put(FuncName.MIN, "min(%s)")
            .put(FuncName.MAX, "max(%s)")
            .put(FuncName.FLOOR, "floor(%s)")
            .put(FuncName.CEIL, "ceil(%s)")
            .put(FuncName.EXP, "exp(%s)")
            .put(FuncName.LN, "ln(%s)")
            .put(FuncName.LOG10, "log10(%s)")
            .put(FuncName.LOG2, "log2(%s)")
            .put(FuncName.SIN, "sin(%s)")
            .put(FuncName.COS, "cos(%s)")
            .put(FuncName.TAN, "tan(%s)")
            .put(FuncName.SIGN, "sign(%s)")
            .put(FuncName.RAND, "rand(%s)")
            .put(FuncName.UNIX_TIMESTAMP, "unix_timestamp(%s)")
            .put(FuncName.CURRENT_TIMESTAMP, "current_timestamp(%s)")
            .put(FuncName.FNV_HASH, "fnv_hash(%s)")
            .put(FuncName.MD5, "md5(%s)")
            .put(FuncName.CRC32, "crc32(%s)")
            .put(FuncName.ABS, "abs(%s)")
            .put(FuncName.STDDEV, "stddev(%s)")
            .put(FuncName.SQRT, "sqrt(%s)")
            .put(FuncName.MOD, "mod(%s, %s)")
            .put(FuncName.PMOD, "pmod(%s, %s)")
            .put(FuncName.CONV, "conv(%s, %s, %s)")
            .put(FuncName.SUBSTR, "substr(%s, %s, %s)")
            .put(FuncName.CAST, "cast(%s as %s)")
            .put(FuncName.UNKNOWN, "UNKNOWN(%s)")
            .build();

    public FuncExpr(FuncName fname, List<Expr> exprs, OverClause overClause) {
        this.expressions = exprs;
        this.funcname = fname;
        this.overClause = overClause;
    }

    public FuncExpr(FuncName fname, Expr expr1, Expr expr2, Expr expr3, OverClause overClause) {
        this.expressions = new ArrayList<Expr>();
        if (expr1 != null) {
            this.expressions.add(expr1);
            if (expr2 != null) {
                this.expressions.add(expr2);
                if (expr3 != null) {
                    this.expressions.add(expr3);
                }
            }
        }
        this.funcname = fname;
        this.overClause = overClause;
    }

    public FuncExpr(FuncName fname, Expr expr1, Expr expr2, OverClause overClause) {
        this(fname, expr1, expr2, null, overClause);
    }

    public FuncExpr(FuncName fname, Expr expr, OverClause overClause) {
        this(fname, expr, null, overClause);
    }

    public FuncExpr(FuncName fname, Expr expr1, Expr expr2, Expr expr3) {
        this(fname, expr1, expr2, expr3, null);
    }

    public FuncExpr(FuncName fname, Expr expr1, Expr expr2) {
        this(fname, expr1, expr2, null, null);
    }

    public FuncExpr(FuncName fname, Expr expr) {
        this(fname, expr, null, null, null);
    }

    public List<Expr> getExpressions() {
        return expressions;
    }

    public FuncName getFuncName() {
        return funcname;
    }

    public OverClause getOverClause() {
        return overClause;
    }

    public static FuncExpr from(String expr) {
        VerdictSQLParser p = StringManipulations.parserOf(expr);
        return from(p.function_call());
    }

    public static FuncExpr from(VerdictSQLParser.Function_callContext ctx) {
        VerdictSQLBaseVisitor<FuncExpr> v = new VerdictSQLBaseVisitor<FuncExpr>() {
            @Override
            public FuncExpr visitAggregate_windowed_function(VerdictSQLParser.Aggregate_windowed_functionContext ctx) {
                FuncName fname;
                Expr expr = null;
                if (ctx.all_distinct_expression() != null) {
                    expr = Expr.from(ctx.all_distinct_expression().expression());
                }
                OverClause overClause = null;

                if (ctx.AVG() != null) {
                    fname = FuncName.AVG;
                } else if (ctx.SUM() != null) {
                    fname = FuncName.SUM;
                } else if (ctx.COUNT() != null) {
                    if (ctx.all_distinct_expression() != null && ctx.all_distinct_expression().DISTINCT() != null) {
                        fname = FuncName.COUNT_DISTINCT;
                    } else {
                        fname = FuncName.COUNT;
                        expr = new StarExpr();
                    }
                } else if (ctx.NDV() != null) {
                    fname = FuncName.IMPALA_APPROX_COUNT_DISTINCT;
                } else if (ctx.MIN() != null) {
                    fname = FuncName.MIN;
                } else if (ctx.MAX() != null) {
                    fname = FuncName.MAX;
                } else {
                    fname = FuncName.UNKNOWN;
                }

                if (ctx.over_clause() != null) {
                    overClause = OverClause.from(ctx.over_clause());
                }

                return new FuncExpr(fname, expr, overClause);
            }

            @Override
            public FuncExpr visitUnary_mathematical_function(VerdictSQLParser.Unary_mathematical_functionContext ctx) {
                String fname = ctx.function_name.getText().toUpperCase();
                FuncName funcName = string2FunctionType.containsKey(fname)? string2FunctionType.get(fname) : FuncName.UNKNOWN;
                if (fname.equals("CAST")) {
                    return new FuncExpr(funcName,
                            Expr.from(ctx.cast_as_expression().expression()),
                            ConstantExpr.from(ctx.cast_as_expression().data_type().getText()));
                } else {
                    return new FuncExpr(funcName, Expr.from(ctx.expression()));
                }
            }

            @Override
            public FuncExpr visitNoparam_mathematical_function(VerdictSQLParser.Noparam_mathematical_functionContext ctx) {
                String fname = ctx.function_name.getText().toUpperCase();
                FuncName funcName = string2FunctionType.containsKey(fname)? string2FunctionType.get(fname) : FuncName.UNKNOWN;
                return new FuncExpr(funcName, null);
            }

            @Override
            public FuncExpr visitBinary_mathematical_function(VerdictSQLParser.Binary_mathematical_functionContext ctx) {
                String fname = ctx.function_name.getText().toUpperCase();
                FuncName funcName = string2FunctionType.containsKey(fname)? string2FunctionType.get(fname) : FuncName.UNKNOWN;
                return new FuncExpr(funcName, Expr.from(ctx.expression(0)), Expr.from(ctx.expression(1)));
            }

            @Override
            public FuncExpr visitTernary_mathematical_function(VerdictSQLParser.Ternary_mathematical_functionContext ctx) {
                String fname = ctx.function_name.getText().toUpperCase();
                FuncName funcName = string2FunctionType.containsKey(fname)? string2FunctionType.get(fname) : FuncName.UNKNOWN;
                return new FuncExpr(funcName, Expr.from(ctx.expression(0)), Expr.from(ctx.expression(1)), Expr.from(ctx.expression(2)));
            }
        };
        return v.visit(ctx);
    }

    public Expr getUnaryExpr() {
        return expressions.get(0);
    }

    public String getUnaryExprInString() {
        return getUnaryExpr().toString();
    }

    public static FuncExpr avg(Expr expr) {
        return new FuncExpr(FuncName.AVG, expr);
    }

    public static FuncExpr avg(String expr) {
        return avg(Expr.from(expr));
    }

    public static FuncExpr sum(Expr expr) {
        return new FuncExpr(FuncName.SUM, expr);
    }

    public static FuncExpr sum(String expr) {
        return sum(Expr.from(expr));
    }

    public static FuncExpr count() {
        return new FuncExpr(FuncName.COUNT, new StarExpr());
    }

    public static FuncExpr countDistinct(Expr expr) {
        return new FuncExpr(FuncName.COUNT_DISTINCT, expr);
    }

    public static FuncExpr countDistinct(String expr) {
        return countDistinct(Expr.from(expr));
    }

    public static FuncExpr round(Expr expr) {
        return new FuncExpr(FuncName.ROUND, expr);
    }

    public static FuncExpr min(Expr expr) {
        return new FuncExpr(FuncName.MIN, expr);
    }

    public static FuncExpr max(Expr expr) {
        return new FuncExpr(FuncName.MAX, expr);
    }

    public static FuncExpr stddev(Expr expr) {
        return new FuncExpr(FuncName.STDDEV, expr);
    }

    public static FuncExpr sqrt(Expr expr) {
        return new FuncExpr(FuncName.SQRT, expr);
    }

    public static FuncExpr approxCountDistinct(Expr expr, VerdictContext vc) {
        if (vc.getDbms().getName().equalsIgnoreCase("impala")) {
            return new FuncExpr(FuncName.IMPALA_APPROX_COUNT_DISTINCT, expr);
        } else {
            return new FuncExpr(FuncName.COUNT_DISTINCT, expr);
        }
    }

    public static FuncExpr approxCountDistinct(String expr, VerdictContext vc) {
        return approxCountDistinct(Expr.from(expr), vc);
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder(50);
        if (expressions.size() == 0) {
            sql.append(String.format(functionPattern.get(funcname), ""));
        } else if (expressions.size() == 1) {
            sql.append(String.format(functionPattern.get(funcname), expressions.get(0).toString()));
        } else if (expressions.size() == 2) {
            sql.append(String.format(functionPattern.get(funcname), expressions.get(0).toString(), expressions.get(1).toString()));
        } else {
            sql.append(String.format(functionPattern.get(funcname),
                    expressions.get(0).toString(),
                    expressions.get(1).toString(),
                    expressions.get(2).toString()));
        }

        if (overClause != null) {
            sql.append(" " + overClause.toString());
        }
        return sql.toString();
    }

    @Override
    public <T> T accept(ExprVisitor<T> v) {
        return v.call(this);
    }

    @Override
    public boolean isagg() {
        if (funcname.equals(FuncName.AVG) || funcname.equals(FuncName.SUM) || funcname.equals(FuncName.COUNT)
         || funcname.equals(FuncName.COUNT_DISTINCT) || funcname.equals(FuncName.IMPALA_APPROX_COUNT_DISTINCT)
         || funcname.equals(FuncName.MIN) || funcname.equals(FuncName.MAX)) {
            return true;
        } else {
            for (Expr expr : expressions) {
                if (expr.isagg()) return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCountDistinct() {
        if (funcname.equals(FuncName.COUNT_DISTINCT)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isCount() {
        if (funcname.equals(FuncName.COUNT)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Expr withTableSubstituted(String newTab) {
        List<Expr> newExprs = new ArrayList<Expr>();
        for (Expr e : expressions) {
            newExprs.add(e.withTableSubstituted(newTab));
        }
        return new FuncExpr(funcname, newExprs, overClause);
    }

    @Override
    public String toSql() {
        StringBuilder sql = new StringBuilder(50);
        if (expressions.size() == 0) {
            sql.append(String.format(functionPattern.get(funcname), ""));
        } else if (expressions.size() == 1) {
            sql.append(String.format(functionPattern.get(funcname), expressions.get(0).toSql()));
        } else if (expressions.size() == 2) {
            sql.append(String.format(functionPattern.get(funcname), expressions.get(0).toSql(), expressions.get(1).toSql()));
        } else {
            sql.append(String.format(functionPattern.get(funcname),
                    expressions.get(0).toSql(),
                    expressions.get(1).toSql(),
                    expressions.get(2).toSql()));
        }

        if (overClause != null) {
            sql.append(" " + overClause.toString());
        }
        return sql.toString();
    }
}
