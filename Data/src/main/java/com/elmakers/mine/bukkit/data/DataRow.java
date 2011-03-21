package com.elmakers.mine.bukkit.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Used to represent a single row of data from a data store.
 * 
 * @author NathanWolf
 * 
 */
public class DataRow
{
    protected static Logger              log      = DataStore.getLogger();

    protected HashMap<String, DataField> fieldMap = new HashMap<String, DataField>();

    protected List<DataField>            fields   = new ArrayList<DataField>();

    protected DataTable                  table;

    /**
     * Create an empty DataRow.
     * 
     * This can be used to populate with data for setting data.
     * 
     */
    public DataRow(DataTable dataTable)
    {
        this.table = dataTable;
    }

    /**
     * Add a new field to this row.
     * 
     * This is used to set up a row for writing to a store. Do not attempt to
     * add the same field name twice.
     * 
     * @param newField
     *            The datafield to add.
     */
    public void add(DataField newField)
    {
        String fieldName = newField.getName();
        if (fieldName == null || fieldName.length() <= 0)
        {
            log.warning("Persistence: Empty DataRow name");
            return;
        }

        DataField existingField = fieldMap.get(fieldName);
        if (existingField != null)
        {
            log.warning("Persistence: Warning, duplicate field in DataRow: " + fieldName);
            return;
        }

        if (newField.isIdField())
        {
            table.addIdFieldName(newField.getName());
        }

        fieldMap.put(fieldName, newField);
        fields.add(newField);
    }

    /**
     * Retrieve a data field from this row by name.
     * 
     * @param columnName
     *            The name of the DataField to find
     * @return A data field, or null if not found
     */
    public DataField get(String columnName)
    {
        return fieldMap.get(columnName);
    }

    public DataField getField(String fieldName)
    {
        return fieldMap.get(fieldName);
    }

    /**
     * Retrieve all of the fields in this row.
     * 
     * @return The internal list of fields
     */
    public final List<DataField> getFields()
    {
        return fields;
    }

    /**
     * Get the table for this row.
     * 
     * @return The table this row comes from
     */
    public DataTable getTable()
    {
        return table;
    }

    public boolean isMigrationRequired(DataRow storeTableHeader)
    {
        for (DataField field : fields)
        {
            DataField storeField = storeTableHeader.getField(field.getName());

            // TODO: Type compatibilty check:
            // || storeField.getType() != field.getType())
            // not sufficient ^ Will always auto-migrate.

            if (storeField == null)
            {
                return true;
            }
        }

        return false;
    }
}
