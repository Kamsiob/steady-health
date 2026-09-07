package com.kamsiob.steadyhealth.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The database passphrase, and where it lives.
 *
 * A random 32-byte passphrase is generated once, wrapped with an AES-256-GCM key
 * held in the Android Keystore, and the wrapped bytes are written beside the
 * database. The unwrapping key never leaves the Keystore and cannot be read out
 * of it, which is the whole point: a copy of the database file taken off the
 * device is a file nobody can open, including us.
 *
 * StrongBox is used where the phone has it and the TEE where it does not, because
 * asking for StrongBox on a device without it throws rather than degrading.
 *
 * This is also why `allowBackup` is false. A backup of an encrypted database
 * without the key is a file that will never open again, and offering one would be
 * a promise the app cannot keep.
 */
object DatabaseKey {

    private const val KEY_ALIAS = "steady_health_db"

    /**
     * A second key, for tests that need to destroy one.
     *
     * The device test suite has to prove that destroying leaves no key behind,
     * and it must not prove that against the key protecting the owner's own rows.
     * Kept in production code rather than in the test source because the alias
     * has to be excluded from the real key's lifecycle in both places.
     */
    const val TEST_ALIAS = "steady_health_db_test"
    private const val KEY_FILE = "steady_db.key"
    private const val PASSPHRASE_BYTES = 32
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    /** The passphrase, created on first use and read back on every launch after. */
    fun passphrase(context: Context, alias: String = KEY_ALIAS): ByteArray {
        val file = keyFile(context, alias)
        return if (file.exists()) unwrap(file.readBytes(), alias) else create(file, alias)
    }

    /**
     * Destroys the key, which is what makes "there is no other copy" true.
     *
     * Called by delete-everything before the database files are removed. Without
     * this the files would be gone and the key would remain, which is tidy rather
     * than complete.
     */
    fun destroy(context: Context, alias: String = KEY_ALIAS) {
        runCatching { keyStore().deleteEntry(alias) }
        keyFile(context, alias).delete()
    }

    /** True when a key exists at all, which the device test asserts against. */
    fun exists(context: Context, alias: String = KEY_ALIAS): Boolean =
        keyFile(context, alias).exists() || runCatching { keyStore().containsAlias(alias) }
            .getOrDefault(false)

    /**
     * The wrapped passphrase file, one per key.
     *
     * The test key needs its own file as well as its own alias, or destroying the
     * test key would delete the file protecting the owner's rows and leave the
     * app with a database it can never open again.
     */
    private fun keyFile(context: Context, alias: String): File =
        if (alias == KEY_ALIAS) {
            File(context.filesDir, KEY_FILE)
        } else {
            File(context.filesDir, "$KEY_FILE.$alias")
        }

    private fun create(file: File, alias: String): ByteArray {
        val passphrase = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, wrappingKey(alias))
        }
        val wrapped = cipher.iv + cipher.doFinal(passphrase)
        file.writeBytes(wrapped)
        return passphrase
    }

    private fun unwrap(wrapped: ByteArray, alias: String): ByteArray {
        val iv = wrapped.copyOfRange(0, IV_BYTES)
        val body = wrapped.copyOfRange(IV_BYTES, wrapped.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, wrappingKey(alias), GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return cipher.doFinal(body)
    }

    private fun wrappingKey(alias: String): SecretKey {
        val store = keyStore()
        (store.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setUserAuthenticationRequired(false)

        return runCatching { generate(spec.setIsStrongBoxBacked(true).build()) }
            .getOrElse { generate(spec.setIsStrongBoxBacked(false).build()) }
    }

    private fun generate(spec: KeyGenParameterSpec): SecretKey =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
            .apply { init(spec) }
            .generateKey()

    private fun keyStore(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    private const val PROVIDER = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_SIZE_BITS = 256
}
