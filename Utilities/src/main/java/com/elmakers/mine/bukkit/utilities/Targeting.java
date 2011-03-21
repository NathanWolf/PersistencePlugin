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
    private final boolean                    allowMaxRange          = false;
    private int                              lastX, lastY, lastZ;
    private double                           length, hLength;
    private Location                         playerLocation;

    private final int                        range                  = 200;
    private final boolean                    reverseTargeting       = false;
    private final double                     step                   = 0.2;
    private final int                        targetHeightRequired   = 1;
    private boolean                          targetingComplete;
    private final HashMap<Material, Boolean> targetThroughMaterials = new HashMap<Material, Boolean>();
    private int                              targetX, targetY, targetZ;
    private final double                     viewHeight             = 1.65;
    private double                           xOffset, yOffset, zOffset;
    private double                           xRotation, yRotation;

}
