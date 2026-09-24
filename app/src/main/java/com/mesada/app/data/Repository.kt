package com.mesada.app.data

import com.mesada.app.data.db.CustomFoodEntity
import com.mesada.app.data.db.CustomRecipeEntity
import com.mesada.app.data.db.DayEntity
import com.mesada.app.data.db.HiddenFoodEntity
import com.mesada.app.data.db.EntryEntity
import com.mesada.app.data.db.GoalsEntity
import com.mesada.app.data.db.MesadaDao
import com.mesada.app.data.db.ProfileEntity
import com.mesada.app.domain.GoalsCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

data class DayState(
    val date: String,
    val entries: List<EntryEntity>,
    val steps: Int,
    val goals: GoalsEntity,
) {
    val totals: Macros get() = entries.fold(Macros()) { acc, e -> acc + e.macros }
    fun meal(m: Meal) = entries.filter { it.meal == m.key }
    val hasProduce: Boolean get() = entries.any { it.isProduce }
    val remainingKcal: Double get() = goals.kcal - totals.kcal
    val remainingProtein: Double get() = goals.protein - totals.protein

    companion object {
        fun empty() = DayState(LocalDate.now().toString(), emptyList(), 0, GoalsEntity())
    }
}

val EntryEntity.food: Food? get() = foodId?.let { FoodCatalog.byId[it] }
val EntryEntity.displayName: String get() = customName ?: food?.name ?: "Alimento"
val EntryEntity.emoji: String get() = customEmoji ?: food?.emoji ?: "🍽️"
val EntryEntity.qtyLabel: String get() = customPortion ?: food?.formatQty(quantity) ?: ""
val EntryEntity.macros: Macros get() = Macros(kcal, protein, carbs, fat)

fun CustomFoodEntity.toFood() = Food(
    id = id, name = name, emoji = "", category = Category.valueOf(category),
    measure = Measure.valueOf(measure), per100 = Macros(kcal, protein, carbs, fat),
    gramsPerPiece = gramsPerPiece, unitSingular = unitSingular, unitPlural = unitPlural,
)

/** Un día pasado con sus totales, pasos y detalle, para el historial. */
data class DaySummary(
    val date: String,
    val totals: Macros,
    val steps: Int,
    val entries: List<EntryEntity>,
) {
    fun meal(m: Meal) = entries.filter { it.meal == m.key }
    val burnedKcal: Int get() = (steps * 0.04).roundToInt()
}

class MesadaRepository(private val dao: MesadaDao) {

    fun today(): String = LocalDate.now().toString()

    fun observeDay(date: String = today()): Flow<DayState> =
        combine(dao.entriesFor(date), dao.day(date), dao.goals()) { entries, day, goals ->
            DayState(date, entries, day?.steps ?: 0, goals ?: GoalsEntity())
        }

    suspend fun snapshot(date: String = today()) = DayState(
        date, dao.entriesOnce(date), dao.dayOnce(date)?.steps ?: 0, dao.goalsOnce() ?: GoalsEntity()
    )

    suspend fun addFood(meal: Meal, food: Food, qty: Double): Macros {
        val m = food.macros(qty)
        dao.insert(
            EntryEntity(
                date = today(), meal = meal.key, foodId = food.id, quantity = qty,
                customName = food.name, customPortion = food.formatQty(qty),
                kcal = m.kcal, protein = m.protein, carbs = m.carbs, fat = m.fat,
                isProduce = food.category == Category.PRODUCE,
            )
        )
        return m
    }

    // --- Catálogo (fijo + personalizado, con ediciones y borrados de los fijos) ---

    /** Combina catálogo fijo, ediciones (override por id) y ocultos, más los alimentos propios. */
    private fun merge(customs: List<CustomFoodEntity>, hidden: List<HiddenFoodEntity>): List<Food> {
        val overrides = customs.associateBy { it.id }
        val hiddenIds = hidden.mapTo(HashSet()) { it.id }
        val base = FoodCatalog.all.filter { it.id !in hiddenIds }.map { overrides[it.id]?.toFood() ?: it }
        val pureCustom = customs.filter { it.id !in FoodCatalog.byId }.map { it.toFood() }
        return base + pureCustom
    }

    fun observeFoods(): Flow<List<Food>> =
        combine(dao.customFoods(), dao.hiddenFoods()) { customs, hidden -> merge(customs, hidden) }

    suspend fun foodMap(): Map<String, Food> =
        merge(dao.customFoodsOnce(), dao.hiddenFoodsOnce()).associateBy { it.id }

    /** Guarda un alimento con el id dado (genera uno nuevo si es null): sirve para crear y editar. */
    suspend fun saveFood(
        id: String?, name: String, category: Category, measure: Measure, per100: Macros,
        gramsPerPiece: Double, unitSingular: String, unitPlural: String,
    ) {
        dao.upsertCustomFood(
            CustomFoodEntity(
                id = id ?: "cf_${UUID.randomUUID()}",
                name = name, category = category.name, measure = measure.name,
                kcal = per100.kcal, protein = per100.protein, carbs = per100.carbs, fat = per100.fat,
                gramsPerPiece = gramsPerPiece, unitSingular = unitSingular, unitPlural = unitPlural,
            )
        )
    }

    /** Elimina un alimento: si es del catálogo fijo lo oculta; si es propio lo borra. */
    suspend fun deleteFood(food: Food) {
        dao.deleteCustomFood(food.id) // quita override propio si existía
        if (FoodCatalog.byId.containsKey(food.id)) dao.hideFood(HiddenFoodEntity(food.id))
    }

    // --- Recetas personalizadas ---

    fun observeCustomRecipes(): Flow<List<CustomRecipeEntity>> = dao.customRecipes()

    suspend fun createRecipe(name: String, items: List<Pair<String, Double>>) {
        val encoded = items.joinToString(";") { (id, q) -> "$id:$q" }
        dao.upsertCustomRecipe(CustomRecipeEntity("cr_${UUID.randomUUID()}", name, encoded))
    }

    suspend fun deleteRecipe(id: String) = dao.deleteCustomRecipe(id)

    /** Agrega todos los ingredientes de una receta a una comida (ignora ids que ya no existan). */
    suspend fun addRecipeItems(items: List<Pair<String, Double>>, meal: Meal = Meal.DINNER) {
        val map = foodMap()
        items.forEach { (id, q) -> map[id]?.let { addFood(meal, it, q) } }
    }

    // --- Historial ---

    fun observeHistory(): Flow<List<DaySummary>> =
        combine(dao.allEntries(), dao.allDays()) { entries, days ->
            val byDate = entries.groupBy { it.date }
            val stepsByDate = days.associate { it.date to it.steps }
            (byDate.keys + stepsByDate.keys).toSortedSet(compareByDescending { it }).map { d ->
                val es = byDate[d].orEmpty()
                DaySummary(d, es.fold(Macros()) { a, e -> a + e.macros }, stepsByDate[d] ?: 0, es)
            }
        }

    suspend fun addCustom(meal: Meal, name: String, emoji: String?, portion: String, m: Macros, produce: Boolean) {
        dao.insert(
            EntryEntity(
                date = today(), meal = meal.key, foodId = null, quantity = 1.0,
                customName = name, customEmoji = emoji, customPortion = portion,
                kcal = m.kcal, protein = m.protein, carbs = m.carbs, fat = m.fat, isProduce = produce,
            )
        )
    }

    suspend fun remove(id: Long) = dao.delete(id)

    /** Quita el último registro de esa comida cuyo nombre contenga [query]. Devuelve el nombre quitado. */
    suspend fun removeByName(meal: Meal, query: String): String? {
        val q = query.trim().lowercase()
        if (q.isBlank()) return null
        val match = dao.entriesOnce(today()).filter { it.meal == meal.key }
            .lastOrNull { it.displayName.lowercase().contains(q) || it.foodId == q } ?: return null
        dao.delete(match.id)
        return match.displayName
    }

    suspend fun changeSteps(delta: Int? = null, absolute: Int? = null): Int {
        val current = dao.dayOnce(today())?.steps ?: 0
        val next = (absolute ?: (current + (delta ?: 0))).coerceAtLeast(0)
        dao.upsertDay(DayEntity(today(), next))
        return next
    }

    suspend fun updateGoals(transform: (GoalsEntity) -> GoalsEntity) {
        dao.upsertGoals(transform(dao.goalsOnce() ?: GoalsEntity()))
    }

    fun observeProfile(): Flow<ProfileEntity?> = dao.profile()

    /** Guarda el perfil y recalcula+guarda los objetivos a partir de él en el mismo paso. */
    suspend fun saveProfile(profile: ProfileEntity) {
        dao.upsertProfile(profile)
        dao.upsertGoals(GoalsCalculator.compute(profile))
    }

    suspend fun clearToday() {
        dao.clearDay(today())
        dao.upsertDay(DayEntity(today(), 0))
    }
}
