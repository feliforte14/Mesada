package com.mesada.app.assistant

import com.mesada.app.data.FoodCatalog
import com.mesada.app.data.Measure
import com.mesada.app.data.Meal
import com.mesada.app.data.pretty
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Bucle de tool use: manda el pedido, ejecuta las herramientas que Claude pida,
 * devuelve los resultados y repite hasta obtener la respuesta final en texto.
 */
class Assistant(
    private val client: ClaudeClient,
    private val tools: AssistantTools,
) {
    private val history = mutableListOf<JsonObject>() // solo turnos de texto, en pares usuario/asistente

    suspend fun handle(userText: String, onAction: (String) -> Unit): String {
        val system = systemPrompt(tools.summary().toString())
        val messages = (history.takeLast(8) + textTurn("user", userText)).toMutableList()

        repeat(MAX_ROUNDS) {
            val response = client.createMessage(system, JsonArray(messages), tools.definitions)
            val content = response["content"]?.jsonArray ?: JsonArray(emptyList())
            messages += buildJsonObject { put("role", "assistant"); put("content", content) }

            val stop = response["stop_reason"]?.jsonPrimitive?.contentOrNull
            if (stop != "tool_use") {
                val answer = content.map { it.jsonObject }
                    .filter { it["type"]?.jsonPrimitive?.contentOrNull == "text" }
                    .joinToString("") { it["text"]?.jsonPrimitive?.contentOrNull.orEmpty() }
                    .trim().ifBlank { "Listo." }
                history += textTurn("user", userText)
                history += textTurn("assistant", answer)
                // La app corre indefinidamente en la tablet de cocina (kiosco) — sin este
                // recorte, history crecería sin límite durante días de uso continuo.
                if (history.size > HISTORY_LIMIT) history.subList(0, history.size - HISTORY_LIMIT).clear()
                return answer
            }

            val results = buildJsonArray {
                content.map { it.jsonObject }
                    .filter { it["type"]?.jsonPrimitive?.contentOrNull == "tool_use" }
                    .forEach { block ->
                        val name = block["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        val input = block["input"]?.jsonObject ?: JsonObject(emptyMap())
                        val outcome = runCatching { tools.execute(name, input) }.getOrElse {
                            ToolOutcome(buildJsonObject { put("ok", false); put("error", it.message ?: "error") }, isError = true)
                        }
                        outcome.action?.let(onAction)
                        addJsonObject {
                            put("type", "tool_result")
                            put("tool_use_id", block["id"]?.jsonPrimitive?.contentOrNull.orEmpty())
                            put("content", outcome.result.toString())
                            if (outcome.isError) put("is_error", true)
                        }
                    }
            }
            messages += buildJsonObject { put("role", "user"); put("content", results) }
        }
        return "Hice parte del pedido pero no pude terminarlo. Probá de nuevo."
    }

    fun reset() = history.clear()

    private fun textTurn(role: String, text: String) = buildJsonObject { put("role", role); put("content", text) }

    private fun systemPrompt(dayJson: String): String {
        val catalog = FoodCatalog.all.joinToString("\n") { f ->
            val unit = when (f.measure) {
                Measure.PIECE -> "cantidad en ${f.unitPlural} (1 ${f.unitSingular} ≈ ${f.gramsPerPiece.pretty()} g)"
                Measure.GRAM -> "cantidad en g"
                Measure.ML -> "cantidad en ml"
            }
            "${f.id}: ${f.name} — $unit"
        }
        return """
            Sos el asistente de voz de "Mesada", una app de nutrición en una pantalla táctil de cocina.
            Hablás en español rioplatense, cálido y muy breve: tus respuestas se leen en voz alta, así que usá
            1 o 2 oraciones, sin listas, sin markdown y sin emojis.
            Usá las herramientas para actuar. Si el usuario dice qué comió, registralo sin pedir confirmación.
            Si no dice en qué comida, usá "${Meal.forNow().key}". Si no dice cantidad, asumí una porción típica argentina.
            Preferí el catálogo; si el alimento no está, usá agregar_personalizado con valores estimados razonables.
            Después de registrar, confirmá qué agregaste y, si suma, cuánto le queda de kcal o proteína.
            No des consejos médicos ni planes de dieta estrictos; ante temas de salud, sugerí consultar a un profesional.

            Catálogo (id: nombre — unidad):
            $catalog

            Estado actual del día (JSON):
            $dayJson
        """.trimIndent()
    }

    private companion object {
        const val MAX_ROUNDS = 6
        const val HISTORY_LIMIT = 16 // se manda takeLast(8); guardamos margen sin dejarla crecer sin fin
    }
}
