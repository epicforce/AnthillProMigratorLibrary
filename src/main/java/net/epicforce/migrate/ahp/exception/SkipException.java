package net.epicforce.migrate.ahp.exception;

/**
 * SkipException -- This is a type of exception that is used to
 * indicate that whatever we're trying to migrate should be skipped.
 *
 * This can be used by jobs and steps at the moment.
 *
 * @author sconley (sconley@epicforce.net)
 */
public class SkipException extends MigrateException
{
    public SkipException(final String msg)
    {
        super(msg);
    }

    public SkipException(final String msg, final Exception e)
    {
        super(msg, e);
    }
}
