package com.elmakers.mine.bukkit.data.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import com.elmakers.mine.bukkit.data.DataField;
import com.elmakers.mine.bukkit.data.DataType;

public class SqlDataField extends DataField
{
    public static void setValue(PreparedStatement ps, int fieldIndex, Object value, DataType dataType) throws SQLException
    {
        if (value == null)
        {
            ps.setNull(fieldIndex, DataType.getSqlType(dataType));
            return;
        }

        Class<?> valueClass = value.getClass();
        switch (dataType)
        {
            case FLOAT:
                if (float.class.isAssignableFrom(valueClass) || Float.class.isAssignableFrom(valueClass))
                {
                    ps.setFloat(fieldIndex, (Float) value);
                    return;
                }
                break;
            case DOUBLE:
                if (double.class.isAssignableFrom(valueClass) || Double.class.isAssignableFrom(valueClass))
                {
                    ps.setDouble(fieldIndex, (Double) value);
                    return;
                }
                break;
            case INTEGER:
                if (int.class.isAssignableFrom(valueClass) || Integer.class.isAssignableFrom(valueClass))
                {
                    ps.setInt(fieldIndex, (Integer) value);
                    return;
                }
                break;
            case BYTE:
                if (byte.class.isAssignableFrom(valueClass) || Byte.class.isAssignableFrom(valueClass))
                {
                    ps.setByte(fieldIndex, (Byte) value);
                    return;
                }
                break;
            case LONG:
                if (long.class.isAssignableFrom(valueClass) || Long.class.isAssignableFrom(valueClass))
                {
                    ps.setLong(fieldIndex, (Long) value);
                    return;
                }
                break;
            case STRING:
                ps.setString(fieldIndex, (String) value);
                return;
            case ENUMERATION:
                if (valueClass.isEnum())
                {
                    Enum<?> enumValue = (Enum<?>) value;
                    ps.setInt(fieldIndex, enumValue.ordinal());
                    return;
                }
                break;
            case DATE:
                Date d = (Date) value;
                Integer seconds = (int) (d.getTime() / 1000);
                ps.setInt(fieldIndex, seconds);
                return;
            case BOOLEAN:
                Boolean flag = (Boolean) value;
                int intValue = flag ? 1 : 0;
                ps.setInt(fieldIndex, intValue);
                return;
        }
        ps.setObject(fieldIndex, value);
    }

    protected SqlDataField(ResultSet rs, int index, String fieldName, DataType dataType)
    {
        super(fieldName, dataType);

        try
        {
            value = rs.getObject(index);
        }
        catch (SQLException ex)
        {
            log.warning("Persistence: error creating SQLDataRow: " + ex.getMessage());
        }

    }

}
