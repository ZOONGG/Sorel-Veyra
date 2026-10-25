import dev.veyra.client.clickgui.*; import dev.veyra.client.clickgui.font.*;
import dev.veyra.client.utils.render.RenderUtils; import org.lwjgl.opengl.*; import org.lwjgl.BufferUtils;
import java.awt.image.BufferedImage; import javax.imageio.ImageIO; import java.io.File;
public class UiPreview {
 static int scale=2, pixelWidth=1280, pixelHeight=900;
 public static void main(String[] args)throws Exception{
 scale=args.length>1?Integer.parseInt(args[1]):2;pixelWidth=640*scale;pixelHeight=450*scale;
 Display.setDisplayMode(new DisplayMode(64,64));Display.setTitle("Veyra renderer verification");Display.setLocation(-12000,-12000);Display.create();
 try{
 int framebuffer=EXTFramebufferObject.glGenFramebuffersEXT();
 EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,framebuffer);
 int target=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,target);
 GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,pixelWidth,pixelHeight,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,(java.nio.ByteBuffer)null);
 EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT,GL11.GL_TEXTURE_2D,target,0);
 if(EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT)!=EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT)throw new AssertionError("Incomplete offscreen target");
 GL11.glViewport(0,0,pixelWidth,pixelHeight);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(0,640,450,0,-1,1);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();
 GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glClearColor(.025f,.035f,.055f,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
 verifyStateIsolation();
 verifyStaleState();
 verifyTextureUnits();
 verifyShaderState();
 verifyTextureMatrix();
 GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
 dev.veyra.client.notification.NotificationToast.draw(new dev.veyra.client.notification.NotificationQueue.Entry("Timer",true,0),10,900);
 dev.veyra.client.notification.NotificationToast.draw(new dev.veyra.client.notification.NotificationQueue.Entry("Timer",false,0),46,900);
 writeFrame(new File(args[0]).getParent()+"/notifications-scale"+scale+".png");
 GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
 int accent=0xFF8B5CF6;
 UiDraw.surface(24,24,616,426,9,UiTokens.Color.WINDOW,UiTokens.Color.BORDER,2);
 VeyraFonts.semibold().drawPlainTracked("veyra",40,34,UiTokens.Color.TEXT_PRIMARY,17f,-.15f);
 UiIcons.settings(570,50,UiTokens.Color.TEXT_SECONDARY);
 UiIcons.star(530,50,5.6f,UiTokens.Color.TEXT_SECONDARY,false);
 UiIcons.star(495,50,5.6f,UiTokens.Color.WARNING,true);
 VeyraFonts.regular().drawPlain("OpenGL primitive verification / GUI scale " + scale + "",40,61,UiTokens.Color.TEXT_MUTED,8.8f);
 for(int row=0;row<4;row++){
 int y=92+row*48;UiDraw.surface(40,y,308,y+40,4,UiTokens.Color.SURFACE,UiTokens.Color.BORDER_SOFT,0);
 VeyraFonts.semibold().drawPlain(new String[]{"AimAssist","Velocity","Настройки интерфейса","Smooth motion 0123456789"}[row],50,y+6,UiTokens.Color.TEXT_PRIMARY,10);
 VeyraFonts.regular().drawPlain("Consistent spacing / ровный текст",50,y+23,UiTokens.Color.TEXT_MUTED,8.4f);
 UiDraw.toggle(270,y+14,294,y+26,row/3f,false,false,accent);
 }
 VeyraFonts.regular().drawPlain("Speed / Скорость",334,96,UiTokens.Color.TEXT_SECONDARY,8.8f);
 VeyraFonts.semibold().drawPlain("1.25",561,96,UiTokens.Color.TEXT_PRIMARY,8.4f);
 UiDraw.slider(334,118,586,121,.43f,-1,accent);
 VeyraFonts.regular().drawPlain("Range",334,148,UiTokens.Color.TEXT_SECONDARY,8.8f);
 UiDraw.slider(334,170,586,173,.2f,.75f,accent);
 RenderUtils.gradient(334,206,586,286,0xFFFFFFFF,accent,accent,0xFFFFFFFF);
 RenderUtils.gradient(334,206,586,286,0,0,0xFF000000,0xFF000000);
 RenderUtils.drawRoundedOutline(333,205,587,287,4,.7f,UiTokens.Color.BORDER);
 UiDraw.button(40,312,122,338,false,false,accent);UiDraw.button(132,312,214,338,true,false,accent);UiDraw.button(224,312,306,338,false,true,accent);
 VeyraFonts.semibold().drawPlain("Default",62,321,UiTokens.Color.TEXT_SECONDARY,8);
 VeyraFonts.semibold().drawPlain("Hover",159,321,UiTokens.Color.TEXT_PRIMARY,8);
 VeyraFonts.semibold().drawPlain("Active",249,321,accent,8);
 for(int i=0;i<5;i++){float h=7+i;VeyraFonts.regular().drawPlain("Inter Aa Bb 0123 — Привет",40,355+i*11,UiTokens.Color.TEXT_PRIMARY,h);}
 GL11.glFinish();int error=GL11.glGetError();if(error!=0)throw new IllegalStateException("GL error "+error);
 var pixels=BufferUtils.createByteBuffer(pixelWidth*pixelHeight*4);GL11.glReadPixels(0,0,pixelWidth,pixelHeight,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
 var image=new BufferedImage(pixelWidth,pixelHeight,BufferedImage.TYPE_INT_RGB);for(int y=0;y<pixelHeight;y++)for(int x=0;x<pixelWidth;x++){int i=((pixelHeight-1-y)*pixelWidth+x)*4;image.setRGB(x,y,((pixels.get(i)&255)<<16)|((pixels.get(i+1)&255)<<8)|(pixels.get(i+2)&255));}
 ImageIO.write(image,"png",new File(args[0]));System.out.println("OpenGL primitives rendered; GL error=0");
 }finally{Display.destroy();}}
static void verifyStateIsolation(){
 GL11.glEnable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_FOG);GL11.glEnable(GL11.GL_ALPHA_TEST);
 GL11.glShadeModel(GL11.GL_FLAT);
 RenderUtils.gradient(0,0,20,20,0xFFFF0000,0xFFFF0000,0xFFFF0000,0xFFFF0000);
 if(!GL11.glIsEnabled(GL11.GL_LIGHTING)||!GL11.glIsEnabled(GL11.GL_FOG)||!GL11.glIsEnabled(GL11.GL_ALPHA_TEST)
  ||GL11.glGetInteger(GL11.GL_SHADE_MODEL)!=GL11.GL_FLAT)throw new AssertionError("Gradient leaked render state");
 var pixel=BufferUtils.createByteBuffer(4);GL11.glReadPixels(5*scale,pixelHeight-5*scale,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixel);
 if((pixel.get(0)&255)<250||(pixel.get(1)&255)>2)throw new AssertionError("Inherited state corrupted gradient color");
 RenderUtils.drawRoundedRect(0,0,20,20,4,0xFF00FF00);
 RenderUtils.drawRoundedOutline(0,0,20,20,4,1,0xFFFFFFFF);
 if(!GL11.glIsEnabled(GL11.GL_LIGHTING)||!GL11.glIsEnabled(GL11.GL_FOG)||!GL11.glIsEnabled(GL11.GL_ALPHA_TEST)
  ||GL11.glGetInteger(GL11.GL_SHADE_MODEL)!=GL11.GL_FLAT)throw new AssertionError("Contour leaked render state");
 GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL11.GL_TEXTURE_ENV_MODE,GL11.GL_REPLACE);
 VeyraFonts.regular().drawPlain("State isolation",0,24,0xFFBBAAFF,10);
 if(!GL11.glIsEnabled(GL11.GL_LIGHTING)||!GL11.glIsEnabled(GL11.GL_FOG)||!GL11.glIsEnabled(GL11.GL_ALPHA_TEST)
  ||GL11.glGetTexEnvi(GL11.GL_TEXTURE_ENV,GL11.GL_TEXTURE_ENV_MODE)!=GL11.GL_REPLACE)throw new AssertionError("Font leaked render state");
 GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL11.GL_TEXTURE_ENV_MODE,GL11.GL_MODULATE);
 GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_FOG);GL11.glDisable(GL11.GL_ALPHA_TEST);
 System.out.println("Inherited lighting, fog, alpha test, flat shading, and texture environment: PASS");
}
static void verifyStaleState(){
 net.minecraft.client.renderer.GlStateManager.enableBlend();
 net.minecraft.client.renderer.GlStateManager.enableTexture2D();
 GL11.glDisable(GL11.GL_BLEND);GL11.glDisable(GL11.GL_TEXTURE_2D);
 GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
 VeyraFonts.regular().drawPlain("MMMM",10,10,0xFFFFFFFF,12);
 int w=60*scale,h=20*scale;
 var samples=BufferUtils.createByteBuffer(w*h*4);
 GL11.glReadPixels(10*scale,pixelHeight-30*scale,w,h,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,samples);
 var padding=BufferUtils.createByteBuffer(4);
 GL11.glReadPixels(10*scale,pixelHeight-10*scale-1,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,padding);
 if((padding.get(0)&255)>4)throw new AssertionError("Transparent glyph padding became a solid rectangle");
 int covered=0;for(int i=0;i<w*h;i++)if((samples.get(i*4)&255)>100)covered++;
 if(covered<10*scale || covered>w*h/3)throw new AssertionError("Font became missing text or solid glyph bars under stale GL cache: "+covered);
 if(GL11.glIsEnabled(GL11.GL_BLEND)||GL11.glIsEnabled(GL11.GL_TEXTURE_2D))throw new AssertionError("Font did not restore inherited state");
 GL11.glClearColor(.025f,.035f,.055f,1);
 System.out.println("Stale Minecraft blend/texture cache: readable coverage and restored GL state PASS");
}
static void verifyTextureUnits(){
 GL13.glActiveTexture(GL13.GL_TEXTURE1);
 int texture=GL11.glGenTextures(); GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
 var black=BufferUtils.createByteBuffer(4); black.putInt(-16777216).flip();
 GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,1,1,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,black);
 GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
 GL11.glEnable(GL11.GL_TEXTURE_2D);
 GL14.glBlendEquation(GL14.GL_FUNC_REVERSE_SUBTRACT);
 GL11.glClearColor(0,0,0,1); GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
 VeyraFonts.regular().drawPlain("MMMM",10,10,0xFFFFFFFF,12);
 var pixels=BufferUtils.createByteBuffer(60*scale*20*scale*4);
 GL11.glReadPixels(10*scale,pixelHeight-30*scale,60*scale,20*scale,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
 int lit=0; for(int i=0;i<pixels.capacity();i+=4)if((pixels.get(i)&255)>100)lit++;
 if(lit<10*scale)throw new AssertionError("Secondary texture unit or blend equation corrupted font");
 if(GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)!=GL13.GL_TEXTURE1 || !GL11.glIsEnabled(GL11.GL_TEXTURE_2D)
   || GL11.glGetInteger(GL14.GL_BLEND_EQUATION)!=GL14.GL_FUNC_REVERSE_SUBTRACT)
   throw new AssertionError("Font leaked multitexture state");
 GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDeleteTextures(texture);
 GL13.glActiveTexture(GL13.GL_TEXTURE0); GL14.glBlendEquation(GL14.GL_FUNC_ADD);
 GL11.glClearColor(.025f,.035f,.055f,1);
 System.out.println("Secondary texture unit and inherited blend equation: PASS");
}


static void verifyShaderState(){
 int vertex=GL20.glCreateShader(GL20.GL_VERTEX_SHADER), fragment=GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
 GL20.glShaderSource(vertex,"#version 120\nvoid main(){ gl_Position=ftransform(); }");
 GL20.glShaderSource(fragment,"#version 120\nvoid main(){ gl_FragColor=vec4(0.0,0.0,0.0,1.0); }");
 GL20.glCompileShader(vertex);GL20.glCompileShader(fragment);
 int program=GL20.glCreateProgram();GL20.glAttachShader(program,vertex);GL20.glAttachShader(program,fragment);GL20.glLinkProgram(program);
 if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new AssertionError(GL20.glGetProgramInfoLog(program,2048));
 GL20.glUseProgram(program);GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
 VeyraFonts.regular().drawPlain("MMMM",10,10,0xFFFFFFFF,12);
 var pixels=BufferUtils.createByteBuffer(60*scale*20*scale*4);
 GL11.glReadPixels(10*scale,pixelHeight-30*scale,60*scale,20*scale,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
 int lit=0;for(int i=0;i<pixels.capacity();i+=4)if((pixels.get(i)&255)>100)lit++;
 if(lit<10*scale)throw new AssertionError("Inherited HUD shader corrupted glyph coverage");
 if(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)!=program)throw new AssertionError("Font did not restore HUD shader");
 GL20.glUseProgram(0);GL20.glDeleteProgram(program);GL20.glDeleteShader(vertex);GL20.glDeleteShader(fragment);
 GL11.glClearColor(.025f,.035f,.055f,1);
 System.out.println("Inherited HUD shader: glyph coverage and program restoration PASS");
}

static void writeFrame(String path)throws Exception {
 var pixels=BufferUtils.createByteBuffer(pixelWidth*pixelHeight*4);
 GL11.glReadPixels(0,0,pixelWidth,pixelHeight,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
 BufferedImage result=new BufferedImage(pixelWidth,pixelHeight,BufferedImage.TYPE_INT_ARGB);
 for(int y=0;y<pixelHeight;y++)for(int x=0;x<pixelWidth;x++){
 int i=((pixelHeight-y-1)*pixelWidth+x)*4;
 result.setRGB(x,y,((pixels.get(i+3)&255)<<24)|((pixels.get(i)&255)<<16)|((pixels.get(i+1)&255)<<8)|(pixels.get(i+2)&255));}
 ImageIO.write(result,"png",new File(path));
}

static void verifyTextureMatrix(){
 GL13.glActiveTexture(GL13.GL_TEXTURE0);
 GL11.glMatrixMode(GL11.GL_TEXTURE);GL11.glLoadIdentity();GL11.glScalef(0,0,1);
 GL11.glMatrixMode(GL11.GL_MODELVIEW);
 GL11.glTexEnvf(GL14.GL_TEXTURE_FILTER_CONTROL,GL14.GL_TEXTURE_LOD_BIAS,3);
 GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
 VeyraFonts.regular().drawPlain("Timer",10,10,0xFFFFFFFF,13);
 var pixels=BufferUtils.createByteBuffer(60*scale*20*scale*4);
 GL11.glReadPixels(10*scale,pixelHeight-30*scale,60*scale,20*scale,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
 int lit=0;for(int i=0;i<pixels.capacity();i+=4)if((pixels.get(i)&255)>100)lit++;
 if(lit<10*scale)throw new AssertionError("Inherited texture matrix or LOD bias corrupted Timer");
 var matrix=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_TEXTURE_MATRIX,matrix);
 if(matrix.get(0)!=0 || matrix.get(5)!=0)throw new AssertionError("Font leaked texture matrix");
 if(GL11.glGetTexEnvf(GL14.GL_TEXTURE_FILTER_CONTROL,GL14.GL_TEXTURE_LOD_BIAS)!=3)throw new AssertionError("Font leaked LOD bias");
 GL11.glMatrixMode(GL11.GL_TEXTURE);GL11.glLoadIdentity();GL11.glMatrixMode(GL11.GL_MODELVIEW);
 GL11.glTexEnvf(GL14.GL_TEXTURE_FILTER_CONTROL,GL14.GL_TEXTURE_LOD_BIAS,0);
 GL11.glClearColor(.025f,.035f,.055f,1);
 System.out.println("Inherited texture matrix and LOD bias: Timer coverage and state restoration PASS");
}
}