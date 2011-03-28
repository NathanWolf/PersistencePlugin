package com.elmakers.mine.bukkit.persistence.dao;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import com.elmakers.mine.bukkit.persisted.PersistClass;
import com.elmakers.mine.bukkit.persisted.PersistField;
import com.elmakers.mine.bukkit.persisted.Persisted;

/**
 * Encapsulates a Material class and its data value.
 * 
 * @author NathanWolf
 * 
 */

@PersistClass(schema = "global", name = "material")
public class MaterialData extends Persisted
{
    protected short    data;
    protected Material type;

    public MaterialData()
    {

    }

    public MaterialData(Block block)
    {
        this.type = block.getType();
        this.data = block.getData();
    }

    public MaterialData(ItemStack stack)
    {
        if (stack != null)
        {
            this.data = (byte) stack.getDurability();
            this.type = stack.getType();
        }
    }

    public MaterialData(Material mat)
    {
        this.type = mat;
        this.data = 0;
    }

    public MaterialData(Material mat, byte data)
    {
        this.type = mat;
        this.data = data;
    }

    @PersistField
    public byte getData()
    {
        return (byte) data;
    }

    public short getDurability()
    {
        return data;
    }

    @PersistField
    public Material getType()
    {
        return type;
    }

    /**
     * Returns a hash code for this Location- does not include orientation.
     * 
     * @return hash code
     */
    @Override
    @PersistField(id = true, name = "id", readonly = true)
    public int hashCode()
    {
        int materialHash = type.hashCode();
        return materialHash << 8 | data & 0xFF;
    }

    public void setData(byte data)
    {
        this.data = data;
    }

    public void setDurability(short durability)
    {
        this.data = durability;
    }

    public void setType(Material type)
    {
        this.type = type;
    }
}
