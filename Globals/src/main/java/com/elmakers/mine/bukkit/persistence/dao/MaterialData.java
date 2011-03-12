package com.elmakers.mine.bukkit.persistence.dao;

import org.bukkit.Material;
import org.bukkit.block.Block;

import com.elmakers.mine.bukkit.persistence.annotation.PersistClass;
import com.elmakers.mine.bukkit.persistence.annotation.PersistField;
import com.elmakers.mine.craftbukkit.persistence.persisted.Persisted;

/**
 * Encapsulates a Material class and its data value.
 * 
 * @author NathanWolf
 *
 */

@PersistClass(schema="global", name="material")
public class MaterialData extends Persisted
{
	public MaterialData()
	{
		
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
	
	public MaterialData(Block block)
	{
		this.type = block.getType();
		this.data = block.getData();
	}
	
	/**
     * Returns a hash code for this Location- does not include orientation.
     *
     * @return hash code
     */
    @Override
    @PersistField(id=true, name="id", readonly=true)
    public int hashCode()
    {
    	int materialHash = type.hashCode();
    	return (materialHash << 8) | data;
    }
    
    @PersistField
    public Material getType()
	{
		return type;
	}
    
	public void setType(Material type)
	{
		this.type = type;
	}
	
	@PersistField
	public byte getData()
	{
		return data;
	}
	
	public void setData(byte data)
	{
		this.data = data;
	}
	
	protected Material	type;
	protected byte		data;
}
