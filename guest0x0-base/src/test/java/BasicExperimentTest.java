import kala.collection.mutable.MutableMap;
import kala.control.Either;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.aya.guest0x0.parser.Guest0x0Lexer;
import org.aya.guest0x0.parser.Guest0x0Parser;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.Parser;
import org.aya.guest0x0.tyck.Elaborator;
import org.aya.guest0x0.tyck.Resolver;
import org.aya.util.error.SourceFile;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BasicExperimentTest {
  @Test public void resolveId() {
    var e = (Expr.Lam) expr("\\x. x");
    assertNotNull(e);
    assertSame(((Expr.Resolved) e.a()).ref(), e.x());
  }

  @Test public void tyckId() {
    var IdE = expr("Pi (A : Type) -> A -> A");
    var id = expr("\\A. \\x. x");
    var akJr = new Elaborator(MutableMap.create());
    var Id = akJr.synth(IdE);
    var artifact = akJr.inherit(id, Id.wellTyped());
    assertNotNull(artifact);
  }

  private @NotNull Expr expr(String s) {
    return new Resolver(MutableMap.create())
      .expr(new Parser(Either.left(SourceFile.NONE)).expr(parser(s).expr()));
  }

  private Guest0x0Parser parser(String s) {
    return new Guest0x0Parser(new CommonTokenStream(new Guest0x0Lexer(CharStreams.fromString(s))));
  }
}
