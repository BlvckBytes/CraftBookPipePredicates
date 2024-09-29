package me.blvckbytes.craft_book_pipe_predicates;

import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.PredicateState;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import org.bukkit.inventory.ItemStack;

public class CachedPredicate {

  private ItemPredicate predicate;
  private boolean isValid = false;

  public void update(PredicateHelper predicateHelper, String expression) {
    try {
      var tokens = predicateHelper.parseTokens(expression);
      this.predicate = predicateHelper.parsePredicate(TranslationLanguage.ENGLISH_US, tokens);
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
