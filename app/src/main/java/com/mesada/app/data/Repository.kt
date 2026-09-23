package com.mesada.app.data

import com.mesada.app.data.db.DayEntity
import com.mesada.app.data.db.EntryEntity
import com.mesada.app.data.db.GoalsEntity
import com.mesada.app.data.db.MesadaDao
import com.mesada.app.data.db.ProfileEntity
import com.mesada.app.domain.GoalsCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

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
                kcal = m.kcal, protein = m.protein, carbs = m.carbs, fat = m.fat,
                isProduce = food.category == Category.PRODUCE,
            )
        )
        return m
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
