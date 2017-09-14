package net.epicforce.migrate.ahp.exception;

/**
 * UnsupportedClassException
 *
 * This is thrown when the loader tries to load a class that is not
 * supported (such as an unimplemented step)
 *
 * @author sconley (sconley@epicforce.net)
 */
public class UnsupportedClassException extends MigrateException
{
    public UnsupportedClassException(final String msg)
    {
        super(msg);
    }

    public UnsupportedClassException(final String msg, final Exception e)
    {
        super(msg, e);
    }
}
