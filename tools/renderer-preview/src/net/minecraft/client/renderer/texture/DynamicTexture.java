package net.minecraft.client.renderer.texture;
import org.lwjgl.opengl.GL11; import org.lwjgl.BufferUtils; import java.awt.image.BufferedImage;
public class DynamicTexture { private int id;
 public DynamicTexture(BufferedImage image){id=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,id);
 var bytes=BufferUtils.createByteBuffer(image.getWidth()*image.getHeight()*4);
 for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int c=image.getRGB(x,y);bytes.put((byte)(c>>16)).put((byte)(c>>8)).put((byte)c).put((byte)(c>>24));}bytes.flip();
 GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,image.getWidth(),image.getHeight(),0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,bytes);}
 public int getGlTextureId(){return id;} public void setBlurMipmap(boolean blur,boolean mip){GL11.glBindTexture(GL11.GL_TEXTURE_2D,id);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,mip?GL11.GL_LINEAR_MIPMAP_LINEAR:blur?GL11.GL_LINEAR:GL11.GL_NEAREST);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,blur?GL11.GL_LINEAR:GL11.GL_NEAREST);}
}