package com.mesada.app.assistant

import com.mesada.app.data.FoodCatalog
import com.mesada.app.data.Macros
import com.mesada.app.data.Meal
import com.mesada.app.data.MesadaRepository
import com.mesada.app.data.displayName
import com.mesada.app.data.macros
import com.mesada.app.data.qtyLabel
import com.mesada.app.domain.KitchenTimer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.math.roundToInt

/** Resultado de una herramienta: lo que vuelve a Claude + una línea opcional para mostrar en pantalla. */
data class ToolOutcome(val result: JsonObject, val action: String? = null, val isError: Boolean = false)

class AssistantTools(
    private val repo: MesadaRepository,
    private val timer: KitchenTimer,
) {
    private val mealKeys = Meal.entries.map { it.key }

    val definitions: JsonArray = buildJsonArray {
        add(tool("agregar_alimento", "Registra un alimento del catálogo en una comida del día.",
            required = listOf("comida", "alimento_id", "cantidad")) {
            enumProp("comida", mealKeys)
            enumProp("alimento_id", FoodCatalog.all.map { it.id })
            numProp("cantidad", "En gramos/ml o en unidades, según indica el catálogo")
        })
        add(tool("agregar_personalizado",
            "Registra un alimento que NO está en el catálogo, con valores estimados para la porción indicada.",
            required = listOf("comida", "nombre", "porcion", "kcal", "proteina", "hidratos", "grasas")) {
            enumProp("comida", mealKeys)
            strProp("nombre"); strProp("emoji", "Un emoji representativo")
            strProp("porcion", "Texto corto, por ejemplo \"1 empanada\" o \"200 g\"")
            numProp("kcal"); numProp("proteina"); numProp("hidratos"); numProp("grasas")
            putJsonObject("es_fruta_o_verdura") { put("type", "boolean") }
        })
        add(tool("quitar_alimento", "Quita el último registro que coincida con el nombre en una comida.",
            required = listOf("comida", "nombre")) {
            enumProp("comida", mealKeys); strProp("nombre")
        })
        add(tool("resumen_del_dia", "Devuelve lo consumido, los objetivos, lo que falta y el detalle por comida.",
            required = emptyList()) {})
        add(tool("iniciar_temporizador", "Inicia el temporizador de cocina.", required = listOf("minutos")) {
            numProp("minutos")
        })
        add(tool("cambiar_pasos", "Suma o fija los pasos del día.", required = listOf("pasos", "modo")) {
            numProp("pasos"); enumProp("modo", listOf("sumar", "fijar"))
        })
    }

    suspend fun execute(name: String, input: JsonObject): ToolOutcome {
        fun s(k: String) = input[k]?.jsonPrimitive?.contentOrNull
        fun d(k: String) = input[k]?.jsonPrimitive?.doubleOrNull

        return when (name) {
            "agregar_alimento" -> {
                val meal = Meal.fromKey(s("comida")) ?: return error("Comida inválida")
                val food = FoodCatalog.byId[s("alimento_id")] ?: return error("Alimento fuera del catálogo")
                val qty = d("cantidad")?.takeIf { it > 0 } ?: food.defaultQty
                val m = repo.addFood(meal, food, qty)
                ToolOutcome(ok { put("kcal", m.kcal.roundToInt()); put("proteina", m.protein.roundToInt()) },
                    "✓ ${food.name} · ${food.formatQty(qty)} → ${meal.label.lowercase()}")
            }
            "agregar_personalizado" -> {
                val meal = Meal.fromKey(s("comida")) ?: return error("Comida inválida")
                val nombre = s("nombre") ?: return error("Falta el nombre")
                val porcion = s("porcion") ?: "1 porción"
                val m = Macros(d("kcal") ?: 0.0, d("proteina") ?: 0.0, d("hidratos") ?: 0.0, d("grasas") ?: 0.0)
                repo.addCustom(meal, nombre, s("emoji"), porcion, m,
                    input["es_fruta_o_verdura"]?.jsonPrimitive?.booleanOrNull ?: false)
                ToolOutcome(ok {}, "✓ $nombre · $porcion → ${meal.label.lowercase()} (estimado)")
            }
            "quitar_alimento" -> {
                val meal = Meal.fromKey(s("comida")) ?: return error("Comida inválida")
                val removed = repo.removeByName(meal, s("nombre").orEmpty())
                    ?: return error("No encontré ese alimento en esa comida")
                ToolOutcome(ok {}, "✓ Quitado: $removed de ${meal.label.lowercase()}")
            }
            "resumen_del_dia" -> ToolOutcome(summary())
            "iniciar_temporizador" -> {
                val min = d("minutos")?.takeIf { it > 0 } ?: return error("Minutos inválidos")
                timer.set(min, autoStart = true)
                ToolOutcome(ok {}, "✓ Temporizador: ${timer.state.value.label}")
            }
            "cambiar_pasos" -> {
                val pasos = d("pasos")?.roundToInt() ?: return error("Pasos inválidos")
                val total = if (s("modo") == "fijar") repo.changeSteps(absolute = pasos) else repo.changeSteps(delta = pasos)
                ToolOutcome(ok { put("pasos", total) }, "✓ Pasos: $total")
            }
            else -> error("Herramienta desconocida: $name")
        }
    }

    suspend fun summary(): JsonObject {
        val day = repo.snapshot()
        val t = day.totals
        return buildJsonObject {
            putJsonObject("consumido") {
                put("kcal", t.kcal.roundToInt()); put("proteina", t.protein.roundToInt())
                put("hidratos", t.carbs.roundToInt()); put("grasas", t.fat.roundToInt())
            }
            putJsonObject("objetivos") {
                put("kcal", day.goals.kcal); put("proteina", day.goals.protein)
                put("hidratos", day.goals.carbs); put("grasas", day.goals.fat)
            }
            putJsonObject("restante") {
                put("kcal", day.remainingKcal.roundToInt()); put("proteina", day.remainingProtein.roundToInt())
            }
            put("pasos", day.steps)
            put("comio_frutas_o_verduras", day.hasProduce)
            putJsonObject("comidas") {
                Meal.entries.forEach { m ->
                    putJsonArray(m.key) {
                        day.meal(m).forEach { add(kotlinx.serialization.json.JsonPrimitive(
                            "${it.displayName} (${it.qtyLabel}, ${it.macros.kcal.roundToInt()} kcal)")) }
                    }
                }
            }
        }
    }

    private fun ok(extra: JsonObjectBuilder.() -> Unit) = buildJsonObject { put("ok", true); extra() }
    private fun error(msg: String) = ToolOutcome(buildJsonObject { put("ok", false); put("error", msg) }, isError = true)

    private fun tool(name: String, description: String, required: List<String>, props: JsonObjectBuilder.() -> Unit) =
        buildJsonObject {
            put("name", name)
            put("description", description)
            putJsonObject("input_schema") {
                put("type", "object")
                putJsonObject("properties", props)
                putJsonArray("required") { required.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }
            }
        }

    private fun JsonObjectBuilder.enumProp(name: String, values: List<String>) = putJsonObject(name) {
        put("type", "string")
        putJsonArray("enum") { values.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }
    }
    private fun JsonObjectBuilder.numProp(name: String, desc: String? = null) = putJsonObject(name) {
        put("type", "number"); desc?.let { put("description", it) }
    }
    private fun JsonObjectBuilder.strProp(name: String, desc: String? = null) = putJsonObject(name) {
        put("type", "string"); desc?.let { put("description", it) }
    }
}
