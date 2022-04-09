package org.aya.guest0x0.syntax;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import org.aya.guest0x0.tyck.Normalizer;
import org.aya.guest0x0.util.Distiller;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

public sealed interface Term extends Docile {
  @Override default @NotNull Doc toDoc() {
    return Distiller.term(this, Distiller.FREE);
  }
  default @NotNull Term subst(@NotNull LocalVar x, @NotNull Term t) {
    return subst(MutableMap.of(x, t));
  }
  default @NotNull Term subst(@NotNull MutableMap<LocalVar, Term> map) {
    return new Normalizer(MutableMap.create(), map).term(this);
  }

  record Ref(@NotNull LocalVar var) implements Term {}
  record Call(@NotNull LocalVar fn, @NotNull ImmutableSeq<Term> args) implements Term {}
  record Two(boolean isApp, @NotNull Term f, @NotNull Term a) implements Term {}
  record Proj(@NotNull Term t, boolean isOne) implements Term {}
  record Lam(@NotNull Param<Term> param, @NotNull Term body) implements Term {}

  static @NotNull Term mkLam(@NotNull ImmutableSeq<Param<Term>> telescope, @NotNull Term body) {
    return telescope.view().foldRight(body, Lam::new);
  }
  static @NotNull Term mkApp(@NotNull Term f, @NotNull Term a) {
    return f instanceof Lam lam ? lam.body.subst(lam.param.x(), a) : new Two(true, f, a);
  }

  record DT(boolean isPi, @NotNull Param<Term> param, @NotNull Term cod) implements Term {
    public @NotNull Term codomain(@NotNull Term term) {
      return cod.subst(param.x(), term);
    }
  }

  static @NotNull Term mkPi(@NotNull ImmutableSeq<Param<Term>> telescope, @NotNull Term body) {
    return telescope.view().foldRight(body, (param, term) -> new DT(true, param, term));
  }
  @NotNull Term U = new UI(true), I = new UI(false);
  record UI(boolean isU) implements Term {}
  record Path(@NotNull Boundary.Data<Term> data) implements Term {}
  record End(boolean isLeft) implements Term {}
  record PLam(@NotNull ImmutableSeq<LocalVar> dims, @NotNull Term fill) implements Term {}
  record PCall(@NotNull LocalVar p, @NotNull ImmutableSeq<Term> i, @NotNull Boundary.Data<Term> b) implements Term {}
}
