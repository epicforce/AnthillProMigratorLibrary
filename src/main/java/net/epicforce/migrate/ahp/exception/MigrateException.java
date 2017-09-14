package net.epicforce.migrate.ahp.exception;

/**
 * MigrateException - base class for AHP Migration exceptions.
 *
 * Can also be used on its own for a generic error.
 *
 * @author sconley (sconley@epicforce.net)
 */
public class MigrateException extends Exception
{
    public MigrateException(final String msg)
    {
        super(msg);
    }

    public MigrateException(final String msg, final Exception e)
    {
        super(msg + ": " + e.getMessage(), e);
    }
}
