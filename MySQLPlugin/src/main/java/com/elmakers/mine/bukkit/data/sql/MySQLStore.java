package com.elmakers.mine.bukkit.data.sql;

import java.util.List;

import com.elmakers.mine.bukkit.data.DataField;
import com.elmakers.mine.bukkit.data.DataRow;
import com.elmakers.mine.bukkit.data.DataTable;
import com.elmakers.mine.bukkit.data.DataType;

public class MySQLStore extends SqlStore
{
    protected String password = null;

    protected String server   = null;

    protected String user     = null;

    public MySQLStore(String schema, String server, String user, String password)
    {
        super(schema);
        this.server = server;
        this.user = user;
        this.password = password;
    }

    @Override
    public String getConnectionString(String schema, String user, String password)
    {
        return "jdbc:mysql://" + server + "/" + schema + "?user=" + user + "&password=" + password;
    }

    @Override
    public String getDriverClassName()
    {
        return "com.mysql.jdbc.Driver";
    }

    @Override
    public String getMasterTableName()
    {
        return "information_schema.tables";
    }

    @Override
    public DataTable getTableHeader(String tableName)
    {
        DataTable currentTable = new DataTable(tableName);
        currentTable.createHeader();
        DataRow headerRow = currentTable.getHeader();

        DataTable pragmaTable = new DataTable("pragma");
        String pragmaSql = "PRAGMA TABLE_INFO(\"" + tableName + "\")";
        load(pragmaTable, pragmaSql);

        List<DataRow> pragmaRows = pragmaTable.getRows();
        for (DataRow row : pragmaRows)
        {
            DataField nameField = row.get("name");
            DataField typeField = row.get("type");

            if (nameField == null || typeField == null)
            {
                continue;
            }

            String fieldName = (String) nameField.getValue();
            String fieldType = (String) typeField.getValue();
            DataType dataType = getTypeFromName(fieldType);

            DataField newColumn = new DataField(fieldName, dataType);
            headerRow.add(newColumn);
        }

        return currentTable;
    }

    // This is all really pointless since SqlLite doesn't even really type
    // things :\
    public DataType getTypeFromName(String typeName)
    {
        if (typeName.equalsIgnoreCase("INTEGER"))
        {
            return DataType.LONG;
        }
        else if (typeName.equalsIgnoreCase("REAL"))
        {
            return DataType.DOUBLE;
        }
        else if (typeName.equalsIgnoreCase("TEXT"))
        {
            return DataType.STRING;
        }

        return DataType.NULL;
    }

    @Override
    public String getTypeName(DataType dataType)
    {
        switch (dataType)
        {
            case INTEGER:
                return "INTEGER";
            case BYTE:
                return "INTEGER";
            case LONG:
                return "INTEGER";
            case BOOLEAN:
                return "INTEGER";
            case DATE:
                return "INTEGER";
            case DOUBLE:
                return "REAL";
            case FLOAT:
                return "REAL";
            case STRING:
                return "TEXT";
        }
        return null;
    }
}
