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

/** Alimento creado por la persona: se suma al catálogo fijo. Guarda las macros cada 100 g/ml. */
@Entity(tableName = "custom_foods")
data class CustomFoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // Category.name
    val measure: String,  // Measure.name
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val gramsPerPiece: Double = 0.0,
    val unitSingular: String = "",
    val unitPlural: String = "",
)

/** Receta creada por la persona. items = "foodId:cantidad;foodId:cantidad". */
@Entity(tableName = "custom_recipes")
data class CustomRecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val items: String,
)

/** Ids de alimentos del catálogo fijo que la persona eligió ocultar (borrado lógico). */
@Entity(tableName = "hidden_foods")
data class HiddenFoodEntity(@PrimaryKey val id: String)

@Dao
interface MesadaDao {
    @Query("SELECT * FROM entries WHERE date = :date ORDER BY createdAt")
    fun entriesFor(date: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE date = :date ORDER BY createdAt")
    suspend fun entriesOnce(date: String): List<EntryEntity>

    @Query("SELECT * FROM entries ORDER BY date DESC, createdAt")
    fun allEntries(): Flow<List<EntryEntity>>

    @Insert suspend fun insert(entry: EntryEntity): Long

    @Query("DELETE FROM entries WHERE id = :id") suspend fun delete(id: Long)

    @Query("DELETE FROM entries WHERE date = :date") suspend fun clearDay(date: String)

    @Query("SELECT * FROM custom_foods ORDER BY name") fun customFoods(): Flow<List<CustomFoodEntity>>
    @Query("SELECT * FROM custom_foods") suspend fun customFoodsOnce(): List<CustomFoodEntity>
    @Upsert suspend fun upsertCustomFood(food: CustomFoodEntity)
    @Query("DELETE FROM custom_foods WHERE id = :id") suspend fun deleteCustomFood(id: String)

    @Query("SELECT * FROM custom_recipes ORDER BY name") fun customRecipes(): Flow<List<CustomRecipeEntity>>
    @Upsert suspend fun upsertCustomRecipe(recipe: CustomRecipeEntity)
    @Query("DELETE FROM custom_recipes WHERE id = :id") suspend fun deleteCustomRecipe(id: String)

    @Query("SELECT * FROM hidden_foods") fun hiddenFoods(): Flow<List<HiddenFoodEntity>>
    @Query("SELECT * FROM hidden_foods") suspend fun hiddenFoodsOnce(): List<HiddenFoodEntity>
    @Upsert suspend fun hideFood(row: HiddenFoodEntity)
    @Query("DELETE FROM hidden_foods WHERE id = :id") suspend fun unhideFood(id: String)

    @Query("SELECT * FROM days WHERE date = :date") fun day(date: String): Flow<DayEntity?>
    @Query("SELECT * FROM days WHERE date = :date") suspend fun dayOnce(date: String): DayEntity?
    @Query("SELECT * FROM days ORDER BY date DESC") fun allDays(): Flow<List<DayEntity>>
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

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS custom_foods (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                measure TEXT NOT NULL,
                kcal REAL NOT NULL,
                protein REAL NOT NULL,
                carbs REAL NOT NULL,
                fat REAL NOT NULL,
                gramsPerPiece REAL NOT NULL,
                unitSingular TEXT NOT NULL,
                unitPlural TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS custom_recipes (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                items TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS hidden_foods (id TEXT NOT NULL PRIMARY KEY)")
    }
}

@Database(
    entities = [
        EntryEntity::class, DayEntity::class, GoalsEntity::class, ProfileEntity::class,
        CustomFoodEntity::class, CustomRecipeEntity::class, HiddenFoodEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class MesadaDatabase : RoomDatabase() {
    abstract fun dao(): MesadaDao

    companion object {
        fun build(context: Context): MesadaDatabase =
            Room.databaseBuilder(context, MesadaDatabase::class.java, "mesada.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
