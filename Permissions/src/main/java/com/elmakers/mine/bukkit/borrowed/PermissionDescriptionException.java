package com.elmakers.mine.bukkit.borrowed;

/**
 * Thrown when attempting to deal with an invalid PermissionDescription
 */
public class PermissionDescriptionException extends Exception
{

    /**
     * Creates a new instance of <code>PermissionDescriptionException</code>
     * without a detailed message.
     */
    public PermissionDescriptionException()
    {
    }

    /**
     * Constructs an instance of <code>PermissionDescriptionException</code>
     * with the specified detailed message.
     * 
     * @param msg
     *            the detailed message.
     */
    public PermissionDescriptionException(String msg)
    {
        super(msg);
    }

    public PermissionDescriptionException(String msg, Throwable cause)
    {
        super(msg, cause);
    }

    public PermissionDescriptionException(Throwable cause)
    {
        super(cause);
    }
}
