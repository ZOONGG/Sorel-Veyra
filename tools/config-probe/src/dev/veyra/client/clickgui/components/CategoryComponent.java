package dev.veyra.client.clickgui.components;
import dev.veyra.client.main.Veyra;
public class CategoryComponent {public enum Category {Combat}public Category categoryName=Category.Combat;private int x=5,y=5;private boolean opened;
public void setX(int value){x=value;if(Veyra.clientConfig!=null)Veyra.clientConfig.saveConfig();}
public void setY(int value){y=value;if(Veyra.clientConfig!=null)Veyra.clientConfig.saveConfig();}
public void setOpened(boolean value){opened=value;if(Veyra.clientConfig!=null)Veyra.clientConfig.saveConfig();}
public int getX(){return x;}public int getY(){return y;}public boolean isOpened(){return opened;}}
