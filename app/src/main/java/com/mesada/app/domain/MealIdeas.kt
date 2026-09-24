package com.mesada.app.domain

import com.mesada.app.data.Food
import com.mesada.app.data.FoodCatalog
import com.mesada.app.data.Macros
import kotlin.math.abs
import kotlin.math.max

data class MealIdea(val name: String, val emoji: String, val items: List<Pair<String, Double>>) {
    val macros: Macros get() = items.fold(Macros()) { acc, (id, q) -> acc + FoodCatalog.byId.getValue(id).macros(q) }
}

/** Receta lista para mostrar: unifica las ideas fijas y las creadas por la persona. */
data class RecipeUi(
    val id: String,
    val name: String,
    val items: List<Pair<String, Double>>,
    val macros: Macros,
    val custom: Boolean,
)

/** Decodifica "foodId:cantidad;foodId:cantidad" ignorando tramos inválidos. */
fun parseRecipeItems(s: String): List<Pair<String, Double>> =
    s.split(";").mapNotNull { part ->
        val bits = part.split(":")
        val id = bits.getOrNull(0)?.trim().orEmpty()
        val qty = bits.getOrNull(1)?.trim()?.toDoubleOrNull()
        if (id.isBlank() || qty == null) null else id to qty
    }

fun macrosOf(items: List<Pair<String, Double>>, foods: Map<String, Food>): Macros =
    items.fold(Macros()) { acc, (id, q) -> acc + (foods[id]?.macros(q) ?: Macros()) }

/** Ordena por cercanía a las kcal restantes, penalizando si no cubre la proteína que falta. */
fun suggestRecipes(recipes: List<RecipeUi>, remainingKcal: Double, remainingProtein: Double, count: Int = 3): List<RecipeUi> {
    val target = max(remainingKcal, 300.0)
    return recipes.sortedBy { r ->
        abs(r.macros.kcal - target) / target + max(0.0, remainingProtein - r.macros.protein) / max(remainingProtein, 1.0) * 0.6
    }.take(count)
}

object MealIdeas {
    val all = listOf(
        MealIdea("Salmón con brócoli y arroz", "🐟", listOf("salmon" to 150.0, "brocoli" to 150.0, "arroz" to 120.0)),
        MealIdea("Lomo con ensalada y palta", "🥩", listOf("carne" to 150.0, "ensalada" to 150.0, "palta" to 50.0)),
        MealIdea("Pollo con verduras y papa", "🍗", listOf("pollo" to 150.0, "brocoli" to 150.0, "papa" to 100.0)),
        MealIdea("Fideos con atún y tomate", "🍝", listOf("fideos" to 200.0, "atun" to 120.0, "tomate" to 100.0)),
        MealIdea("Omelette con ensalada", "🍳", listOf("huevo" to 3.0, "ensalada" to 150.0, "pan" to 1.0)),
    )

    /** Ordena por cercanía a las kcal restantes, penalizando si no cubre la proteína que falta. */
    fun suggest(remainingKcal: Double, remainingProtein: Double, count: Int = 3): List<MealIdea> {
        val target = max(remainingKcal, 300.0)
        return all.sortedBy { idea ->
            val m = idea.macros
            abs(m.kcal - target) / target + max(0.0, remainingProtein - m.protein) / max(remainingProtein, 1.0) * 0.6
        }.take(count)
    }
}
