package org.aya.guest0x0.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple;
import org.aya.guest0x0.syntax.Boundary;
import org.aya.guest0x0.syntax.Def;
import org.aya.guest0x0.syntax.LocalVar;
import org.aya.guest0x0.syntax.Term;
import org.jetbrains.annotations.NotNull;

public interface Unifier {
  static boolean untyped(@NotNull Term l, @NotNull Term r) {
    return switch (l) {
      case Term.Lam lam && r instanceof Term.Lam ram -> untyped(lam.body(), rhs(ram.body(), ram.x(), lam.x()));
      case Term.Lam lam -> eta(r, lam);
      case Term ll && r instanceof Term.Lam ram -> eta(ll, ram);
      case Term.Ref lref && r instanceof Term.Ref rref -> lref.var() == rref.var();
      case Term.Two lapp && r instanceof Term.Two rapp ->
        lapp.isApp() == rapp.isApp() && untyped(lapp.f(), rapp.f()) && untyped(lapp.a(), rapp.a());
      case Term.DT ldt && r instanceof Term.DT rdt -> ldt.isPi() == rdt.isPi()
        && untyped(ldt.param().type(), rdt.param().type())
        && untyped(ldt.cod(), rhs(rdt.cod(), rdt.param().x(), ldt.param().x()));
      case Term.Proj lproj && r instanceof Term.Proj rproj ->
        lproj.isOne() == rproj.isOne() && untyped(lproj.t(), rproj.t());
      case Term.UI lu && r instanceof Term.UI ru -> lu.isU() == ru.isU();
      case Term.Call lcall && r instanceof Term.Call rcall -> lcall.fn() == rcall.fn()
        && lcall.args().sameElements(rcall.args(), true);
      case Term.PLam plam && r instanceof Term.PLam pram && plam.dims().sizeEquals(pram.dims()) ->
        untyped(plam.fill(), pram.fill().subst(MutableMap.from(
          pram.dims().zip(plam.dims()).map(p -> Tuple.of(p._1, new Term.Ref(p._2))))));
      case Term.Formula lf && r instanceof Term.Formula rf -> formulae(lf.formula(), rf.formula());
      // Cubical subtyping?? Are we ever gonna unify cubes?
      default -> false;
    };
  }
  // Hopefully.... I don't know. :shrug:
  static boolean formulae(Boundary.Formula<Term> lf, Boundary.Formula<Term> rf) {
    return switch (lf) {
      case Boundary.Lit<Term> l && rf instanceof Boundary.Lit<Term> r -> l.isLeft() == r.isLeft();
      case Boundary.Inv<Term> l && rf instanceof Boundary.Inv<Term> r -> untyped(l.i(), r.i());
      case Boundary.Conn<Term> l && rf instanceof Boundary.Conn<Term> r && l.isAnd() == r.isAnd() ->
        untyped(l.l(), r.l()) && untyped(l.r(), r.r());
      default -> false;
    };
  }
  private static boolean eta(@NotNull Term r, Term.Lam lam) {
    return untyped(lam.body(), Term.mkApp(r, new Term.Ref(lam.x())));
  }

  private static @NotNull Term rhs(Term rhs, LocalVar rb, LocalVar lb) {
    return rhs.subst(rb, new Term.Ref(lb));
  }

  record Cof(@NotNull Normalizer l, @NotNull Normalizer r) {
    public Cof(@NotNull MutableMap<LocalVar, Def<Term>> sigma) {
      this(new Normalizer(sigma, MutableMap.create()), new Normalizer(sigma, MutableMap.create()));
    }

    public void unify(
      @NotNull ImmutableSeq<LocalVar> dims,
      @NotNull ImmutableSeq<Boundary.Case> lc, // Lambda calculus!!
      @NotNull ImmutableSeq<Boundary.Case> rc  // Reference counting!!
    ) {
      assert lc.sizeEquals(dims) && rc.sizeEquals(dims);
      for (var ttt : dims.zipView(lc.zipView(rc))) {
        if (ttt._2._1 == ttt._2._2) continue;
        if (ttt._2._1 == Boundary.Case.VAR) r.rho().put(ttt._1, Term.end(ttt._2._2 == Boundary.Case.LEFT));
        if (ttt._2._2 == Boundary.Case.VAR) l.rho().put(ttt._1, Term.end(ttt._2._1 == Boundary.Case.LEFT));
      }
    }
  }
}
