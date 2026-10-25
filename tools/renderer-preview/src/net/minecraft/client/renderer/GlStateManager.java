package net.minecraft.client.renderer;
import org.lwjgl.opengl.GL11;
public class GlStateManager {
 private static boolean blend, texturing;
 public static void pushMatrix(){GL11.glPushMatrix();} public static void popMatrix(){GL11.glPopMatrix();}
 public static void enableBlend(){if(!blend){GL11.glEnable(GL11.GL_BLEND);blend=true;}} public static void enableTexture2D(){if(!texturing){GL11.glEnable(GL11.GL_TEXTURE_2D);texturing=true;}}
 public static void disableBlend(){if(blend){GL11.glDisable(GL11.GL_BLEND);blend=false;}} public static void disableTexture2D(){if(texturing){GL11.glDisable(GL11.GL_TEXTURE_2D);texturing=false;}}
 public static void tryBlendFuncSeparate(int a,int b,int c,int d){GL11.glBlendFunc(a,b);}
 public static void bindTexture(int t){GL11.glBindTexture(GL11.GL_TEXTURE_2D,t);} public static void color(float r,float g,float b,float a){GL11.glColor4f(r,g,b,a);}
}
