package org.aya.guest0x0.util;

import kala.collection.Seq;
import kala.collection.mutable.MutableList;
import org.aya.guest0x0.syntax.Boundary;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.Param;
import org.aya.guest0x0.syntax.Term;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public interface Distiller {
  static <T> @NotNull Doc boundaryData(@NotNull Boundary.Data<T> data, Function<T, Doc> f) {
    var head = MutableList.of(Doc.symbol("[|"));
    data.dims().forEach(d -> head.append(Doc.symbol(d.name())));
    head.appendAll(new Doc[]{Doc.symbol("|]"), f.apply(data.ty())});
    return Doc.cblock(Doc.sep(head), 2, Doc.vcat(data.boundaries().map(b -> {
      var zesen = MutableList.of(Doc.symbol("|"));
      b.pats().forEach(d -> zesen.append(Doc.symbol(switch (d) {
        case LEFT -> "0";
        case RIGHT -> "1";
        case VAR -> "_";
      })));
      zesen.append(Doc.symbol("=>"));
      zesen.append(f.apply(b.body()));
      return Doc.sep(zesen);
    })));
  }

  static @NotNull Doc expr(@NotNull Expr expr) {
    return switch (expr) {
      case Expr.UI u -> Doc.plain(u.isU() ? "U" : "I");
      case Expr.Two two && two.isApp() -> Doc.parened(Doc.sep(expr(two.f()), expr(two.a())));
      case Expr.Two two /*&& !two.isApp()*/ -> Doc.wrap("<<", ">>",
        Doc.commaList(Seq.of(expr(two.f()), expr(two.a()))));
      case Expr.Lam lam -> Doc.parened(Doc.sep(Doc.cat(
        Doc.plain("\\"), Doc.symbol(lam.x().name()), Doc.plain(".")), expr(lam.a())));
      case Expr.Resolved resolved -> Doc.plain(resolved.ref().name());
      case Expr.Path path -> boundaryData(path.data(), Distiller::expr);
      case Expr.Unresolved unresolved -> Doc.plain(unresolved.name());
      case Expr.Proj proj -> Doc.cat(expr(proj.t()), Doc.plain("." + (proj.isOne() ? 1 : 2)));
      case Expr.DT dt -> dependentType(dt.isPi(), dt.param(), dt.cod());
    };
  }
  private static @NotNull Doc dependentType(boolean isPi, Param<?> param, Docile cod) {
    return Doc.sep(Doc.plain(isPi ? "Pi" : "Sig"),
      param.toDoc(), Doc.symbol(isPi ? "->" : "**"), cod.toDoc());
  }

  static @NotNull Doc term(@NotNull Term term) {
    return switch (term) {
      case Term.DT dt -> dependentType(dt.isPi(), dt.param(), dt.cod());
      case Term.UI ui -> Doc.plain(ui.isU() ? "U" : "I");
      case Term.Ref ref -> Doc.plain(ref.var().name());
      case Term.Path path -> boundaryData(path.data(), Distiller::term);
      case Term.Lam lam -> Doc.parened(Doc.sep(Doc.cat(Doc.plain("\\"),
        Doc.symbol(lam.param().x().name()), Doc.plain(".")), term(lam.body())));
      case Term.Proj proj -> Doc.cat(term(proj.t()), Doc.plain("." + (proj.isOne() ? 1 : 2)));
      case Term.End end -> Doc.plain(end.isLeft() ? "0" : "1");
      case Term.Two two && two.isApp() -> Doc.parened(Doc.sep(term(two.f()), term(two.a())));
      case Term.Two two /*&& !two.isApp()*/ -> Doc.wrap("<<", ">>",
        Doc.commaList(Seq.of(term(two.f()), term(two.a()))));
      case Term.Call call -> Doc.parened(Doc.sep(call.args().view()
        .map(Distiller::term).prepended(Doc.plain(call.fn().name()))));
      case Term.PApp pApp -> term(new Term.Two(true, pApp.p(), pApp.i()));
      case Term.PLam pLam -> {
        var docs = MutableList.of(Doc.plain("\\"));
        pLam.dims().forEach(d -> docs.append(Doc.symbol(d.name())));
        docs.append(Doc.plain("."));
        docs.append(term(pLam.fill()));
        yield Doc.parened(Doc.sep(docs));
      }
    };
  }
}
