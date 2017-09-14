package net.epicforce.migrate.ahp.migrate;

/*
 * AbstractStep.java
 *
 * Each step in AHP will have a counterpart in the migration system.
 * That counterpart is responsible for translating AHP's step to whatever
 * platform you're migrating to.
 *
 * The Context will be provided.
 *
 * @author sconley (sconley@epicforce.net)
 */

import net.epicforce.migrate.ahp.context.AbstractContext;
import net.epicforce.migrate.ahp.exception.MigrateException;

import com.urbancode.commons.util.crypto.CryptStringUtil;

import java.security.GeneralSecurityException;

public abstract class AbstractStep
{
    /**
     * This must be implemented by each step.
     *
     * The step will be assumed to run successfully unless an exception
     * is thrown.
     *
     * @param context  The migration context
     * @throws MigrateException on any error.
     */
    public abstract void run(AbstractContext context)
           throws MigrateException;

    /**
     * Decrypt an encrypted AHP string.  This is not normally needed; when
     * using the Property class, getValue automatically decrypts for us.
     *
     * However, sometimes this is needed for certain system passwords.
     * At the time I'm writing this, I only know for sure its needed
     * for source repositories.
     *
     * @param val           The value to decrypt
     * @return the decrypted string
     * @throws MigrateException on the unlikely failure of the decryption
     */
    public String decrypt(final String val)
           throws MigrateException
    {
        try {
            return CryptStringUtil.decrypt(val);
        } catch(GeneralSecurityException e) {
            throw new MigrateException("Error while decrypting Anthill data",
                                       e
            );
        }
    }
}
