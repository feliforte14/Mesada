package com.mesada.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** Cada registro guarda un snapshot de sus macros: si cambia el catálogo, el historial no se altera. */
@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val meal: String,
    val foodId: String?,
    val quantity: Double,
    val customName: String? = null,
    val customEmoji: String? = null,
    val customPortion: String? = null,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val isProduce: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "days")
data class DayEntity(@PrimaryKey val date: String, val steps: Int = 0)

@Entity(tableName = "goals")
data class GoalsEntity(
    @PrimaryKey val id: Int = 0,
    val kcal: Int = 2200,
    val protein: Int = 130,
    val carbs: Int = 230,
    val fat: Int = 70,
)

/** Datos de la persona: se guardan aparte de GoalsEntity para poder recalcular
 *  los objetivos si cambia el peso, sin perder qué parámetros los originaron. */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 0,
    val weightKg: Double,
    val heightCm: Double,
    val age: Int,
    val sex: String,      // "M" | "F"
    val activity: String, // sedentary | light | moderate | active | veryActive
    val goal: String,     // lose | maintain | gain
)

@Dao
interface MesadaDao {
    @Query("SELECT * FROM entries WHERE date = :date ORDER BY createdAt")
    fun entriesFor(date: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE date = :date ORDER BY createdAt")
    suspend fun entriesOnce(date: String): List<EntryEntity>

    @Insert suspend fun insert(entry: EntryEntity): Long

    @Query("DELETE FROM entries WHERE id = :id") suspend fun delete(id: Long)

    @Query("DELETE FROM entries WHERE date = :date") suspend fun clearDay(date: String)

    @Query("SELECT * FROM days WHERE date = :date") fun day(date: String): Flow<DayEntity?>
    @Query("SELECT * FROM days WHERE date = :date") suspend fun dayOnce(date: String): DayEntity?
    @Upsert suspend fun upsertDay(day: DayEntity)

    @Query("SELECT * FROM goals WHERE id = 0") fun goals(): Flow<GoalsEntity?>
    @Query("SELECT * FROM goals WHERE id = 0") suspend fun goalsOnce(): GoalsEntity?
    @Upsert suspend fun upsertGoals(goals: GoalsEntity)

    @Query("SELECT * FROM profile WHERE id = 0") fun profile(): Flow<ProfileEntity?>
    @Query("SELECT * FROM profile WHERE id = 0") suspend fun profileOnce(): ProfileEntity?
    @Upsert suspend fun upsertProfile(profile: ProfileEntity)
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS profile (
                id INTEGER NOT NULL PRIMARY KEY,
                weightKg REAL NOT NULL,
                heightCm REAL NOT NULL,
                age INTEGER NOT NULL,
                sex TEXT NOT NULL,
                activity TEXT NOT NULL,
                goal TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

@Database(
    entities = [EntryEntity::class, DayEntity::class, GoalsEntity::class, ProfileEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class MesadaDatabase : RoomDatabase() {
    abstract fun dao(): MesadaDao

    companion object {
        fun build(context: Context): MesadaDatabase =
            Room.databaseBuilder(context, MesadaDatabase::class.java, "mesada.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
