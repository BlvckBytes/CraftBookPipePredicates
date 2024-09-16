package me.blvckbytes.craft_book_pipe_predicates;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum CommandAction {
  GET,
  SET,
  REMOVE
  ;

  public static final List<String> NAMES = ImmutableList.copyOf(
    Arrays.stream(values()).map(Enum::name).collect(Collectors.toList())
  );

  public static @Nullable CommandAction fromString(String value) {
    try {
      return CommandAction.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
