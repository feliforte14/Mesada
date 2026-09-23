package com.mesada.app.domain

import com.mesada.app.data.db.GoalsEntity
import com.mesada.app.data.db.ProfileEntity
import kotlin.math.roundToInt

enum class Sex(val key: String, val label: String) { MALE("M", "Varón"), FEMALE("F", "Mujer") }

enum class ActivityLevel(val key: String, val label: String, val factor: Double) {
    SEDENTARY("sedentary", "Sedentario (oficina, poco movimiento)", 1.2),
    LIGHT("light", "Actividad ligera (1-3 días/semana)", 1.375),
    MODERATE("moderate", "Actividad moderada (3-5 días/semana)", 1.55),
    ACTIVE("active", "Actividad intensa (6-7 días/semana)", 1.725),
    VERY_ACTIVE("veryActive", "Muy intensa (entreno 2x/día o trabajo físico)", 1.9);

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: MODERATE
    }
}

enum class WeightGoal(val key: String, val label: String, val kcalAdjust: Int) {
    LOSE("lose", "Bajar de peso", -500),
    MAINTAIN("maintain", "Mantener peso", 0),
    GAIN("gain", "Subir de peso", 300);

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: MAINTAIN
    }
}

/**
 * Calcula objetivos diarios a partir de datos de la persona.
 *
 * BMR (metabolismo basal) por Mifflin-St Jeor: más precisa que Harris-Benedict para la
 * población general y la fórmula que hoy recomiendan la mayoría de las guías clínicas.
 *   Varón:  10·peso(kg) + 6.25·altura(cm) − 5·edad + 5
 *   Mujer:  10·peso(kg) + 6.25·altura(cm) − 5·edad − 161
 *
 * TDEE = BMR × factor de actividad. El objetivo de kcal es el TDEE ajustado según si se
 * busca bajar (−500 kcal/día ≈ −0.5 kg/semana), mantener o subir de peso (+300 kcal/día,
 * superávit conservador para minimizar ganancia de grasa).
 *
 * Proteína: 1.8 g/kg si se mantiene o se baja de peso (preserva masa muscular en déficit),
 * 2.0 g/kg si se busca subir (síntesis muscular). Grasas: 25% de las kcal totales. Hidratos:
 * lo que quede del total después de proteína y grasas.
 */
object GoalsCalculator {
    fun compute(profile: ProfileEntity): GoalsEntity {
        val bmr = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age +
            if (Sex.entries.firstOrNull { it.key == profile.sex } == Sex.FEMALE) -161 else 5

        val activity = ActivityLevel.fromKey(profile.activity)
        val goal = WeightGoal.fromKey(profile.goal)
        val tdee = bmr * activity.factor
        val kcal = (tdee + goal.kcalAdjust).coerceAtLeast(1200.0) // piso de seguridad

        val proteinPerKg = if (goal == WeightGoal.GAIN) 2.0 else 1.8
        val protein = profile.weightKg * proteinPerKg
        val fat = kcal * 0.25 / 9.0
        val carbs = ((kcal - protein * 4 - fat * 9) / 4.0).coerceAtLeast(0.0)

        return GoalsEntity(
            kcal = kcal.roundToInt(),
            protein = protein.roundToInt(),
            carbs = carbs.roundToInt(),
            fat = fat.roundToInt(),
        )
    }
}
