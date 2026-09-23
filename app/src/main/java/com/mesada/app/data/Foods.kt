package com.mesada.app.data

import java.time.LocalTime
import java.util.Locale

enum class Measure { GRAM, ML, PIECE }

enum class Category(val label: String) {
    PROTEIN("Proteínas"), CARBS("Hidratos"), DAIRY("Lácteos"), PRODUCE("Frutas y verduras"), OTHER("Otros")
}

enum class Meal(val key: String, val label: String) {
    BREAKFAST("desayuno", "Desayuno"),
    LUNCH("almuerzo", "Almuerzo"),
    SNACK("merienda", "Merienda"),
    DINNER("cena", "Cena");

    companion object {
        fun fromKey(key: String?): Meal? = entries.firstOrNull { it.key == key }
        fun forNow(hour: Int = LocalTime.now().hour): Meal = when {
            hour < 11 -> BREAKFAST
            hour < 15 -> LUNCH
            hour < 19 -> SNACK
            else -> DINNER
        }
    }
}

data class Macros(
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
) {
    operator fun plus(o: Macros) = Macros(kcal + o.kcal, protein + o.protein, carbs + o.carbs, fat + o.fat)
}

/** Valores nutricionales cada 100 g / 100 ml. Para PIECE, gramsPerPiece convierte unidades a gramos. */
data class Food(
    val id: String,
    val name: String,
    val emoji: String,
    val category: Category,
    val measure: Measure,
    val per100: Macros,
    val gramsPerPiece: Double = 0.0,
    val unitSingular: String = "",
    val unitPlural: String = "",
) {
    fun grams(qty: Double) = if (measure == Measure.PIECE) qty * gramsPerPiece else qty

    fun macros(qty: Double): Macros {
        val f = grams(qty) / 100.0
        return Macros(per100.kcal * f, per100.protein * f, per100.carbs * f, per100.fat * f)
    }

    fun formatQty(qty: Double): String = when (measure) {
        Measure.PIECE -> "${qty.pretty()} ${if (qty <= 1.0) unitSingular else unitPlural}"
        Measure.GRAM -> "${qty.pretty()} g"
        Measure.ML -> "${qty.pretty()} ml"
    }

    val step: Double get() = if (measure == Measure.PIECE) 0.5 else 10.0
    val defaultQty: Double get() = when (measure) { Measure.PIECE -> 1.0; Measure.ML -> 250.0; Measure.GRAM -> 100.0 }
    val presets: List<Double> get() = when (measure) {
        Measure.PIECE -> listOf(0.5, 1.0, 2.0, 3.0)
        Measure.ML -> listOf(100.0, 200.0, 250.0, 300.0)
        Measure.GRAM -> listOf(50.0, 100.0, 150.0, 200.0)
    }
    val unitHint: String get() = when (measure) {
        Measure.PIECE -> "1 $unitSingular ≈ ${gramsPerPiece.pretty()} g"
        Measure.GRAM -> "Cada 100 g: ${per100.kcal.pretty()} kcal"
        Measure.ML -> "Cada 100 ml: ${per100.kcal.pretty()} kcal"
    }
}

private val AR = Locale("es", "AR")
fun Double.pretty(): String = if (this % 1.0 == 0.0) toLong().toString() else String.format(AR, "%.1f", this)

private fun g(id: String, name: String, emoji: String, cat: Category, k: Double, p: Double, c: Double, f: Double) =
    Food(id, name, emoji, cat, Measure.GRAM, Macros(k, p, c, f))

private fun pc(id: String, name: String, emoji: String, cat: Category, per: Double, s: String, pl: String,
               k: Double, p: Double, c: Double, f: Double) =
    Food(id, name, emoji, cat, Measure.PIECE, Macros(k, p, c, f), per, s, pl)

object FoodCatalog {
    val all: List<Food> = listOf(
        pc("huevo", "Huevo", "🥚", Category.PROTEIN, 50.0, "unidad", "unidades", 143.0, 12.6, 0.7, 9.5),
        g("pollo", "Pechuga de pollo", "🍗", Category.PROTEIN, 165.0, 31.0, 0.0, 3.6),
        g("carne", "Carne magra", "🥩", Category.PROTEIN, 190.0, 29.0, 0.0, 8.0),
        g("salmon", "Salmón", "🐟", Category.PROTEIN, 208.0, 20.0, 0.0, 13.0),
        g("atun", "Atún al natural", "🥫", Category.PROTEIN, 116.0, 26.0, 0.0, 1.0),
        pc("jamon", "Jamón cocido", "🥓", Category.PROTEIN, 20.0, "feta", "fetas", 110.0, 17.0, 2.0, 4.0),
        pc("queso", "Queso light", "🧀", Category.DAIRY, 20.0, "feta", "fetas", 250.0, 25.0, 2.0, 15.0),
        g("yogur", "Yogur griego", "🥛", Category.DAIRY, 100.0, 8.0, 4.0, 5.0),
        Food("lecheprot", "Leche proteica", "☕", Category.DAIRY, Measure.ML, Macros(60.0, 6.5, 5.0, 1.5)),
        pc("bagel", "Bagel de semillas", "🥯", Category.CARBS, 100.0, "unidad", "unidades", 270.0, 10.0, 50.0, 3.5),
        pc("pan", "Pan integral", "🍞", Category.CARBS, 30.0, "rebanada", "rebanadas", 250.0, 9.0, 43.0, 3.4),
        g("papa", "Papa", "🥔", Category.CARBS, 87.0, 1.9, 20.0, 0.1),
        g("arroz", "Arroz cocido", "🍚", Category.CARBS, 130.0, 2.7, 28.0, 0.3),
        g("fideos", "Fideos cocidos", "🍝", Category.CARBS, 158.0, 5.8, 31.0, 0.9),
        g("avena", "Avena", "🌾", Category.CARBS, 380.0, 13.0, 67.0, 7.0),
        g("granola", "Granola", "🥣", Category.CARBS, 450.0, 10.0, 64.0, 17.0),
        g("copos", "Copos de maíz", "🌽", Category.CARBS, 370.0, 7.0, 84.0, 0.9),
        pc("banana", "Banana", "🍌", Category.PRODUCE, 120.0, "unidad", "unidades", 89.0, 1.1, 23.0, 0.3),
        pc("manzana", "Manzana", "🍎", Category.PRODUCE, 180.0, "unidad", "unidades", 52.0, 0.3, 14.0, 0.2),
        g("brocoli", "Brócoli", "🥦", Category.PRODUCE, 35.0, 2.4, 7.0, 0.4),
        g("ensalada", "Ensalada mixta", "🥗", Category.PRODUCE, 20.0, 1.5, 3.5, 0.2),
        g("tomate", "Tomate", "🍅", Category.PRODUCE, 18.0, 0.9, 3.9, 0.2),
        g("palta", "Palta", "🥑", Category.PRODUCE, 160.0, 2.0, 9.0, 15.0),
        pc("aceite", "Aceite de oliva", "🫒", Category.OTHER, 13.0, "cucharada", "cucharadas", 884.0, 0.0, 0.0, 100.0),
        // Valores estimados a partir de recetas y tablas típicas — igual que el resto del
        // catálogo, conviene contrastarlos con una fuente nutricional oficial antes de confiar
        // en ellos para un uso más allá de una estimación aproximada.
        pc("empanada", "Empanada de carne (horno)", "🥟", Category.CARBS, 90.0, "unidad", "unidades", 245.0, 9.0, 24.0, 12.0),
        pc("milanesa", "Milanesa de carne", "🍖", Category.PROTEIN, 150.0, "unidad", "unidades", 213.0, 18.7, 10.0, 10.7),
        g("dulcedeleche", "Dulce de leche", "🍯", Category.OTHER, 315.0, 6.5, 55.0, 7.0),
        pc("facturas", "Factura (medialuna)", "🥐", Category.CARBS, 40.0, "unidad", "unidades", 350.0, 7.5, 40.0, 17.5),
    )
    val byId: Map<String, Food> = all.associateBy { it.id }
}
