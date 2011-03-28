package com.elmakers.mine.bukkit.persistence.dao;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

import com.elmakers.mine.bukkit.persisted.PersistClass;
import com.elmakers.mine.bukkit.persisted.PersistField;
import com.elmakers.mine.bukkit.persisted.Persisted;

@PersistClass(schema="global", name="parameter")
public class ParameterData extends Persisted
{
    protected String        id;
    protected String        value;
    protected ParameterType type;
    protected ParameterType listType;
    
    @PersistField(id=true)
    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    @PersistField
    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    @PersistField
    public ParameterType getType()
    {
        return type;
    }

    public void setType(ParameterType type)
    {
        this.type = type;
    }

    @PersistField
    public ParameterType getListType()
    {
        return listType;
    }

    public void setListType(ParameterType listType)
    {
        this.listType = listType;
    }

    public ParameterData()
    {
        
    }
    
    public boolean isFlag(String flagName)
    {
        if (type == ParameterType.BOOLEAN && Boolean.parseBoolean(value))
        {
            return true;
        }
        
        return false;
    }
    
    public ParameterData(List<? extends Object> dataList, Class<? extends Object> typeOf)
    {
        type = ParameterType.LIST;
        listType = ParameterType.UNKNOWN;
        
        List<String> stringList = new ArrayList<String>();
        int csvLength = 0;
        
        if (typeOf.equals(Material.class))
        {            
            type = ParameterType.MATERIAL;   
            for (Object dataValue : dataList)
            {
                Material mat = (Material)dataValue;
                String stringValue = Integer.toString(mat.getId());
                stringList.add(stringValue);
                csvLength += stringValue.length() + 1;
            }
        }
        else if (typeOf.equals(MaterialData.class))
        {
            type = ParameterType.MATERIAL;
            for (Object dataValue : dataList)
            {
                MaterialData mat = (MaterialData)dataValue;
                String stringValue = Integer.toString(mat.getType().getId());
                stringList.add(stringValue);
                csvLength += stringValue.length() + 1;
            }
        }
        else if (typeOf.equals(String.class))
        {
            type = ParameterType.STRING;
            for (Object dataValue : dataList)
            {
                String stringValue = (String)dataValue;
                stringList.add(stringValue);
                csvLength += stringValue.length() + 1;
            }
        }
        else if (typeOf.equals(Double.class) || typeOf.equals(double.class))
        {
            type = ParameterType.DOUBLE;
            for (Object dataValue : dataList)
            {
                String stringValue = Double.toString((double)(Double)dataValue);
                stringList.add(stringValue);
                csvLength += stringValue.length() + 1;
            }
        }
        else if (typeOf.equals(Integer.class) || typeOf.equals(int.class))
        {
            type = ParameterType.INTEGER;
            for (Object dataValue : dataList)
            {
                String stringValue = Integer.toString((int)(Integer)dataValue);
                stringList.add(stringValue);
                csvLength += stringValue.length() + 1;
            }
        }
        else if (typeOf.equals(Boolean.class) || typeOf.equals(boolean.class))
        {
            type = ParameterType.BOOLEAN;
            for (Object dataValue : dataList)
            {
                String stringValue = Boolean.toString((boolean)(Boolean)dataValue);
                stringList.add(stringValue);
                csvLength += stringValue.length() + 1;
            }
        }
        
        boolean first = true;
        StringBuffer sb   = new StringBuffer(csvLength);
        for (String s : stringList)
        {
            if (!first)
            {
                sb.append(",");
            }
            sb.append(s);
        }
        value = sb.toString();
    }
    
    public ParameterData(Object dataValue)
    {
        Class<?> typeOf = dataValue.getClass();
        listType = ParameterType.UNKNOWN;
        type = ParameterType.UNKNOWN;
        
        if (typeOf.equals(Material.class))
        {
            Material mat = (Material)dataValue;
            type = ParameterType.MATERIAL;
            value = Integer.toString(mat.getId());
        }
        else if (typeOf.equals(MaterialData.class))
        {
            MaterialData mat = (MaterialData)dataValue;
            type = ParameterType.MATERIAL;
            value = Integer.toString(mat.getType().getId());
        }
        else if (typeOf.equals(String.class))
        {
            type = ParameterType.STRING;
            value = (String)dataValue;
        }
        else if (typeOf.equals(Double.class) || typeOf.equals(double.class))
        {
            type = ParameterType.DOUBLE;
            value = Double.toString((double)(Double)dataValue);
        }
        else if (typeOf.equals(Integer.class) || typeOf.equals(int.class))
        {
            type = ParameterType.INTEGER;
            value = Integer.toString((int)(Integer)dataValue);
        } 
        else if (typeOf.equals(Boolean.class) || typeOf.equals(boolean.class))
        {
            type = ParameterType.INTEGER;
            value = Boolean.toString((boolean)(Boolean)dataValue);
        } 
        else if (List.class.isAssignableFrom(typeOf))
        {
            type = ParameterType.LIST;
            value = "";
        }
    }
}
