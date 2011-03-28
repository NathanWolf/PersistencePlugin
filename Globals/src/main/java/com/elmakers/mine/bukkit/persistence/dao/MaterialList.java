package com.elmakers.mine.bukkit.persistence.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;

import com.elmakers.mine.bukkit.persisted.PersistClass;
import com.elmakers.mine.bukkit.persisted.PersistField;

/**
 * Implements a hashset of Materials for quick add/lookup
 * 
 * Uses MaterialData to differentiate between data variants.
 * 
 * @author NathanWolf
 * 
 */

@PersistClass(schema = "global", name = "materialList")
public class MaterialList implements Collection<MaterialData>
{
    /**
	 * 
	 */
    private static final long         serialVersionUID = 1L;

    protected HashSet<MaterialData>   dataMap          = null;

    protected String                  id               = null;

    // Need an extra list for persistence, for now
    protected ArrayList<MaterialData> materialList     = null;
    protected HashSet<Material>       materialMap      = null;

    public MaterialList()
    {

    }

    public MaterialList(String id)
    {
        this.id = id;
    }

    public void add(Block block)
    {
        add(new MaterialData(block));
    }

    public boolean add(Material material)
    {
        return add(new MaterialData(material));
    }

    public boolean add(MaterialData newMaterial)
    {
        if (newMaterial == null)
        {
            return false;
        }

        if (materialMap == null || dataMap == null || materialList == null)
        {
            materialMap = new HashSet<Material>();
            dataMap = new HashSet<MaterialData>();
            materialList = new ArrayList<MaterialData>();
        }

        materialList.add(newMaterial);
        materialMap.add(newMaterial.getType());
        dataMap.add(newMaterial);

        return true;
    }

    public boolean addAll(Collection<? extends MaterialData> materials)
    {
        if (materials == null)
        {
            return false;
        }
        for (MaterialData material : materials)
        {
            add(material);
        }

        return true;
    }

    public void clear()
    {
        if (dataMap != null)
        {
            dataMap.clear();
        }

        if (materialMap != null)
        {
            materialMap.clear();
        }

        if (materialList != null)
        {
            materialList.clear();
        }
    }

    public boolean contains(Material material)
    {
        if (materialMap != null)
        {
            return materialMap.contains(material);
        }
        return false;
    }

    public boolean contains(MaterialData material)
    {
        if (materialMap != null)
        {
            return dataMap.contains(material);
        }
        return false;
    }

    public boolean contains(Object o)
    {
        if (dataMap == null)
        {
            return false;
        }
        return dataMap.contains(o);
    }

    public boolean containsAll(Collection<?> other)
    {
        if (dataMap == null)
        {
            return other == null || other.size() == 0;
        }

        return dataMap.containsAll(other);
    }

    @PersistField(id = true)
    public String getId()
    {
        return id;
    }

    @PersistField(name = "materials")
    public ArrayList<MaterialData> getMaterialList()
    {
        return materialList;
    }

    public boolean isEmpty()
    {
        if (dataMap == null)
        {
            return true;
        }

        return dataMap.isEmpty();
    }

    public Iterator<MaterialData> iterator()
    {
        if (dataMap == null)
        {
            return null;
        }

        return dataMap.iterator();
    }

    public boolean remove(Material material)
    {
        if (dataMap == null || materialMap == null || materialList == null)
        {
            return true;
        }

        List<MaterialData> dataList = new ArrayList<MaterialData>();
        dataList.addAll(dataMap);
        for (MaterialData checkData : dataList)
        {
            if (checkData.getType() == material)
            {
                dataMap.remove(checkData);
            }
        }

        materialList.remove(material);
        return materialMap.remove(material);
    }

    public boolean remove(MaterialData material)
    {
        if (dataMap == null || materialMap == null || materialList == null)
        {
            return true;
        }

        materialList.remove(material.getType());
        materialMap.remove(material.getType());
        return dataMap.remove(material);
    }

    public boolean remove(Object o)
    {
        return false;
    }

    public boolean removeAll(Collection<? extends Object> materials)
    {
        if (materials == null)
        {
            return false;
        }
        for (Object material : materials)
        {
            if (material instanceof Material)
            {
                add((Material) material);
            }
            else if (material instanceof MaterialData)
            {
                add((MaterialData) material);
            }
        }

        return true;
    }

    public boolean retainAll(Collection<?> arg0)
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public void setMaterialList(ArrayList<MaterialData> materialList)
    {
        this.materialList = materialList;
    }

    public int size()
    {
        if (dataMap == null)
        {
            return 0;
        }

        return dataMap.size();
    }

    public Object[] toArray()
    {
        if (dataMap == null)
        {
            return null;
        }

        return dataMap.toArray();
    }

    public <T> T[] toArray(T[] other)
    {
        if (dataMap == null)
        {
            return null;
        }

        return dataMap.toArray(other);
    }
}
