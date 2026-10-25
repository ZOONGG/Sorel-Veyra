import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Standalone fixture: production cards/input/config, no Minecraft lifecycle. */
public class BindUxProbe {
    public static boolean focused = true;
    public static boolean isFocused() { return focused; }
    public static final Set<Integer> physicalKeys = new HashSet<>();
    public static boolean physicalState(int code) { return physicalKeys.contains(code); }
    static ClassLoader loader;
    static Class<?> screenClass, moduleClass, rectClass;
    static Object screen;
    static int checks;
    static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    static Object get(Object obj, String name) throws Exception {
        Field f = obj.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(obj);
    }
    static void set(Object obj, String name, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(name); f.setAccessible(true); f.set(obj, value);
    }
    static Object invoke(Object obj, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = obj.getClass().getDeclaredMethod(name, types); m.setAccessible(true); return m.invoke(obj,args);
    }
    static void begin(Object module) throws Exception { invoke(screen,"beginBind",new Class[]{moduleClass},module); }
    static int key(Object module) throws Exception { return (int)moduleClass.getMethod("getKeycode").invoke(module); }
    static void bind(Object module, int key) throws Exception { moduleClass.getMethod("setBind",int.class).invoke(module,key); }
    static Object rect(int x,int y,int w,int h) throws Exception {
        var ctor=rectClass.getDeclaredConstructor(int.class,int.class,int.class,int.class);ctor.setAccessible(true);return ctor.newInstance(x,y,w,h);
    }
    static Object dummy(String name) throws Exception {
        return loader.loadClass("dev.veyra.probe.DummyModule").getConstructor(String.class).newInstance(name);
    }
    static class Isolated extends ClassLoader {
        Isolated() { super(BindUxProbe.class.getClassLoader()); }
        Class<?> define(byte[] bytes) { return defineClass(null, bytes, 0, bytes.length); }
        @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith("dev.veyra.")) return super.loadClass(name,resolve);
            synchronized(getClassLoadingLock(name)) {
                Class<?> result = findLoadedClass(name);
                if (result == null) try(var input = getParent().getResourceAsStream(name.replace('.','/')+".class")) {
                    if(input==null)throw new ClassNotFoundException(name);
                    ClassNode node=new ClassNode();new ClassReader(input).accept(node,0);
                    if(name.equals("dev.veyra.client.module.Module")) node.methods.removeIf(m->m.name.equals("<clinit>"));
                    if(name.equals("dev.veyra.client.coordination.input.InputLeaseManager")) {
                        for(var m:node.methods)if(m.name.equals("physicalState")) {
                            m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!=null)m.localVariables.clear();
                            m.instructions.add(new VarInsnNode(Opcodes.ILOAD,0));
                            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"BindUxProbe","physicalState","(I)Z",false));
                            m.instructions.add(new InsnNode(Opcodes.IRETURN));m.maxStack=1;m.maxLocals=1;
                        }
                    }
                    if(name.equals("dev.veyra.client.clickgui.ClickGui")) {
                        // Offscreen fixture controls focus without activating a desktop window.
                        for(var m:node.methods)for(var instruction:m.instructions) {
                            if(instruction instanceof MethodInsnNode call && call.owner.equals("org/lwjgl/opengl/Display") && call.name.equals("isActive")) {
                                call.owner="BindUxProbe";call.name="isFocused";
                            }
                        }
                    }
                    ClassWriter writer=new ClassWriter(0);node.accept(writer);
                    byte[] bytes=writer.toByteArray();result=defineClass(name,bytes,0,bytes.length);
                } catch(java.io.IOException e) {throw new ClassNotFoundException(name,e);}
                if(resolve)resolveClass(result);return result;
            }
        }
    }
    public static void main(String[] args) throws Exception {
        Display.setDisplayMode(new DisplayMode(64,64));Display.setLocation(-12000,-12000);
        Display.setTitle("Veyra isolated bind UX verification");Display.create();
        try {
            loader=new Isolated();
            moduleClass=loader.loadClass("dev.veyra.client.module.Module");
            ClassWriter w=new ClassWriter(0);
            w.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"dev/veyra/probe/DummyModule",null,"dev/veyra/client/module/Module",null);
            var c=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","(Ljava/lang/String;)V",null,null);c.visitCode();
            c.visitVarInsn(Opcodes.ALOAD,0);c.visitVarInsn(Opcodes.ALOAD,1);
            c.visitFieldInsn(Opcodes.GETSTATIC,"dev/veyra/client/module/Module$ModuleCategory","Combat","Ldev/veyra/client/module/Module$ModuleCategory;");
            c.visitInsn(Opcodes.ICONST_0);c.visitMethodInsn(Opcodes.INVOKESPECIAL,"dev/veyra/client/module/Module","<init>","(Ljava/lang/String;Ldev/veyra/client/module/Module$ModuleCategory;I)V",false);
            c.visitInsn(Opcodes.RETURN);c.visitMaxs(4,2);c.visitEnd();w.visitEnd();((Isolated)loader).define(w.toByteArray());
            Class<?> manager=loader.loadClass("dev.veyra.client.module.ModuleManager");
            manager.getField("initialized").setBoolean(null,true);
            Object modules=manager.getConstructor().newInstance();
            loader.loadClass("dev.veyra.client.main.Veyra").getField("moduleManager").set(null,modules);
            screenClass=loader.loadClass("dev.veyra.client.clickgui.ClickGui");
            rectClass=loader.loadClass("dev.veyra.client.clickgui.ClickGui$Rect");
            screen=screenClass.getConstructor().newInstance();
            set(screen,"contentClip",rect(0,0,640,450));
            verifyInput();verifyLegacyConfig();verifyMovementInput();verifyLegacyRoot(Path.of(args[0]).getParent());verifyReservedInputs();verifyExpansion();render(args[0]);
            check(GL11.glGetError()==0,"OpenGL error");
            System.out.println("Bind UX production fixture: "+checks+" checks PASS; cards rendered; GL error=0; no Minecraft lifecycle");
        } finally { Display.destroy(); }
    }
    static void verifyInput() throws Exception {
        Object module=dummy("Sprint");bind(module,47);
        begin(module);screenClass.getMethod("keyTyped",char.class,int.class).invoke(screen,'\0',1);
        check(key(module)==47 && get(screen,"bindingModule")==null,"Escape must preserve previous bind");
        begin(module);screenClass.getMethod("keyTyped",char.class,int.class).invoke(screen,'r',19);
        check(key(module)==19,"Keyboard assignment");
        check(!(boolean)moduleClass.getMethod("isEnabled").invoke(module),"Assignment toggled module");
        check((boolean)loader.loadClass("dev.veyra.client.module.bind.CapturedBindInput").getMethod("isSuppressed",int.class,boolean.class).invoke(null,19,true),"Assigned press leaked");
        begin(module);screenClass.getMethod("keyTyped",char.class,int.class).invoke(screen,'\0',14);
        check(key(module)==0,"Backspace did not clear");
        bind(module,47);begin(module);screenClass.getMethod("keyTyped",char.class,int.class).invoke(screen,'\0',211);
        check(key(module)==0,"Delete did not clear");
        bind(module,47);begin(module);set(screen,"bindingStartedNanos",System.nanoTime()-2_900_000_000L);
        invoke(screen,"updateBindCapture",new Class[]{});check(key(module)==47 && get(screen,"bindingModule")==module,"Capture ended before three seconds");
        set(screen,"bindingStartedNanos",System.nanoTime()-3_000_000_000L);
        invoke(screen,"updateBindCapture",new Class[]{});check(key(module)==0 && get(screen,"bindingModule")==null,"Three-second timeout did not clear");
        bind(module,47);begin(module);focused=false;invoke(screen,"updateBindCapture",new Class[]{});focused=true;
        check(key(module)==47 && get(screen,"bindingModule")==null,"Focus loss must cancel capture");
        begin(module);screenClass.getMethod("mouseClicked",int.class,int.class,int.class).invoke(screen,620,430,3);
        check(key(module)==-97,"Mouse side button assignment");
        begin(module);set(screen,"searchRect",rect(500,0,100,20));
        screenClass.getMethod("mouseClicked",int.class,int.class,int.class).invoke(screen,510,10,0);
        check(get(screen,"bindingModule")==null && (boolean)get(screen,"searchFocused"),"Search click was captured");
        screenClass.getMethod("keyTyped",char.class,int.class).invoke(screen,'r',19);
        check(get(screen,"searchQuery").equals("r") && key(module)==-97,"Search typed into bind");
        set(screen,"searchFocused",false);set(screen,"searchQuery","");
        bind(module,47);
        invoke(screen,"drawHeaderBind",new Class[]{moduleClass,rectClass,int.class,int.class,boolean.class,int.class},module,rect(20,20,244,42),189,40,true,0xFF8B5CF6);
        screenClass.getMethod("mouseClicked",int.class,int.class,int.class).invoke(screen,187,40,0);
        check(key(module)==0 && get(screen,"settingsModule")==null,"Cross must clear without expanding");
        ((List<?>)get(screen,"actionHitboxes")).clear();
        invoke(screen,"drawHeaderBind",new Class[]{moduleClass,rectClass,int.class,int.class,boolean.class,int.class},module,rect(20,20,244,42),150,40,true,0xFF8B5CF6);
        screenClass.getMethod("mouseClicked",int.class,int.class,int.class).invoke(screen,150,40,0);
        check(get(screen,"bindingModule")==module && get(screen,"settingsModule")==null,"Header bind expanded card");
        Object waitingRect=invoke(screen,"drawHeaderBind",new Class[]{moduleClass,rectClass,int.class,int.class,boolean.class,int.class},module,rect(20,20,244,42),150,40,false,0xFF8B5CF6);
        set(screen,"bindingModule",null);
        Object idleRect=invoke(screen,"drawHeaderBind",new Class[]{moduleClass,rectClass,int.class,int.class,boolean.class,int.class},module,rect(20,20,244,42),150,40,false,0xFF8B5CF6);
        for(String dimension:new String[]{"x","y","width","height"})check(get(waitingRect,dimension).equals(get(idleRect,dimension)),"Capture changed button "+dimension);
        invoke(screen,"updateScreen",new Class[]{});set(screen,"bindingModule",null);
        check((long)getStatic(screenClass,"BIND_TIMEOUT_NANOS")==3_000_000_000L,"Timeout is not three seconds");
        System.out.println("Production ClickGui input: capture/remap, keyboard/mouse, cross, timeout boundary, Escape, Delete/Backspace, focus, search isolation PASS");
    }
    static Object getStatic(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
    static Object allocate(Class<?> type) throws Exception {
        Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);
        return ((sun.misc.Unsafe)f.get(null)).allocateInstance(type);
    }
    static void verifyMovementInput() throws Exception {
        // Allocate inert vanilla holders. No Minecraft/World/Player constructor or run loop.
        var minecraft=(net.minecraft.client.Minecraft)allocate(net.minecraft.client.Minecraft.class);
        Field singleton=net.minecraft.client.Minecraft.class.getDeclaredField("theMinecraft");singleton.setAccessible(true);singleton.set(null,minecraft);
        Field moduleMc=moduleClass.getDeclaredField("mc");moduleMc.setAccessible(true);moduleMc.set(null,minecraft);
        minecraft.gameSettings=(net.minecraft.client.settings.GameSettings)allocate(net.minecraft.client.settings.GameSettings.class);
        minecraft.thePlayer=(net.minecraft.client.entity.EntityPlayerSP)allocate(net.minecraft.client.entity.EntityPlayerSP.class);
        minecraft.theWorld=(net.minecraft.client.multiplayer.WorldClient)allocate(net.minecraft.client.multiplayer.WorldClient.class);
        minecraft.inGameHasFocus=true;
        var player=minecraft.thePlayer;
        player.movementInput=new net.minecraft.util.MovementInput();
        player.capabilities=new net.minecraft.entity.player.PlayerCapabilities();
        player.inventory=new net.minecraft.entity.player.InventoryPlayer(player);
        player.ticksExisted=100;
        var sprintKey=new net.minecraft.client.settings.KeyBinding("probe.sprint",29,"probe");
        minecraft.gameSettings.keyBindSprint=sprintKey;
        Class<?> sprint=loader.loadClass("dev.veyra.client.module.modules.movement.Sprint");
        Object autoSprint=sprint.getConstructor().newInstance();sprint.getMethod("onEnable").invoke(autoSprint);
        player.movementInput.moveForward=1;
        sprint.getMethod("updateVanillaSprintInput").invoke(null);check(sprintKey.isKeyDown(),"Forward must request vanilla sprint");
        player.movementInput.moveForward=0;player.movementInput.moveStrafe=1;
        sprint.getMethod("updateVanillaSprintInput").invoke(null);check(!sprintKey.isKeyDown(),"Strafe-only must not request sprint");
        player.movementInput.moveForward=1;player.movementInput.sneak=true;
        sprint.getMethod("updateVanillaSprintInput").invoke(null);check(!sprintKey.isKeyDown(),"Sneaking must release sprint input");
        player.movementInput.sneak=false;
        sprint.getMethod("pauseAutomationUntilTick",int.class).invoke(autoSprint,105);
        sprint.getMethod("updateVanillaSprintInput").invoke(null);check(!sprintKey.isKeyDown(),"WTap pause ignored");
        player.ticksExisted=106;sprint.getMethod("updateVanillaSprintInput").invoke(null);check(sprintKey.isKeyDown(),"Sprint did not resume after pause");
        player.isCollidedHorizontally=true;sprint.getMethod("updateVanillaSprintInput").invoke(null);check(!sprintKey.isKeyDown(),"Collision must release sprint input");
        player.isCollidedHorizontally=false;player.hurtTime=2;sprint.getMethod("updateVanillaSprintInput").invoke(null);check(!sprintKey.isKeyDown(),"Damage pause ignored");
        player.hurtTime=0;
        minecraft.currentScreen=(net.minecraft.client.gui.GuiScreen)screen;
        sprint.getMethod("updateVanillaSprintInput").invoke(null);check(!sprintKey.isKeyDown(),"GUI captured sprint input");
        minecraft.currentScreen=null;
        var remappedSprint=new net.minecraft.client.settings.KeyBinding("probe.sprint.remapped",54,"probe");
        minecraft.gameSettings.keyBindSprint=remappedSprint;sprint.getMethod("updateVanillaSprintInput").invoke(null);
        check(remappedSprint.isKeyDown() && !sprintKey.isKeyDown(),"Remapped Sprint not respected");
        sprint.getMethod("onDisable").invoke(autoSprint);check(!remappedSprint.isKeyDown(),"Sprint input stuck after disable");
        sprint.getMethod("onEnable").invoke(autoSprint);sprint.getMethod("updateVanillaSprintInput").invoke(null);
        physicalKeys.add(54);sprint.getMethod("onDisable").invoke(autoSprint);check(remappedSprint.isKeyDown(),"Disable discarded physical sprint key");
        physicalKeys.clear();
        Class<?> bridge=loader.loadClass("dev.veyra.client.module.modules.player.SafeWalk");Object assist=bridge.getConstructor().newInstance();
        Object condition=get(assist,"requireSneakKey");condition.getClass().getMethod("setEnabled",boolean.class).invoke(condition,true);
        Object blocks=get(assist,"holdingBlocks");blocks.getClass().getMethod("setEnabled",boolean.class).invoke(blocks,false);
        player.rotationPitch=80;player.onGround=false;player.movementInput.moveForward=0;player.movementInput.moveStrafe=0;
        var sneak=new net.minecraft.client.settings.KeyBinding("probe.sneak",46,"probe");minecraft.gameSettings.keyBindSneak=sneak;
        Class<?> tick=loader.loadClass("net.weavemc.api.event.TickEvent$Pre");
        Class<?> leases=loader.loadClass("dev.veyra.client.coordination.input.InputLeaseManager");
        bridge.getMethod("onEnable").invoke(assist);
        bridge.getMethod("onTick",tick).invoke(assist,new Object[]{null});
        check(!(boolean)leases.getMethod("owns",String.class,int.class).invoke(null,"bridge-assist",46),"Released Sneak activated assist");
        physicalKeys.add(46);bridge.getMethod("onTick",tick).invoke(assist,new Object[]{null});
        check((boolean)leases.getMethod("owns",String.class,int.class).invoke(null,"bridge-assist",46),"Held remapped Sneak did not activate assist");
        check(!sneak.isKeyDown(),"Physical Sneak prevented assisted unsneak on safe ground");
        var remappedSneak=new net.minecraft.client.settings.KeyBinding("probe.sneak.remapped",45,"probe");minecraft.gameSettings.keyBindSneak=remappedSneak;
        physicalKeys.add(45);bridge.getMethod("onTick",tick).invoke(assist,new Object[]{null});
        check(!(boolean)leases.getMethod("owns",String.class,int.class).invoke(null,"bridge-assist",46)
                && (boolean)leases.getMethod("owns",String.class,int.class).invoke(null,"bridge-assist",45),"Changing Sneak key retained stale ownership");
        physicalKeys.clear();bridge.getMethod("onTick",tick).invoke(assist,new Object[]{null});
        check(!(boolean)leases.getMethod("owns",String.class,int.class).invoke(null,"bridge-assist",45)
                && !(boolean)bridge.getMethod("shouldHardClampMovement").invoke(null),"Release retained Sneak or ledge guard");
        physicalKeys.add(45);bridge.getMethod("onTick",tick).invoke(assist,new Object[]{null});bridge.getMethod("onDisable").invoke(assist);
        check(remappedSneak.isKeyDown() && !(boolean)bridge.getMethod("shouldHardClampMovement").invoke(null),"Disable did not restore held vanilla Sneak");
        bridge.getMethod("onEnable").invoke(assist);bridge.getMethod("onTick",tick).invoke(assist,new Object[]{null});
        minecraft.currentScreen=(net.minecraft.client.gui.GuiScreen)screen;
        bridge.getMethod("onTick",tick).invoke(assist,new Object[]{null});
        check(!remappedSneak.isKeyDown(),"Sneak leaked behind GUI");
        bridge.getMethod("onDisable").invoke(assist);minecraft.currentScreen=null;physicalKeys.clear();
        System.out.println("Production Sprint/Bridge input fixture: forward/strafe, pause/recovery, collision, GUI, remapped keys, held Sneak, release/disable restoration PASS (inert game objects, simulated hardware input)");
    }
    static void verifyLegacyConfig() throws Exception {
        var parser=new com.google.gson.JsonParser();
        for(String type:new String[]{"movement.Sprint","player.SafeWalk"}) {
            Object module=loader.loadClass("dev.veyra.client.module.modules."+type).getConstructor().newInstance();
            var legacy=parser.parse("{\"enabled\":false,\"keycode\":47,\"bindMode\":\"HOLD\",\"favorite\":true,\"settings\":{\"Omni\":{\"value\":true},\"Sneak on jump\":{\"value\":true},\"Avoid double-sneaking\":{\"value\":true},\"Holding blocks\":{\"type\":\"tick\",\"value\":false},\"Sneak key pressed\":{\"type\":\"tick\",\"value\":true}}}").getAsJsonObject();
            moduleClass.getMethod("applyConfigFromJson",com.google.gson.JsonObject.class).invoke(module,legacy);
            var saved=(com.google.gson.JsonObject)moduleClass.getMethod("getConfigAsJson").invoke(module);
            check(saved.get("keycode").getAsInt()==47 && saved.get("favorite").getAsBoolean(),"Legacy bind/favorite lost");
            check(saved.get("bindMode").getAsString().equals("HOLD"),"Legacy Hold compatibility lost");
            var settings=saved.getAsJsonObject("settings");
            check(!settings.has("Omni") && !settings.has("Sneak on jump") && !settings.has("Avoid double-sneaking"),"Removed settings serialized");
            if(type.endsWith("SafeWalk")) {
                check(!settings.getAsJsonObject("Holding blocks").get("value").getAsBoolean(),"Unrelated setting reset");
                check(settings.getAsJsonObject("Sneak key pressed").get("value").getAsBoolean(),"Sneak condition lost");
            }
            moduleClass.getMethod("applyConfigFromJson",com.google.gson.JsonObject.class).invoke(module,saved);
            check(saved.equals(moduleClass.getMethod("getConfigAsJson").invoke(module)),"Config round-trip differs");
        }
        System.out.println("Production Module configuration: unknown removed settings, retained values, legacy Hold, round-trip PASS");
    }
    static void verifyReservedInputs() throws Exception {
        Class<?> veyra=loader.loadClass("dev.veyra.client.main.Veyra");
        Object manager=veyra.getField("moduleManager").get(null);
        List modules=(List)manager.getClass().getMethod("getModules").invoke(manager);
        Object gui=loader.loadClass("dev.veyra.client.module.modules.client.ClickGuiModule").getConstructor().newInstance();
        modules.add(gui);Object module=dummy("Sprint");bind(module,47);
        for(int reserved:new int[]{28,87,61}) {
            begin(module);screenClass.getMethod("keyTyped",char.class,int.class).invoke(screen,'\0',reserved);
            check(key(module)==47 && get(screen,"bindingModule")==module,"Reserved key captured: "+reserved);
        }
        begin(gui);screenClass.getMethod("keyTyped",char.class,int.class).invoke(screen,'r',19);
        check(key(gui)==19,"GUI shortcut could not be remapped");
        bind(gui,-97);begin(module);screenClass.getMethod("mouseClicked",int.class,int.class,int.class).invoke(screen,620,430,3);
        check(key(module)==47 && get(screen,"bindingModule")==module,"GUI mouse shortcut captured by another module");
        modules.clear();set(screen,"bindingModule",null);
        System.out.println("Reserved keyboard/GUI mouse shortcuts and GUI shortcut remapping PASS");
    }
    static void verifyLegacyRoot(Path directory) throws Exception {
        Object manager=loader.loadClass("dev.veyra.client.main.Veyra").getField("moduleManager").get(null);
        List modules=(List)manager.getClass().getMethod("getModules").invoke(manager);
        Object sprint=loader.loadClass("dev.veyra.client.module.modules.movement.Sprint").getConstructor().newInstance();
        Object bridge=loader.loadClass("dev.veyra.client.module.modules.player.SafeWalk").getConstructor().newInstance();
        Object autoBlock=dummy("AutoBlock");modules.add(sprint);modules.add(bridge);modules.add(autoBlock);
        Files.createDirectories(directory);Path legacy=directory.resolve("legacy-removed-modules.rcfg");
        Files.writeString(legacy,"{\"creationTime\":1,\"modules\":{\"AutoBlock In\":{\"enabled\":true,\"keycode\":29},\"AutoBlockIn\":{\"enabled\":true},\"Sprint\":{\"keycode\":47,\"favorite\":true,\"settings\":{\"Omni\":{\"type\":\"tick\",\"value\":true}}},\"BridgeAssist\":{\"keycode\":45,\"settings\":{\"Sneak on jump\":{\"value\":true},\"Avoid double-sneaking\":{\"value\":false},\"Sneak key pressed\":{\"type\":\"tick\",\"value\":true}}},\"AutoBlock\":{\"keycode\":46,\"favorite\":true}}}");
        Class<?> config=loader.loadClass("dev.veyra.client.config.Config");Object file=config.getConstructor(java.io.File.class).newInstance(legacy.toFile());
        Class<?> configs=loader.loadClass("dev.veyra.client.config.ConfigManager");Object inertManager=allocate(configs);
        configs.getMethod("setConfig",config).invoke(inertManager,file);
        check(key(sprint)==47 && key(bridge)==45 && key(autoBlock)==46,"Unknown legacy module reset other module binds");
        check((boolean)moduleClass.getMethod("isFavorite").invoke(autoBlock),"Unknown module reset other favorites");
        check((boolean)get(bridge,"requireSneakKey").getClass().getMethod("isToggled").invoke(get(bridge,"requireSneakKey")),"Legacy bridge condition lost");
        modules.clear();System.out.println("Production ConfigManager: legacy AutoBlockIn/AutoBlock In ignored; Sprint/Bridge/AutoBlock values retained PASS");
    }
    static void verifyExpansion() throws Exception {
        Object module=loader.loadClass("dev.veyra.client.module.modules.player.SafeWalk").getConstructor().newInstance();
        var minecraft=net.minecraft.client.Minecraft.getMinecraft();
        minecraft.displayWidth=1280;minecraft.displayHeight=900;minecraft.gameSettings.guiScale=2;
        Field language=net.minecraft.client.Minecraft.class.getDeclaredField("mcLanguageManager");language.setAccessible(true);
        language.set(minecraft,allocate(net.minecraft.client.resources.LanguageManager.class));
        Field screenMc=net.minecraft.client.gui.GuiScreen.class.getDeclaredField("mc");screenMc.setAccessible(true);screenMc.set(screen,minecraft);
        ((List<?>)get(screen,"moduleHitboxes")).clear();((List<?>)get(screen,"actionHitboxes")).clear();
        invoke(screen,"drawModule",new Class[]{moduleClass,rectClass,int.class,int.class},module,rect(20,20,244,42),255,40);
        screenClass.getMethod("mouseClicked",int.class,int.class,int.class).invoke(screen,255,40,0);
        check(get(screen,"settingsModule")==module,"Arrow did not expand module");
        Map expansion=(Map)get(screen,"expansion");expansion.put(module,1F);
        int height=(int)invoke(screen,"animatedModuleHeight",new Class[]{moduleClass},module);
        ((List<?>)get(screen,"actionHitboxes")).clear();((List<?>)get(screen,"settingHitboxes")).clear();
        invoke(screen,"drawModule",new Class[]{moduleClass,rectClass,int.class,int.class},module,rect(20,20,244,height),255,40);
        check(((List<?>)get(screen,"actionHitboxes")).size()==1,"Expanded body retained obsolete bind/reset actions");
        check(((List<?>)get(screen,"settingHitboxes")).size()==10,"Bridge settings rows missing after cleanup");
        int content=(int)invoke(screen,"contentHeight",new Class[]{List.class},List.of(module,dummy("Sprint"),dummy("AutoBlock")));
        check(content==height+42+12+16,"Two-column content/scroll height changed");
        begin(module);screenClass.getMethod("onGuiClosed").invoke(screen);
        check(get(screen,"bindingModule")==null,"Closing GUI left capture active");
        set(screen,"settingsModule",null);expansion.clear();((List<?>)get(screen,"moduleHitboxes")).clear();
        System.out.println("Production card expansion, setting rows, two-column scroll geometry, capture cleanup on close PASS");
    }
    static void render(String destination) throws Exception {
        int width=1280,height=900;
        int framebuffer=EXTFramebufferObject.glGenFramebuffersEXT();EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,framebuffer);
        int texture=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,width,height,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,(java.nio.ByteBuffer)null);
        EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT,GL11.GL_TEXTURE_2D,texture,0);
        check(EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT)==EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT,"Incomplete framebuffer");
        GL11.glViewport(0,0,width,height);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(0,640,450,0,-1,1);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glClearColor(.025f,.035f,.055f,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        for(int column=0;column<2;column++) {
            int cardWidth=column==0?244:188,x=column==0?24:300;
            for(int row=0;row<7;row++) {
                String[] names={"Sprint","BridgeAssist","AutoBlock","AutoBlock","BridgeAssist","AutoBlock","VeryLongModuleName"};
                Object module=dummy(names[row]);
                if(row==1)bind(module,47);if(row==3)bind(module,-97);if(row==6)bind(module,157);
                if(row==4 || row==5)begin(module);else set(screen,"bindingModule",null);
                ((List<?>)get(screen,"actionHitboxes")).clear();((List<?>)get(screen,"moduleHitboxes")).clear();
                int y=24+row*55,mouseX=row==3?x+cardWidth-77:-1,mouseY=y+20;
                invoke(screen,"drawModule",new Class[]{moduleClass,rectClass,int.class,int.class},module,rect(x,y,cardWidth,42),mouseX,mouseY);
                for(Object hitbox:(List<?>)get(screen,"actionHitboxes")) {
                    Object r=get(hitbox,"row");int rx=(int)get(r,"x"),rw=(int)get(r,"width");
                    check(rx>=x && rx+rw<=x+cardWidth-70 && rw>0,"Bind exceeds card controls");
                    if(get(hitbox,"action").toString().equals("BIND"))check(rw==48 && (int)get(r,"y")==y+13 && (int)get(r,"height")==15,"Bind is not compact and aligned");
                }
            }
        }
        GL11.glFinish();var bytes=BufferUtils.createByteBuffer(width*height*4);GL11.glReadPixels(0,0,width,height,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,bytes);
        var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){int i=((height-1-y)*width+x)*4;image.setRGB(x,y,((bytes.get(i)&255)<<16)|((bytes.get(i+1)&255)<<8)|(bytes.get(i+2)&255));}
        Files.createDirectories(Path.of(destination).getParent());ImageIO.write(image,"png",Path.of(destination).toFile());
    }
}
