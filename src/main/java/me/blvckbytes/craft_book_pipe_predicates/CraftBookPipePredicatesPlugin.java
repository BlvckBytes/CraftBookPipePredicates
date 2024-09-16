package me.blvckbytes.craft_book_pipe_predicates;

import me.blvckbytes.item_predicate_parser.ItemPredicateParserPlugin;
import me.blvckbytes.item_predicate_parser.parse.PredicateParserFactory;
import me.blvckbytes.item_predicate_parser.translation.*;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

public class CraftBookPipePredicatesPlugin extends JavaPlugin implements Listener {

  @Override
  public void onEnable() {
    var logger = getLogger();

    try {
      var parserPlugin = ItemPredicateParserPlugin.getInstance();

      if (parserPlugin == null)
        throw new IllegalStateException("Depending on ItemPredicateParser to be successfully loaded");

      var translationRegistry = parserPlugin.getLanguageRegistry().getTranslationRegistry(TranslationLanguage.ENGLISH_US);
      var parserFactory = new PredicateParserFactory(translationRegistry);
      var pipeEventHandler = new PipeEventHandler(this, parserFactory);

      Bukkit.getServer().getPluginManager().registerEvents(pipeEventHandler, this);
      Objects.requireNonNull(getCommand("pipepredicate")).setExecutor(new PipePredicateCommand(pipeEventHandler, parserFactory));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not initialize plugin", e);
      Bukkit.getPluginManager().disablePlugin(this);
    }
  }
}
