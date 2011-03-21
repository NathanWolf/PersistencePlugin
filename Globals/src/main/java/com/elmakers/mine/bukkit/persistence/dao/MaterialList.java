package com.elmakers.mine.bukkit.persistence.dao;

import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * Implements a hashset of Materials for quick add/lookup
 * 
 * Uses MaterialData to differentiate between data variants.
 * 
 * @author NathanWolf
 * 
 */
public class MaterialList extends HashSet<MaterialData>
{
    /**
	 * 
	 */
    private static final long   serialVersionUID = 1L;

    protected HashSet<Material> materialMap      = null;

    public boolean add(Material material)
    {
        return add(new MaterialData(material));
    }

    @Override
    public boolean add(MaterialData newMaterial)
    {
        if (newMaterial == null)
        {
            return false;
        }

        if (materialMap == null)
        {
            materialMap = new HashSet<Material>();
        }
        materialMap.add(newMaterial.getType());
        return super.add(newMaterial);
    }

    public void addFromBlock(Block block)
    {
        add(new MaterialData(block));
    }

    public boolean contains(Material material)
    {
        if (materialMap != null)
        {
            return materialMap.contains(material);
        }
        return contains(new MaterialData(material));
    }
}
