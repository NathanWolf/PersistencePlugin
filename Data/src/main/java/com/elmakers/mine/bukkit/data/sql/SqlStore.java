package com.elmakers.mine.bukkit.data.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.elmakers.mine.bukkit.data.DataField;
import com.elmakers.mine.bukkit.data.DataRow;
import com.elmakers.mine.bukkit.data.DataStore;
import com.elmakers.mine.bukkit.data.DataTable;
import com.elmakers.mine.bukkit.data.DataType;

/**
 * An abstract base clase for all JDBC-SQL-based stores.
 * 
 * @author NathanWolf
 * 
 */
public abstract class SqlStore extends DataStore
{
    protected static boolean driversLoaded    = false;
    protected static boolean logSqlStatements = false;

    public static void logSqlStatement(String statement)
    {
        if (logSqlStatements)
        {
            log.info("Persistence: SQL: " + statement);
        }
    }

    protected Connection connection = null;

    public SqlStore(String schema)
    {
        super(schema);
    }

    @Override
    public boolean clear(DataTable table)
    {
        String deleteSql = "DELETE FROM \"" + table.getName() + "\"";

        try
        {
            PreparedStatement deleteStatement = connection.prepareStatement(deleteSql);
            logSqlStatement(deleteSql);
            deleteStatement.execute();
        }
        catch (SQLException ex)
        {
            log.warning("Persistence: Error deleting list " + table.getName() + ": " + ex.getMessage());
            log.info(deleteSql);
            return false;
        }

        if (table.getRows().size() > 0)
        {
            save(table);
        }

        return true;
    }

    @Override
    public boolean clearIds(DataTable table, List<Object> ids)
    {
        int rowCount = 0;

        if (ids.size() <= 0)
        {
            return true;
        }

        List<String> idFields = table.getIdFieldNames();
        if (idFields.size() < 1)
        {
            return false;
        }

        String tableName = table.getName();
        String idField = idFields.get(0);
        String deleteSql = "DELETE FROM \"" + tableName + "\" WHERE \"" + idField + "\" IN (";

        boolean firstId = true;
        for (int i = 0; i < ids.size(); i++)
        {
            if (!firstId)
            {
                deleteSql += ", ";
            }
            firstId = false;
            deleteSql += "?";
            rowCount++;
        }
        deleteSql += ")";

        try
        {
            PreparedStatement deleteStatement = connection.prepareStatement(deleteSql);

            int index = 1;
            for (Object id : ids)
            {
                deleteStatement.setObject(index, id);
                index++;
            }
            logSqlStatement(deleteSql);
            deleteStatement.execute();
        }
        catch (SQLException ex)
        {
            log.warning("Persistence: Error deleting ids " + tableName + ": " + ex.getMessage());
            log.info(deleteSql);
            return false;
        }

        if (table.getRows().size() > 0)
        {
            save(table);
        }

        logStoreAccess("Persistence: deleted %d objects from " + schema + "." + tableName, rowCount);

        return true;
    }

    @Override
    public boolean connect()
    {
        if (connection != null)
        {
            boolean closed = true;
            try
            {
                closed = connection.isClosed();
            }
            catch (SQLException ex)
            {
                closed = true;
            }
            if (!closed)
            {
                return true;
            }
        }

        // Try to load drivers if necessary
        if (!driversLoaded)
        {
            // Check to see if the driver is loaded
            String jdbcClass = getDriverClassName();
            try
            {
                Class.forName(jdbcClass);
                driversLoaded = true;
            }
            catch (ClassNotFoundException e)
            {
                driversLoaded = false;
            }
        }

        // Create or connect to the database

        // TODO: user, password
        String user = "";
        String password = "";

        try
        {
            connection = DriverManager.getConnection(getConnectionString(schema, user, password));
        }
        catch (SQLException e)
        {
            connection = null;
            log.severe("Permissions: error connecting to sqllite db: " + e.getMessage());
        }

        return isConnected() && onConnect();
    }

    @Override
    public boolean create(DataTable table)
    {
        String tableName = table.getName();
        String createStatement = "CREATE TABLE \"" + tableName + "\" (";
        int fieldCount = 0;
        DataRow header = table.getHeader();
        for (DataField field : header.getFields())
        {
            if (fieldCount != 0)
            {
                createStatement += ",";
            }
            fieldCount++;

            createStatement += "\"" + field.getName() + "\" " + getTypeName(field.getType());
        }

        List<String> idFields = table.getIdFieldNames();
        createStatement += ", PRIMARY KEY (";
        boolean firstField = true;
        for (String id : idFields)
        {
            if (!firstField)
            {
                createStatement += ", ";
            }
            firstField = false;

            createStatement += "\"" + id + "\"";
        }
        createStatement += "))";

        if (fieldCount == 0)
        {
            log.warning("Persistence: class " + tableName + " has no fields");
            return false;
        }

        logStoreAccess("Persistence: Created table " + schema + "." + tableName);
        try
        {
            PreparedStatement ps = connection.prepareStatement(createStatement);
            logSqlStatement(createStatement);
            ps.execute();
        }
        catch (SQLException ex)
        {
            log.severe("Peristence: error creating table: " + ex.getMessage());
            log.info(createStatement);
        }

        return true;
    }

    @Override
    public void disconnect()
    {
        if (connection != null)
        {
            try
            {
                connection.close();
            }
            catch (SQLException e)
            {

            }
        }
        connection = null;
    }

    @Override
    public boolean drop(String tableName)
    {
        if (tableExists(tableName))
        {
            String dropQuery = "DROP TABLE \"" + tableName + "\"";
            try
            {
                PreparedStatement ps = connection.prepareStatement(dropQuery);
                logSqlStatement(dropQuery);
                ps.execute();
            }
            catch (SQLException ex)
            {
                log.severe("Persistence: error dropping table: " + ex.getMessage());
                log.info(dropQuery);
                return false;
            }
            logStoreAccess("Dropped table " + schema + "." + tableName);
        }
        return true;
    }

    public abstract String getConnectionString(String schema, String user, String password);

    public abstract String getDriverClassName();

    public abstract String getMasterTableName();

    public abstract String getTypeName(DataType dataType);

    public boolean isConnected()
    {
        boolean isClosed = true;
        try
        {
            isClosed = connection == null || connection.isClosed();
        }
        catch (SQLException e)
        {
            isClosed = true;
        }
        return connection != null && !isClosed;
    }

    @Override
    public boolean load(DataTable table)
    {
        String tableName = table.getName();

        // Select all columns instead of building a column list
        // This lets me sort out missing columns instead of throwing SQL errors.
        String selectQuery = "SELECT * FROM \"" + tableName + "\"";

        int rowCount = load(table, selectQuery);
        logStoreAccess("Persistence: loaded %d objects from " + schema + "." + tableName, rowCount);

        return rowCount >= 0;
    }

    protected int load(DataTable table, String sqlQuery)
    {
        int rowCount = 0;
        try
        {
            PreparedStatement ps = connection.prepareStatement(sqlQuery);
            logSqlStatement(sqlQuery);
            ResultSet rs = ps.executeQuery();

            while (rs.next())
            {
                SqlDataRow row = new SqlDataRow(table, rs);
                table.addRow(row);
                rowCount++;
            }
            rs.close();
        }
        catch (SQLException ex)
        {
            log.warning("Persistence: Error selecting from table " + table.getName() + ": " + ex.getMessage());
            return -1;
        }

        return rowCount;
    }

    /**
     * Called on connect- override to perform special actions on connect.
     * 
     * @return false if the connection failed.
     */
    public boolean onConnect()
    {
        return true;
    }

    @Override
    public boolean save(DataTable table)
    {
        int rowCount = 0;

        if (table.getRows().size() == 0)
        {
            return clear(table);
        }

        String tableName = table.getName();
        String fieldList = "";
        String valueList = "";
        int fieldCount = 0;
        DataRow header = table.getHeader();

        for (DataField field : header.getFields())
        {
            if (fieldCount != 0)
            {
                fieldList += ", ";
                valueList += ", ";
            }
            fieldCount++;
            fieldList += "\"" + field.getName() + "\"";
            valueList += "?";
        }

        if (fieldCount == 0)
        {
            log.warning("Persistence: class " + tableName + " has no fields");
            return false;
        }

        String updateSql = "INSERT OR REPLACE INTO \"" + tableName + "\" (" + fieldList + ") VALUES (" + valueList + ")";
        for (DataRow row : table.getRows())
        {
            try
            {
                PreparedStatement updateStatement = connection.prepareStatement(updateSql);

                int index = 1;
                List<DataField> fields = row.getFields();
                for (DataField field : fields)
                {
                    SqlDataField.setValue(updateStatement, index, field.getValue(), field.getType());
                    index++;
                }

                rowCount++;
                logSqlStatement(updateSql);
                updateStatement.execute();
            }
            catch (SQLException ex)
            {
                log.warning("Persistence: Error updating table " + tableName + ": " + ex.getMessage());
                log.info(updateSql);
                return false;
            }

        }

        logStoreAccess("Persistence: saved %d objects to " + schema + "." + tableName, rowCount);

        return true;
    }

    @Override
    public boolean tableExists(String tableName)
    {
        String checkQuery = "SELECT name FROM \"" + getMasterTableName() + "\" WHERE type='table' AND name='" + tableName + "'";
        boolean tableExists = false;
        try
        {
            PreparedStatement ps = connection.prepareStatement(checkQuery);
            logSqlStatement(checkQuery);
            ResultSet rs = ps.executeQuery();
            tableExists = rs.next();
            rs.close();
        }
        catch (SQLException ex)
        {
            log.severe("Persistence: Error getting table data: " + ex.getMessage());
            log.info(checkQuery);
            return false;
        }
        return tableExists;
    }
}
