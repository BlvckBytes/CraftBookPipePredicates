package me.blvckbytes.craft_book_pipe_predicates;

import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.parse.PredicateParserFactory;
import me.blvckbytes.item_predicate_parser.parse.TokenParser;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.PredicateState;
import org.bukkit.inventory.ItemStack;

public class CachedPredicate {

  private ItemPredicate predicate;
  private boolean isValid = false;

  public void update(PredicateParserFactory parserFactory, String expression) {
    try {
      this.predicate = parserFactory.create(TokenParser.parseTokens(expression), false).parseAst();
      this.isValid = true;
    } catch (ItemPredicateParseException e) {
      this.isValid = false;
    }
  }

  public boolean isValid() {
    return isValid;
  }

  public boolean test(ItemStack item) {
    if (this.predicate == null)
      return false;
    return predicate.test(new PredicateState(item));
  }
}
