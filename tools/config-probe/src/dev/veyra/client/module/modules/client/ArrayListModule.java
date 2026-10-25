package dev.veyra.client.module.modules.client;
public class ArrayListModule {public static int hudX=5,hudY=5;public static final String HUDX_prefix="HUDX~ ",HUDY_prefix="HUDY~ ",FILTER_PREFIX="arraylist-filter~ ";
public static int getHudX(){return hudX;}public static int getHudY(){return hudY;}public static void setHudX(int x){hudX=x;}public static void setHudY(int y){hudY=y;}
public static String serializeFilterMembers(){return "";}public static boolean hasPersistedFilterMembers(){return true;}public static void loadPersistedFilterMembers(){}public static void deserializeFilterMembers(String x){}public static void persistFilterMembers(){} }
