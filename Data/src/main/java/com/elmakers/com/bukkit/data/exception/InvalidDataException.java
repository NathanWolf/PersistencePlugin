package com.elmakers.com.bukkit.data.exception;

import com.elmakers.mine.bukkit.data.DataField;
import com.elmakers.mine.bukkit.data.DataRow;
import com.elmakers.mine.bukkit.data.DataTable;

public class InvalidDataException extends Exception
{
    /**
     * Need to support Serializable via Exception
     */
    private static final long serialVersionUID = 1L;

    DataField                 dataField        = null;

    DataRow                   dataRow          = null;

    DataTable                 dataTable        = null;

    public InvalidDataException(DataTable dataTable)
    {
        this.dataTable = dataTable;
    }

    public InvalidDataException(DataTable dataTable, DataRow dataRow)
    {
        this.dataTable = dataTable;
        this.dataRow = dataRow;
    }

    public InvalidDataException(DataTable dataTable, DataRow dataRow, DataField dataField)
    {
        this.dataTable = dataTable;
        this.dataRow = dataRow;
        this.dataField = dataField;
    }

    public InvalidDataException(DataTable dataTable, DataRow dataRow, DataField dataField, String message)
    {
        super(message);
        this.dataTable = dataTable;
        this.dataRow = dataRow;
        this.dataField = dataField;
    }

    public InvalidDataException(DataTable dataTable, DataRow dataRow, String message)
    {
        super(message);
        this.dataTable = dataTable;
        this.dataRow = dataRow;
    }

    public InvalidDataException(DataTable dataTable, DataRow dataRow, Throwable cause)
    {
        super(cause);
        this.dataTable = dataTable;
        this.dataRow = dataRow;
    }

    public InvalidDataException(DataTable dataTable, String message)
    {
        super(message);
        this.dataTable = dataTable;
    }

    public InvalidDataException(String message)
    {
        super(message);
    }

    public InvalidDataException(Throwable cause)
    {
        super(cause);
    }

    public DataField getDataField()
    {
        return dataField;
    }

    public DataRow getDataRow()
    {
        return dataRow;
    }

    public DataTable getDataTable()
    {
        return dataTable;
    }
}
