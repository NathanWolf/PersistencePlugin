package com.elmakers.mine.bukkit.borrowed;

import java.util.Map;

/**
 * Represents a numeric permission description node
 */
public class IntegerPermissionDescriptionNode extends PermissionDescriptionNode
{
    private int maximum = Integer.MAX_VALUE;
    private int minimum = Integer.MIN_VALUE;

    public IntegerPermissionDescriptionNode(final PermissionDescriptionNode parent, Map<String, Object> map) throws PermissionDescriptionException, PermissionDescriptionNodeException
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
                return Integer.parseInt(map.get("default").toString(), 10);
            }
            catch (ClassCastException ex)
            {
                return 0;
            }
        }
        else
        {
            return 0;
        }
    }

    /**
     * Gets the maximum value that this node allows
     * 
     * @return Maximum value allowed by this node
     */
    public int getMaximum()
    {
        return maximum;
    }

    /**
     * Gets the minimum value that this node allows
     * 
     * @return Minimum value allowed by this node
     */
    public int getMinimum()
    {
        return minimum;
    }

    @Override
    public boolean isValid(final Object value)
    {
        if (value != null && value instanceof Integer)
        {
            int real = (Integer) value;

            if (real >= minimum && real <= maximum)
            {
                return true;
            }
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
            throw new IllegalArgumentException("Default value must be a valid " + "integer between " + minimum + " and " + maximum);
        }
    }

    /**
     * Sets the maximum value that this node allows
     * 
     * @param value
     *            New maximum value allowed by this node
     */
    public void setMaximum(final int value)
    {
        maximum = value;
    }

    /**
     * Sets the minimum value that this node allows
     * 
     * @param value
     *            New minimum value allowed by this node
     */
    public void setMinimum(final int value)
    {
        minimum = value;
    }
}
