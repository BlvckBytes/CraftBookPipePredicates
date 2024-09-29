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
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Piston;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PipePredicateCommand implements CommandExecutor, TabCompleter {

  private static final BlockFace[] POSSIBLE_CONTAINER_PISTON_FACES = {
    BlockFace.UP, BlockFace.DOWN,
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  private static final int MAX_COMPLETER_RESULTS = 30;

  private final PredicateCache predicateCache;
  private final PredicateHelper predicateHelper;

  public PipePredicateCommand(PredicateCache predicateCache, PredicateHelper predicateHelper) {
    this.predicateCache = predicateCache;
    this.predicateHelper = predicateHelper;
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

    var pistonBlock = resolvePistonBlock(player.getTargetBlock(null, 5));

    if (pistonBlock == null) {
      player.sendMessage("§cPlease look at a pipe-output (container, piston or sign)");
      return true;
    }

    switch (action) {
      case REMOVE -> {
        if (predicateCache.removePredicate(pistonBlock)) {
          player.sendMessage("§aPredicate removed successfully");
          return true;
        }
      }

      case GET -> {
        var expression = predicateCache.retrievePredicate(pistonBlock);

        if (expression != null) {
          if (expression.isBlank()) {
            player.sendMessage("§cThere's currently no predicate stored on this pipe");
            return true;
          }

          var expressionComponent = new TextComponent("§a" + expression);
          expressionComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aClick to suggest edit-command")));
          expressionComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/pipepredicate set " + expression));

          var messageComponent = new ComponentBuilder()
            .append("§7Current predicate: ")
            .append(expressionComponent)
            .build();

          player.spigot().sendMessage(messageComponent);
          return true;
        }
      }

      case SET -> {
        ItemPredicate predicate;

        try {
          var tokens = predicateHelper.parseTokens(args, 1);
          predicate = predicateHelper.parsePredicate(TranslationLanguage.ENGLISH_US, tokens);
        } catch (ItemPredicateParseException e) {
          player.sendMessage(predicateHelper.createExceptionMessage(e));
          return true;
        }

        if (predicate == null) {
          player.sendMessage("§cPlease provide a non-empty predicate");
          return true;
        }

        var expression = predicateCache.storePredicate(pistonBlock, predicate);

        if (expression != null) {
          player.sendMessage("§7Set predicate: §a" + expression);
          return true;
        }
      }
    }

    player.sendMessage("§cThis is not a pipe output in predicate mode");
    return true;
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

  private @Nullable Block resolvePistonBlock(Block block) {
    if (block.getType() == Material.PISTON)
      return block;

    var blockData = block.getBlockData();

    if (blockData instanceof Sign || blockData instanceof WallSign) {
      BlockFace mountingFace = BlockFace.DOWN;

      if (blockData instanceof Directional directional)
        mountingFace = directional.getFacing().getOppositeFace();

      var mountingBlock = block.getRelative(mountingFace);
      return mountingBlock.getType() == Material.PISTON ? mountingBlock : null;
    }

    if (!(block.getState() instanceof Container container))
      return null;

    var containerInventory = container.getInventory();

    Location location;

    if (containerInventory instanceof DoubleChestInventory doubleChestInventory) {
      Block locationBlock;

      if ((location = doubleChestInventory.getLeftSide().getLocation()) != null) {
        if ((locationBlock = getAttachedPiston(location.getBlock())) != null)
          return locationBlock;
      }

      if ((location = doubleChestInventory.getRightSide().getLocation()) != null) {
        if ((locationBlock = getAttachedPiston(location.getBlock())) != null)
          return locationBlock;
      }

      return null;
    }

    if ((location = container.getInventory().getLocation()) != null)
      return getAttachedPiston(location.getBlock());

    return null;
  }

  private @Nullable Block getAttachedPiston(Block containerBlock) {
    for (var currentFace : POSSIBLE_CONTAINER_PISTON_FACES) {
      var currentBlock = containerBlock.getRelative(currentFace);

      if (currentBlock.getType() != Material.PISTON)
        continue;

      var pistonFacing = ((Piston) currentBlock.getBlockData()).getFacing();

      if (pistonFacing != currentFace.getOppositeFace())
        continue;

      return currentBlock;
    }

    return null;
  }
}
