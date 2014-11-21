package com.example.spritegame;

import org.andengine.entity.sprite.Sprite;

public class UserData {

	String mName;
	Sprite mSprite;
	
	UserData(String name, Sprite sprite) {
		mName = name;
		mSprite = sprite;
	}
	
	public String getName() {
		return mName;
	}
	
	public Sprite getSprite() {
		return mSprite;
	}
}
