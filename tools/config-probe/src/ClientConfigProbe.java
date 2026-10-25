import java.nio.file.*;import java.util.Arrays;
import dev.veyra.client.main.Veyra;import dev.veyra.client.config.ClientConfig;import dev.veyra.client.module.modules.client.ArrayListModule;
public class ClientConfigProbe {
static void reopen(){ArrayListModule.hudX=5;ArrayListModule.hudY=5;Veyra.clientConfig=new ClientConfig();Veyra.clientConfig.applyConfig();}
public static void main(String[] args)throws Exception{
System.setProperty("user.home",Path.of(args[0]).toAbsolutePath().toString());
Path file=Path.of(System.getProperty("user.home"),".weave/veyra/client.scfg");Files.createDirectories(file.getParent());
String fixture="clickgui-pos~ Combat~34~28~true\nloaded-cfg~ config\nHUDX~ 321\nHUDY~ 117\narraylist-filter~ timer\nfavorites-category~ false\n";
Files.writeString(file,fixture);byte[] before=Files.readAllBytes(file);reopen();
if(ArrayListModule.hudX!=321||ArrayListModule.hudY!=117)throw new AssertionError("Saved HUD position not loaded");
if(!Arrays.equals(before,Files.readAllBytes(file)))throw new AssertionError("Loading menu coordinates rewrote HUD position on disk");
ArrayListModule.hudX=123;ArrayListModule.hudY=91;Veyra.clientConfig.saveConfig();byte[] saved=Files.readAllBytes(file);reopen();
if(ArrayListModule.hudX!=123||ArrayListModule.hudY!=91||!Arrays.equals(saved,Files.readAllBytes(file)))throw new AssertionError("Position changed after save/restart");
Files.writeString(file,"clickgui-pos~ Combat~bad~28~true\nHUDX~ 2\nHUDY~ 2\n");
try{Veyra.clientConfig.applyConfig();throw new AssertionError("Expected malformed coordinate");}catch(NumberFormatException expected){}
ArrayListModule.hudX=77;Veyra.clientConfig.saveConfig();if(!Files.readString(file).contains("HUDX~ 77"))throw new AssertionError("Load failure left saving disabled");
try(var children=Files.list(file.getParent())){if(children.anyMatch(p->p.toString().endsWith(".tmp")))throw new AssertionError("Temporary save leaked");}
System.out.println("Production ClientConfig: startup read-only, HUD save/restart, failed-load recovery, atomic save cleanup PASS");
}}
