package dev.veyra.client.config;

import dev.veyra.client.main.Veyra;
import dev.veyra.client.utils.Utils;
import dev.veyra.client.clickgui.components.CategoryComponent;
import dev.veyra.client.clickgui.ClickGui;
import dev.veyra.client.module.modules.client.ArrayListModule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ClientConfig {
   private final File configFile;
   private boolean applyingConfig;
   private final File weaveDir = new File(System.getProperty("user.home"), ".weave");
   private final File configDir = new File(weaveDir, "veyra");;
   private final String fileName = "client.scfg";
   private final String clickGuiPosPrefix = "clickgui-pos~ ";
   private final String loadedConfigPrefix = "loaded-cfg~ ";
   private final String favoritesCategoryPrefix = "favorites-category~ ";

   public ClientConfig(){
      if(!configDir.exists()){
         configDir.mkdir();
      }

      configFile = new File(configDir, fileName);
      if(!configFile.exists()){
         try {
            configFile.createNewFile();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   public synchronized void saveConfig() {
      if (applyingConfig) return;
      List<String> config = new ArrayList<>();
      config.add(clickGuiPosPrefix + getClickGuiPos());
      config.add(loadedConfigPrefix + Veyra.configManager.getConfig().getName());
      config.add(ArrayListModule.HUDX_prefix + ArrayListModule.getHudX());
      config.add(ArrayListModule.HUDY_prefix + ArrayListModule.getHudY());
      config.add(ArrayListModule.FILTER_PREFIX + ArrayListModule.serializeFilterMembers());
      config.add(favoritesCategoryPrefix + ClickGui.isFavoritesCategoryEnabled());

      java.nio.file.Path destination = configFile.toPath();
      java.nio.file.Path temporary = null;
      try {
         temporary = java.nio.file.Files.createTempFile(destination.getParent(), "client-", ".tmp");
         java.nio.file.Files.write(temporary, config, java.nio.charset.StandardCharsets.UTF_8);
         try {
            java.nio.file.Files.move(temporary, destination, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
         } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            java.nio.file.Files.move(temporary, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         if (temporary != null) try { java.nio.file.Files.deleteIfExists(temporary); }
         catch (IOException ignored) { }
      }
   }

   public synchronized void applyConfig(){
      applyingConfig = true;
      try {
      List<String> config = this.parseConfigFile();
      String legacyArrayListFilter = null;

      for(String line : config){
         if(line.startsWith(clickGuiPosPrefix)){
            loadClickGuiCoords(line.replace(clickGuiPosPrefix, ""));
         } else if(line.startsWith(loadedConfigPrefix)){
            Veyra.configManager.loadConfigByName(line.replace(loadedConfigPrefix, ""));
         } else if (line.startsWith(ArrayListModule.HUDX_prefix)) {
            try {
               ArrayListModule.setHudX(Integer.parseInt(line.replace(ArrayListModule.HUDX_prefix, "")));
            } catch (Exception e) {e.printStackTrace();}
         } else if (line.startsWith(ArrayListModule.HUDY_prefix)) {
            try {
               ArrayListModule.setHudY(Integer.parseInt(line.replace(ArrayListModule.HUDY_prefix, "")));
            } catch (Exception e) {e.printStackTrace();}
         } else if (line.startsWith(ArrayListModule.FILTER_PREFIX)) {
            legacyArrayListFilter = line.substring(ArrayListModule.FILTER_PREFIX.length());
         } else if (line.startsWith(favoritesCategoryPrefix)) {
            ClickGui.setFavoritesCategoryEnabled(Boolean.parseBoolean(
                    line.substring(favoritesCategoryPrefix.length())));
         }
      }

      if (ArrayListModule.hasPersistedFilterMembers()) {
         ArrayListModule.loadPersistedFilterMembers();
      } else if (legacyArrayListFilter != null) {
         ArrayListModule.deserializeFilterMembers(legacyArrayListFilter);
         ArrayListModule.persistFilterMembers();
      }
      } finally { applyingConfig = false; }
   }

   private List<String> parseConfigFile() {
      List<String> configFileContents = new java.util.ArrayList<>();
      try (Scanner reader = new Scanner(this.configFile)) {
         while (reader.hasNextLine()) {
            configFileContents.add(reader.nextLine());
         }
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      }
      return configFileContents;
   }

   private void loadClickGuiCoords(String decryptedString) {
      for (String what : decryptedString.split("/")){
         for (CategoryComponent cat : Veyra.clickGui.getCategoryList()) {
            if(what.startsWith(cat.categoryName.name())){
               List<String> cfg = Utils.Java.StringListToList(what.split("~"));
               cat.setX(Integer.parseInt(cfg.get(1)));
               cat.setY(Integer.parseInt(cfg.get(2)));
               cat.setOpened(Boolean.parseBoolean(cfg.get(3)));
            }
         }
      }
   }

   public String getClickGuiPos() {
      StringBuilder posConfig = new StringBuilder();
      for (CategoryComponent cat : Veyra.clickGui.getCategoryList()) {
         posConfig.append(cat.categoryName.name());
         posConfig.append("~");
         posConfig.append(cat.getX());
         posConfig.append("~");
         posConfig.append(cat.getY());
         posConfig.append("~");
         posConfig.append(cat.isOpened());
         posConfig.append("/");
      }
      return posConfig.length() == 0 ? "" : posConfig.substring(0, posConfig.length() - 1);

   }
}
