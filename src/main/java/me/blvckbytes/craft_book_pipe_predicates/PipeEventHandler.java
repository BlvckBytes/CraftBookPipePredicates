package me.blvckbytes.craft_book_pipe_predicates;

import com.sk89q.craftbook.mechanics.pipe.PipeFilterEvent;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PipeEventHandler implements Listener, PredicateCache {

  private static final BlockFace[] PISTON_SIGN_FACES = {
    BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
  };

  private static final Side[] SIGN_SIDES = Side.values();
  private static final String PREDICATE_MARKER_LINE_CONTENT = "§6Predicate";
  private static final String PREDICATE_MARKER_LINE_INVALID = "§cPredicate Invalid";

  private final PredicateHelper predicateHelper;
  private final NamespacedKey pipePredicateKey, pipeLine2Key, pipeLine3Key;
  private final Map<Block, CachedPredicate> predicateCache;

  public PipeEventHandler(Plugin plugin, PredicateHelper predicateHelper) {
    this.predicateHelper = predicateHelper;
    this.pipePredicateKey = new NamespacedKey(plugin, "pipe-predicate");
    this.pipeLine2Key = new NamespacedKey(plugin, "pipe-line-2");
    this.pipeLine3Key = new NamespacedKey(plugin, "pipe-line-3");
    this.predicateCache = new WeakHashMap<>();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onSignChange(SignChangeEvent event) {
    if (!event.getLines()[1].equalsIgnoreCase("[pipe]"))
      return;

    var dataContainer = ((Sign) event.getBlock().getState()).getPersistentDataContainer();

    if (!dataContainer.has(pipePredicateKey))
      return;

    event.setCancelled(true);
    event.getPlayer().sendMessage("§cPlease remove predicate mode before trying to manually edit the pipe");
  }

  @EventHandler
  public void onPipeFilter(PipeFilterEvent event) {
    var pistonBlock = event.getBlock();
    var pistonSign = getPistonSign(pistonBlock);

    if (pistonSign == null)
      return;

    var predicateSide = findPipeSignSide(pistonSign);

    if (predicateSide == null)
      return;

    var dataContainer = pistonSign.getPersistentDataContainer();
    var expression = dataContainer.get(pipePredicateKey, PersistentDataType.STRING);

    if (expression == null)
      return;

    var pipePredicate = predicateCache.computeIfAbsent(pistonBlock, key -> {
      var predicate = new CachedPredicate();
      predicate.update(predicateHelper, expression);
      return predicate;
    });

    if (!pipePredicate.isValid()) {
      predicateSide.setLine(0, PREDICATE_MARKER_LINE_INVALID);
      pistonSign.update(true, false);
      return;
    }

    var result = new ArrayList<ItemStack>();

    for (var item : event.getItems()) {
      if (pipePredicate.test(item))
        result.add(item);
    }

    event.setFilteredItems(result);
  }

  @Override
  public @Nullable String storePredicate(Block pistonBlock, @NotNull ItemPredicate predicate) {
    var pistonSign = getPistonSign(pistonBlock);

    if (pistonSign == null)
      return null;

    var pipeSignSide = findPipeSignSide(pistonSign);

    if (pipeSignSide == null)
      return null;

    predicateCache.remove(pistonBlock);

    var expressionString = predicate.stringify(true);
    var dataContainer = pistonSign.getPersistentDataContainer();

    dataContainer.set(pipePredicateKey, PersistentDataType.STRING, expressionString);
    dataContainer.set(pipeLine2Key, PersistentDataType.STRING, pipeSignSide.getLine(2));
    dataContainer.set(pipeLine3Key, PersistentDataType.STRING, pipeSignSide.getLine(3));

    pipeSignSide.setLine(0, PREDICATE_MARKER_LINE_CONTENT);
    pipeSignSide.setLine(2, "");
    pipeSignSide.setLine(3, "");

    pistonSign.update(true, false);

    return expressionString;
  }

  @Override
  public boolean removePredicate(Block pistonBlock) {
    var pistonSign = getPistonSign(pistonBlock);

    if (pistonSign == null)
      return false;

    var dataContainer = pistonSign.getPersistentDataContainer();

    if (!dataContainer.has(pipePredicateKey))
      return false;

    var pipeSignSide = findPipeSignSide(pistonSign);

    if (pipeSignSide == null)
      return false;

    pipeSignSide.setLine(0, "");

    String line;

    if ((line = dataContainer.get(pipeLine2Key, PersistentDataType.STRING)) != null)
      pipeSignSide.setLine(2, line);

    if ((line = dataContainer.get(pipeLine3Key, PersistentDataType.STRING)) != null)
      pipeSignSide.setLine(3, line);

    dataContainer.remove(pipeLine2Key);
    dataContainer.remove(pipeLine3Key);
    dataContainer.remove(pipePredicateKey);

    pistonSign.update(true, false);
    predicateCache.remove(pistonBlock);

    return true;
  }

  @Override
  public @Nullable String retrievePredicate(Block pistonBlock) {
    var pistonSign = getPistonSign(pistonBlock);

    if (pistonSign == null || findPipeSignSide(pistonSign) == null)
      return null;

    var expression = pistonSign.getPersistentDataContainer().get(pipePredicateKey, PersistentDataType.STRING);

    if (expression == null)
      return "";

    return expression;
  }

  private @Nullable SignSide findPipeSignSide(Sign sign) {
    for (var side : SIGN_SIDES) {
      var signSide = sign.getSide(side);
      var lines = signSide.getLines();

      if (lines[1].equalsIgnoreCase("[pipe]"))
        return signSide;
    }

    return null;
  }

  private @Nullable Sign getPistonSign(Block pistonBlock) {
    for (var face : PISTON_SIGN_FACES) {
      var faceBlock = pistonBlock.getRelative(face);

      if (!(faceBlock.getState() instanceof Sign sign))
        continue;

      return sign;
    }

    return null;
  }
}
