package com.mesada.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.mesada.app.data.Meal
import com.mesada.app.ui.theme.Palette

/** Identidad visual de cada comida, compartida entre Hoy y Evolución. */
fun mealIcon(meal: Meal): ImageVector = when (meal) {
    Meal.BREAKFAST -> Icons.Filled.WbSunny
    Meal.LUNCH -> Icons.Filled.LunchDining
    Meal.SNACK -> Icons.Filled.Cookie
    Meal.DINNER -> Icons.Filled.DinnerDining
}

fun mealColor(meal: Meal): Color = when (meal) {
    Meal.BREAKFAST -> Palette.Breakfast
    Meal.LUNCH -> Palette.Lunch
    Meal.SNACK -> Palette.Snack
    Meal.DINNER -> Palette.Dinner
}
