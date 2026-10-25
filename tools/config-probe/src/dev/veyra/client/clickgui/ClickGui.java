package dev.veyra.client.clickgui;
import java.util.*;import dev.veyra.client.clickgui.components.CategoryComponent;
public class ClickGui {private final List<CategoryComponent> list=List.of(new CategoryComponent());public List<CategoryComponent> getCategoryList(){return list;}public static boolean isFavoritesCategoryEnabled(){return false;}public static void setFavoritesCategoryEnabled(boolean value){} }
