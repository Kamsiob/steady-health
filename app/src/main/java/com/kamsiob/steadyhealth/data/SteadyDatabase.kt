package com.kamsiob.steadyhealth.data

import android.content.Context
import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.kamsiob.steadyhealth.data.dao.AbilityDao
import com.kamsiob.steadyhealth.data.dao.BodyDao
import com.kamsiob.steadyhealth.data.dao.CheckDao
import com.kamsiob.steadyhealth.data.dao.CheckInDao
import com.kamsiob.steadyhealth.data.dao.DailyPromptDao
import com.kamsiob.steadyhealth.data.dao.DocumentDao
import com.kamsiob.steadyhealth.data.dao.LadderDao
import com.kamsiob.steadyhealth.data.dao.NotesDao
import com.kamsiob.steadyhealth.data.dao.NoticeDao
import com.kamsiob.steadyhealth.data.dao.PlanDao
import com.kamsiob.steadyhealth.data.dao.ProfileDao
import com.kamsiob.steadyhealth.data.dao.ReminderDao
import com.kamsiob.steadyhealth.data.dao.RunDao
import com.kamsiob.steadyhealth.data.dao.SessionDao
import com.kamsiob.steadyhealth.data.dao.SynonymDao
import com.kamsiob.steadyhealth.data.dao.WeighInDao
import com.kamsiob.steadyhealth.data.entity.BloodPressureEntity
import com.kamsiob.steadyhealth.data.entity.CheckEntity
import com.kamsiob.steadyhealth.data.entity.CheckInEntity
import com.kamsiob.steadyhealth.data.entity.CheckInTagEntity
import com.kamsiob.steadyhealth.data.entity.DailyPromptEntity
import com.kamsiob.steadyhealth.data.entity.DocumentEntity
import com.kamsiob.steadyhealth.data.entity.DocumentPageEntity
import com.kamsiob.steadyhealth.data.entity.ExclusionEntity
import com.kamsiob.steadyhealth.data.entity.ExperimentEntity
import com.kamsiob.steadyhealth.data.entity.ItemRatingEntity
import com.kamsiob.steadyhealth.data.entity.LadderStateEntity
import com.kamsiob.steadyhealth.data.entity.MeasureResultEntity
import com.kamsiob.steadyhealth.data.entity.NoticeEntity
import com.kamsiob.steadyhealth.data.entity.PatternEntity
import com.kamsiob.steadyhealth.data.entity.PersonSynonymEntity
import com.kamsiob.steadyhealth.data.entity.PhotoEntity
import com.kamsiob.steadyhealth.data.entity.PlanEntity
import com.kamsiob.steadyhealth.data.entity.PlanItemEntity
import com.kamsiob.steadyhealth.data.entity.ReadinessEntity
import com.kamsiob.steadyhealth.data.entity.ReminderSentEntity
import com.kamsiob.steadyhealth.data.entity.RunEntity
import com.kamsiob.steadyhealth.data.entity.RunMovementEntity
import com.kamsiob.steadyhealth.data.entity.SessionEntity
import com.kamsiob.steadyhealth.data.entity.SettingEntity
import com.kamsiob.steadyhealth.data.entity.SoreAreaEntity
import com.kamsiob.steadyhealth.data.entity.StepNameEntity
import com.kamsiob.steadyhealth.data.entity.TrackedItemEntity
import com.kamsiob.steadyhealth.data.entity.VisitSummaryEntity
import com.kamsiob.steadyhealth.data.entity.WaistEntity
import com.kamsiob.steadyhealth.data.entity.WeeklyNoteEntity
import com.kamsiob.steadyhealth.data.entity.WeighInEntity
import kotlinx.coroutines.Dispatchers
import net.zetetic.database.sqlcipher.driver.SQLCipherDriver

/**
 * The one database, encrypted at rest.
 *
 * Everything the app stores is in here, which is what makes the promise on the
 * privacy page checkable rather than rhetorical: there is one file, it is
 * encrypted with a key that cannot leave the phone, and deleting it deletes
 * everything.
 *
 * Room 3 opens it through a driver rather than an open helper, which is the
 * change from Room 2 that matters here: `setDriver` replaced
 * `openHelperFactory`, and SQLCipher 4.18 ships a driver for it.
 */
@Database(
    entities = [
        WeighInEntity::class,
        CheckInEntity::class,
        CheckInTagEntity::class,
        PersonSynonymEntity::class,
        SessionEntity::class,
        StepNameEntity::class,
        LadderStateEntity::class,
        TrackedItemEntity::class,
        ItemRatingEntity::class,
        CheckEntity::class,
        MeasureResultEntity::class,
        WaistEntity::class,
        BloodPressureEntity::class,
        PhotoEntity::class,
        WeeklyNoteEntity::class,
        RunEntity::class,
        RunMovementEntity::class,
        SoreAreaEntity::class,
        DailyPromptEntity::class,
        DocumentEntity::class,
        DocumentPageEntity::class,
        PlanEntity::class,
        PlanItemEntity::class,
        PatternEntity::class,
        ExperimentEntity::class,
        VisitSummaryEntity::class,
        NoticeEntity::class,
        ReminderSentEntity::class,
        SettingEntity::class,
        ExclusionEntity::class,
        ReadinessEntity::class,
    ],
    // Version 2 adds the three tables ADDENDUM-03's session needs. Purely additive,
    // so the migration is generated rather than written, and nobody's rows move.
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ],
)
abstract class SteadyDatabase : RoomDatabase() {

    abstract fun weighIns(): WeighInDao
    abstract fun checkIns(): CheckInDao
    abstract fun synonyms(): SynonymDao
    abstract fun sessions(): SessionDao
    abstract fun ladders(): LadderDao
    abstract fun abilities(): AbilityDao
    abstract fun checks(): CheckDao
    abstract fun body(): BodyDao
    abstract fun notes(): NotesDao

    abstract fun runs(): RunDao
    abstract fun notices(): NoticeDao
    abstract fun reminders(): ReminderDao
    abstract fun dailyPrompts(): DailyPromptDao
    abstract fun documents(): DocumentDao
    abstract fun plans(): PlanDao
    abstract fun profile(): ProfileDao

    companion object {
        private const val NAME = "steady.db"

        @Volatile
        private var instance: SteadyDatabase? = null

        /**
         * Opened on first use rather than at startup, per C7's reasoning applied
         * to the database: an app that opens an encrypted file before it knows
         * whether this launch will read one is slower at the only moment somebody
         * is watching.
         */
        fun get(context: Context): SteadyDatabase = instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

        /**
         * A separate database, for the device tests that have to destroy one.
         *
         * The template rule is that data-affecting tests do not run against the
         * owner's data, and a test proving that destroy leaves nothing behind is
         * the most data-affecting test there is. It ran against the real database
         * once, on the phone, and wiped a real setup. Now it cannot.
         */
        fun forTesting(context: Context, name: String, alias: String): SteadyDatabase =
            build(context.applicationContext, name, alias)

        fun destroyTesting(context: Context, name: String, alias: String) {
            DatabaseKey.destroy(context, alias)
            val dir = context.applicationContext.getDatabasePath(name).parentFile
            dir?.listFiles()?.filter { it.name.startsWith(name) }?.forEach { it.delete() }
        }

        private fun build(
            context: Context,
            name: String = NAME,
            alias: String? = null,
        ): SteadyDatabase {
            // SQLCipher is a native library and nothing loads it for you. Without
            // this the first query throws UnsatisfiedLinkError, which is a crash
            // on the first screen that reads anything, and it cannot be caught by
            // a JVM test because there is no native library there to be missing.
            System.loadLibrary("sqlcipher")

            val passphrase = if (alias == null) {
                DatabaseKey.passphrase(context)
            } else {
                DatabaseKey.passphrase(context, alias)
            }
            return Room.databaseBuilder(context, SteadyDatabase::class.java, name)
                .setDriver(SQLCipherDriver(passphrase, null, null))
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }

        /**
         * Everything, gone, in the order that leaves nothing behind.
         *
         * Close, then destroy the key, then delete the files. The other order
         * leaves a key in the Keystore for a database that no longer exists, and
         * a promise that says "there is no copy anywhere else" should be true
         * about the key as well as about the rows.
         */
        fun destroy(context: Context) {
            synchronized(this) {
                instance?.close()
                instance = null
            }
            DatabaseKey.destroy(context)

            // Everything the database name owns, rather than a list of the
            // suffixes we happen to know about. On the phone this turned up a
            // steady.db.lck that a hardcoded list of -wal, -shm and -journal did
            // not, and a leftover file makes "there is no copy anywhere else"
            // false in the only way that matters.
            val dir = context.applicationContext.getDatabasePath(NAME).parentFile
            dir?.listFiles()?.filter { it.name.startsWith(NAME) }?.forEach { it.delete() }
        }
    }
}
