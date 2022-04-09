package org.aya.guest0x0.syntax;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.control.Option;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record Boundary<E>(@NotNull ImmutableSeq<Case> pats, @NotNull E body) {
  public enum Case {
    LEFT, RIGHT, VAR
  }

  public <T> @NotNull Boundary<T> fmap(@NotNull Function<E, T> f) {
    return new Boundary<>(pats, f.apply(body));
  }

  public record Data<E extends Docile>(
    @NotNull ImmutableSeq<LocalVar> dims,
    @NotNull E ty,
    @NotNull ImmutableSeq<Boundary<E>> boundaries
  ) implements Docile {
    public @NotNull Data<E> fmap(@NotNull Function<E, E> f, @NotNull ImmutableSeq<LocalVar> newDims) {
      return new Data<>(newDims, f.apply(ty), boundaries.map(b -> b.fmap(f)));
    }

    public @NotNull Data<E> fmap(@NotNull Function<E, E> f) {
      return fmap(f, dims);
    }

    @Override public @NotNull Doc toDoc() {
      var head = MutableList.of(Doc.symbol("[|"));
      dims.forEach(d -> head.append(Doc.symbol(d.name())));
      head.appendAll(new Doc[]{Doc.symbol("|]"), ty.toDoc()});
      return Doc.cblock(Doc.sep(head), 2, Doc.vcat(boundaries.map(b -> {
        var zesen = MutableList.of(Doc.symbol("|"));
        b.pats().forEach(d -> zesen.append(Doc.symbol(switch (d) {
          case LEFT -> "0";
          case RIGHT -> "1";
          case VAR -> "_";
        })));
        zesen.append(Doc.symbol("=>"));
        zesen.append(b.body().toDoc());
        return Doc.sep(zesen);
      })));
    }
  }

  public record Ends<T>(@NotNull Option<T> left, @NotNull Option<T> right) {
    public @NotNull Option<T> choose(boolean isLeft) {
      return isLeft ? left : right;
    }

    public <E> @NotNull Ends<E> fmap(@NotNull Function<T, E> f) {
      return new Ends<>(left.map(f), right.map(f));
    }
  }
}

