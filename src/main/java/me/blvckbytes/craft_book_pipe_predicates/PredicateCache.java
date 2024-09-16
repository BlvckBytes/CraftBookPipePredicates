package me.blvckbytes.craft_book_pipe_predicates;

import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PredicateCache {
  /**
   * @return Predicate expression if it has been written successfully
   */
  @Nullable String storePredicate(Block pistonBlock, @NotNull ItemPredicate predicate);

  boolean removePredicate(Block pistonBlock);

  @Nullable String retrievePredicate(Block pistonBlock);

}
