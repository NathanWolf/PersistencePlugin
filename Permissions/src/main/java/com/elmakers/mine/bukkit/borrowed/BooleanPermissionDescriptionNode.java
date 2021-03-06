package com.elmakers.mine.bukkit.borrowed;

import java.util.Map;

/**
 * Represents a boolean permission description node, for simple true/false
 * access
 */
public class BooleanPermissionDescriptionNode extends PermissionDescriptionNode
{
    public BooleanPermissionDescriptionNode(final PermissionDescriptionNode parent, Map<String, Object> map) throws PermissionDescriptionException, PermissionDescriptionNodeException
    {
        super(parent, map);
    }

    @Override
    public Object getDefault()
    {
        if (map.containsKey("default"))
        {
            try
            {
                // TODO: Better type checking/conversion (see DataType)
                return Boolean.parseBoolean(map.get("default").toString());
            }
            catch (ClassCastException ex)
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean isValid(final Object value)
    {
        if (value != null && value instanceof Boolean)
        {
            return true;
        }

        return false;
    }

    @Override
    public void setDefault(final Object value)
    {
        if (isValid(value))
        {
            this.map.put("default", value);
        }
        else
        {
            throw new IllegalArgumentException("Default value must be a boolean");
        }
    }
}
