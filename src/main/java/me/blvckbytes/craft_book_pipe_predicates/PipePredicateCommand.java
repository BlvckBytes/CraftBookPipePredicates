package me.blvckbytes.craft_book_pipe_predicates;

import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PipePredicateCommand implements CommandExecutor, TabCompleter {

  private final PredicateDataHandler dataHandler;
  private final PredicateHelper predicateHelper;
  private final TranslationLanguage language;

  public PipePredicateCommand(PredicateDataHandler dataHandler, PredicateHelper predicateHelper, TranslationLanguage language) {
    this.dataHandler = dataHandler;
    this.predicateHelper = predicateHelper;
    this.language = language;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player player))
      return false;

    CommandAction action;

    if (args.length == 0 || (action = CommandAction.fromString(args[0])) == null) {
      player.sendMessage("§cUsage: /" + label + " <" + CommandAction.NAMES + "> [expression]");
      return true;
    }

    var pistonBlock = BlockUtility.resolvePistonBlock(player.getTargetBlock(null, 5));

    if (pistonBlock == null) {
      player.sendMessage("§cPlease look at a pipe-output (container, piston or sign)");
      return true;
    }

    var pistonSign = BlockUtility.getPistonSign(pistonBlock);

    if (pistonSign == null) {
      player.sendMessage("§cCould not locate pipe-sign");
      return true;
    }

    switch (action) {
      case REMOVE -> {
        PredicateData predicateData;

        if ((predicateData = dataHandler.remove(pistonSign)) == null) {
          player.sendMessage("§cHad no predicate stored");
          return true;
        }

        predicateData.restoreLines(pistonSign);

        player.sendMessage("§aPredicate removed successfully");
        return true;
      }

      case GET -> {
        var predicateData = dataHandler.access(pistonSign);

        if (predicateData == null) {
          player.sendMessage("§cThere's currently no predicate stored on this pipe");
          return true;
        }

        player.sendMessage("§8§m------------------------------");

        player.spigot().sendMessage(
          new ComponentBuilder("§7Current token predicate: ")
            .append(
              new ComponentBuilder("§a" + predicateData.tokensPredicate())
                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + label + " set " + predicateData.tokensPredicate()))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aClick to suggest").create()))
                .create()
            )
            .create()
        );

        player.spigot().sendMessage(
          new ComponentBuilder("§7Current expanded predicate: ")
            .append(
              new ComponentBuilder("§a" + predicateData.expandedPredicate())
                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + label + " set " + predicateData.expandedPredicate()))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aClick to suggest").create()))
                .create()
            )
            .create()
        );

        if (predicateData.parseException() != null)
          player.sendMessage("§7Error: §c" + predicateHelper.createExceptionMessage(predicateData.parseException()));

        player.sendMessage("§8§m------------------------------");
        return true;
      }

      case SET -> {
        ItemPredicate predicate;

        try {
          var tokens = predicateHelper.parseTokens(args, 1);
          predicate = predicateHelper.parsePredicate(language, tokens);
        } catch (ItemPredicateParseException e) {
          player.sendMessage(predicateHelper.createExceptionMessage(e));
          return true;
        }

        if (predicate == null) {
          player.sendMessage("§cPlease provide a non-empty predicate");
          return true;
        }

        var existingPredicateData = dataHandler.access(pistonSign);

        PredicateData newPredicateData;

        if (existingPredicateData != null)
          newPredicateData = PredicateData.makeUpdate(predicate, existingPredicateData);
        else
          newPredicateData = PredicateData.makeInitial(predicate, pistonSign);

        pistonSign.setLine(0, "§" + MarkerConstants.PREDICATE_OK_COLOR + MarkerConstants.PREDICATE_MARKER);
        pistonSign.setLine(2, "");
        pistonSign.setLine(3, "");
        pistonSign.update(true, false);

        dataHandler.store(newPredicateData, pistonSign);

        player.sendMessage("§7Set predicate: §a" + predicate.stringify(true));
        return true;
      }
      default -> { return true; }
    }
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player player))
      return null;

    if (args.length == 1)
      return CommandAction.NAMES;

    if (CommandAction.fromString(args[0]) != CommandAction.SET)
      return null;

    try {
      var tokens = predicateHelper.parseTokens(args, 1);
      var completions = predicateHelper.createCompletion(TranslationLanguage.ENGLISH_US, tokens);

      if (completions.expandedPreviewOrError() != null)
        showActionBarMessage(player, completions.expandedPreviewOrError());

      return completions.suggestions();
    } catch (ItemPredicateParseException e) {
      showActionBarMessage(player, predicateHelper.createExceptionMessage(e));
      return null;
    }
  }

  private void showActionBarMessage(Player player, String message) {
    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
  }
}
