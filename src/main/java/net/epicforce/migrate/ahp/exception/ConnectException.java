package net.epicforce.migrate.ahp.exception;

/**
 * ConnectException
 *
 * An error happening while connecting to AHP.  Could be network or
 * could be authentication credentials.
 *
 * @author sconley (sconley@epicforce.net)
 */
public class ConnectException extends MigrateException
{
    public ConnectException(final String msg)
    {
        super(msg);
    }

    public ConnectException(final String msg, final Exception e)
    {
        super(msg, e);
    }
}
