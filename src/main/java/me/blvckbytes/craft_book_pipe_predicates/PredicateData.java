package me.blvckbytes.craft_book_pipe_predicates;

import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.Nullable;

public record PredicateData(
  String tokensPredicate,
  String expandedPredicate,
  String signLine1,
  String signLine3,
  String signLine4,
  @Nullable ItemPredicate parsedPredicate,
  @Nullable ItemPredicateParseException parseException
) {

  public void restoreLines(Sign sign) {
    sign.setLine(0, signLine1);

    if (!signLine3.isBlank())
      sign.setLine(2, signLine3);

    if (!signLine4.isBlank())
      sign.setLine(3, signLine4);

    sign.update(true, false);
  }

  public static PredicateData makeInitial(ItemPredicate predicate, Sign sign) {
    return new PredicateData(
      predicate.stringify(true),
      predicate.stringify(false),
      sign.getLine(0),
      sign.getLine(2),
      sign.getLine(3),
      predicate, null
    );
  }

  public static PredicateData makeUpdate(ItemPredicate predicate, PredicateData previous) {
    return new PredicateData(
      predicate.stringify(true),
      predicate.stringify(false),
      previous.signLine1,
      previous.signLine3,
      previous.signLine4,
      predicate, null
    );
  }
}
