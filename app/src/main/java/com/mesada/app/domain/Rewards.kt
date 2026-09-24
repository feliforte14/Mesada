package com.mesada.app.domain

import com.mesada.app.data.DayState
import com.mesada.app.data.DaySummary
import com.mesada.app.data.Meal
import java.time.LocalDate
import kotlin.math.roundToInt

enum class RewardScope(val label: String, val eyebrow: String) {
    DAILY("Objetivos de hoy", "Diarios"),
    WEEKLY("Esta semana", "Últimos 7 días"),
    MONTHLY("Este mes", "Últimos 30 días"),
}

data class Reward(val title: String, val detail: String, val current: Int, val target: Int) {
    val done: Boolean get() = current >= target
    val progress: Float get() = (current.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
}

data class RewardGroup(val scope: RewardScope, val rewards: List<Reward>)

object Rewards {
    const val STEP_GOAL = 10_000

    /** Se considera cumplida la meta de calorías si comió cerca del objetivo (ni muy poco ni de más). */
    private fun kcalReached(kcal: Double, goal: Int) = goal > 0 && kcal >= goal * 0.85 && kcal <= goal * 1.10

    fun compute(day: DayState, history: List<DaySummary>): List<RewardGroup> {
        val goals = day.goals
        val today = LocalDate.now()
        fun lastN(n: Int) = history.filter {
            runCatching { !LocalDate.parse(it.date).isBefore(today.minusDays((n - 1).toLong())) }.getOrDefault(false)
        }
        val week = lastN(7)
        val month = lastN(30)
        val mealsLogged = Meal.entries.count { day.meal(it).isNotEmpty() }

        val daily = listOf(
            Reward("Proteína del día", "Alcanzá tu objetivo de proteína", day.totals.protein.roundToInt(), goals.protein),
            Reward("Meta de pasos", "Movete ${STEP_GOAL} pasos hoy", day.steps, STEP_GOAL),
            Reward("Todas las comidas", "Registrá desayuno, almuerzo, merienda y cena", mealsLogged, 4),
            Reward("Frutas y verduras", "Sumá al menos una porción hoy", if (day.hasProduce) 1 else 0, 1),
        )
        val weekly = listOf(
            Reward("Semana en objetivo", "5 días dentro de tus calorías", week.count { kcalReached(it.totals.kcal, goals.kcal) }, 5),
            Reward("Semana activa", "5 días llegando a los pasos", week.count { it.steps >= STEP_GOAL }, 5),
            Reward("Constancia", "Registrá comida los 7 días", week.count { it.totals.kcal > 0 }, 7),
        )
        val monthly = listOf(
            Reward("Mes constante", "20 días con registro", month.count { it.totals.kcal > 0 || it.steps > 0 }, 20),
            Reward("Mes activo", "15 días llegando a los pasos", month.count { it.steps >= STEP_GOAL }, 15),
            Reward("Proteína del mes", "20 días alcanzando la proteína", month.count { it.totals.protein.roundToInt() >= goals.protein }, 20),
        )
        return listOf(
            RewardGroup(RewardScope.DAILY, daily),
            RewardGroup(RewardScope.WEEKLY, weekly),
            RewardGroup(RewardScope.MONTHLY, monthly),
        )
    }
}
