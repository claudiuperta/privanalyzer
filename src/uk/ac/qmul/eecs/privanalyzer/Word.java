package uk.ac.qmul.eecs.privanalyzer;

import android.graphics.Color;


/*
 * Tag class:
 * For now tags are just cubes. Later they will be replaced by real texts!
 */
public class Word implements Comparable<Word> {
    
    private String text, url;
    private int popularity;  //this is the importance/popularity of the Tag 
    private int textSize;
    private float locX, locY, locZ; //the center of the 3D Tag
    private float loc2DX, loc2DY;
    private float scale;
    private float mX, mY;
    private int mColor = Color.RED;
    private float colorR, colorG, colorB, alpha;
    private static final int DEFAULT_POPULARITY = 1;
       
    public Word(String text, int popularity) {
        this(text, 0f, 0f, 0f, 1.0f, popularity);
    }
    
    public Word(String text,float locX, float locY, float locZ) {
        this(text, locX, locY, locZ, 1.0f, DEFAULT_POPULARITY);
    }
    
    public Word(String text,float locX, float locY, float locZ, float scale) {
        this(text, locX, locY, locZ, scale, DEFAULT_POPULARITY);
    }
    
    public Word(String text,float locX, float locY, float locZ, float scale, int popularity) {
        this.text = text;
        this.locX = locX;
        this.locY = locY;
        this.locZ = locZ;
        this.mX = 0;
        this.mY=0;

        this.loc2DX = 0;
        this.loc2DY=0;
        
        this.colorR= 0.5f;
        this.colorG= 0.5f;
        this.colorB= 0.5f;
        this.alpha = 1.0f;
        
        this.scale = scale;
        this.popularity= popularity;        
    }   
    
    @Override
    public int compareTo(Word another) {
        return (int)(another.popularity - popularity);
    }
    
    public float getLocX() {
        return locX;
    }
    
    public void setX(float x) {
        this.mX = x;
    }
   
    public float getX() {
        return mX;
    }

    public void setY(float y) {
        this.mY = y;
    }
   
    public float getY() {
        return mY;
    }
    
    public void setLocX(float locX) {
        this.locX = locX;
    }
    
    public float getLocY() {
        return locY;
    }
    
    public void setLocY(float locY) {
        this.locY = locY;
    }
    
    public float getLocZ() {
        return locZ;
    }
    
    public void setLocZ(float locZ) {
        this.locZ = locZ;
    }
    
    public float getScale() {
        return scale;
    }
    
    public void setScale(float scale) {
        this.scale = scale;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public float getColorR() {
        return colorR;
    }
    
    public void setColorR(float colorR) {
        this.colorR = colorR;
    }
    
    public float getColorG() {
        return colorG;
    }
    
    public void setColorG(float colorG) {
        this.colorG = colorG;
    }
    
    public float getColorB() {
        return colorB;
    }
    
    public void setColorB(float colorB) {
        this.colorB = colorB;
    }
    
    public float getAlpha() {
        return alpha;
    }
    
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }
    
    public int getPopularity() {
        return popularity;
    }
    
    public void setPopularity(int popularity) {
        this.popularity = popularity;
    }
    
    public int getTextSize() {
        return textSize;
    }
    
    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }
    
    public float getLoc2DX() {
        return loc2DX;
    }
    
    public void setLoc2DX(float loc2dx) {
        loc2DX = loc2dx;
    }
    
    public float getLoc2DY() {
        return loc2DY;
    }
    
    public void setLoc2DY(float loc2dy) {
        loc2DY = loc2dy;
    }
    
    public void setColor(int color) {
        mColor = color;
    }
    
    public int getColor() {
        return mColor;
    }
}