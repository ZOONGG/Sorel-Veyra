package dev.veyra.client.config;
public class ConfigManager { public static class Config {public String getName(){return "config";}} public Config getConfig(){return new Config();}public void loadConfigByName(String name){} }
