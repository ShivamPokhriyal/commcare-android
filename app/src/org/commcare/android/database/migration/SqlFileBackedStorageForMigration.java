package org.commcare.android.database.migration;

import android.content.ContentValues;

import org.commcare.android.crypt.CryptUtil;
import org.commcare.android.database.AndroidDbHelper;
import org.commcare.android.database.SqlFileBackedStorage;
import org.commcare.modern.database.DatabaseHelper;
import org.javarosa.core.services.storage.Persistable;

/**
 * File backed storage for use before session w/ key have been setup:
 * when performing user db migrations
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class SqlFileBackedStorageForMigration<T extends Persistable> extends SqlFileBackedStorage<T> {
    private final byte[] fileKeySeed;

    public SqlFileBackedStorageForMigration(String table,
                                            Class<? extends T> ctype,
                                            AndroidDbHelper helper,
                                            String baseDir,
                                            byte[] fileKeySeed) {
        super(table, ctype, helper, baseDir);

        this.fileKeySeed = fileKeySeed;
    }

    @Override
    protected byte[] generateKey(ContentValues contentValues) {
        byte[] key = CryptUtil.generateSymetricKey(CryptUtil.uniqueSeedFromSecureStatic(fileKeySeed)).getEncoded();
        contentValues.put(DatabaseHelper.AES_COL, key);
        return key;
    }
}
