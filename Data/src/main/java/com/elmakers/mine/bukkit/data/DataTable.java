package com.elmakers.mine.bukkit.data;

import java.util.ArrayList;
import java.util.List;

public class DataTable
{
    protected List<String>  idFieldNames = new ArrayList<String>();

    protected String        name;

    protected List<DataRow> rows         = new ArrayList<DataRow>();

    public DataTable(String name)
    {
        this.name = name;
    }

    public void addIdFieldName(String idFieldName)
    {
        if (!idFieldNames.contains(idFieldName))
        {
            idFieldNames.add(idFieldName);
        }
    }

    public void addRow(DataRow row)
    {
        rows.add(row);
    }

    public void createHeader()
    {
        if (rows.size() > 0)
        {
            return;
        }

        DataRow headerRow = new DataRow(this);
        rows.add(headerRow);
    }

    public DataRow getHeader()
    {
        if (rows.size() == 0)
        {
            return null;
        }

        return rows.get(0);
    }

    public List<String> getIdFieldNames()
    {
        return idFieldNames;
    }

    public String getName()
    {
        return name;
    }

    public final List<DataRow> getRows()
    {
        return rows;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
