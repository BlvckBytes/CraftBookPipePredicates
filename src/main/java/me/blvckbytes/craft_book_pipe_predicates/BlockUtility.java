package me.blvckbytes.craft_book_pipe_predicates;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Piston;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.inventory.DoubleChestInventory;
import org.jetbrains.annotations.Nullable;

public class BlockUtility {

  // TODO: These could use a review and some test-cases... :)

  private static final BlockFace[] POSSIBLE_CONTAINER_PISTON_FACES = {
    BlockFace.UP, BlockFace.DOWN,
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  private static final BlockFace[] PISTON_SIGN_FACES = {
    BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
  };

  public static @Nullable Sign getPistonSign(Block pistonBlock) {
    for (var face : PISTON_SIGN_FACES) {
      var faceBlock = pistonBlock.getRelative(face);

      if (!(faceBlock.getState() instanceof Sign sign))
        continue;

      if (!sign.getLine(1).equalsIgnoreCase(MarkerConstants.PIPE_MARKER))
        continue;

      return sign;
    }

    return null;
  }

  public static @Nullable Block resolvePistonBlock(Block block) {
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

  public static @Nullable Block getAttachedPiston(Block containerBlock) {
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
