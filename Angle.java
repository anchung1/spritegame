package com.example.spritegame;

public class Angle {
	
	Angle() {
		
	}
	
	public double normalizeAngle(double angle) {
		if (angle<0) {
			return (360 - Math.abs(angle));
		}
		return angle;
	}
	
	public boolean IsClockwise(double refAngle, double newAngle) {
		
		refAngle = normalizeAngle(refAngle);
		newAngle = normalizeAngle(newAngle);
		
		if (newAngle < refAngle) {
			newAngle = 360 + newAngle;
		}
		
		return ((newAngle-refAngle<180 ));
		
	}
}
