package com.elmakers.mine.bukkit.utilities;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;

/**
 * An entity and block targeting system
 * 
 * @author NathanWolf
 */
public class Targeting
{
	private boolean								allowMaxRange			= false;
	private int									range					= 200;
	private double								viewHeight				= 1.65;
	private double								step					= 0.2;

	private boolean								targetingComplete;
	private int									targetHeightRequired	= 1;
	private Location							playerLocation;
	private double								xRotation, yRotation;
	private double								length, hLength;
	private double								xOffset, yOffset, zOffset;
	private int									lastX, lastY, lastZ;
	private int									targetX, targetY, targetZ;
	private final HashMap<Material, Boolean>	targetThroughMaterials	= new HashMap<Material, Boolean>();
	private boolean								reverseTargeting		= false;

}
