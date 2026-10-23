package com.calorielens.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.Transaction
import com.calorielens.app.domain.FoodItem
import com.calorielens.app.domain.Meal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

@Entity(tableName = "meals")
data class MealEntity(@PrimaryKey val id: String, val timestamp: Long, val title: String, val imagePath: String?)

@Entity(tableName = "meal_items")
data class MealItemEntity(
    @PrimaryKey val id: String,
    val mealId: String,
    val name: String,
    val grams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val fiberPer100g: Double,
    val confidence: Float
)

data class MealWithItems(
    @androidx.room.Embedded val meal: MealEntity,
    @Relation(parentColumn = "id", entityColumn = "mealId") val items: List<MealItemEntity>
)

@Dao
interface MealDao {
    @Transaction
    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    fun observeMeals(): Flow<List<MealWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<MealItemEntity>)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMeal(id: String)

    @Query("DELETE FROM meal_items WHERE mealId = :id")
    suspend fun deleteItems(id: String)
}

@Database(entities = [MealEntity::class, MealItemEntity::class], version = 1, exportSchema = false)
abstract class CalorieDatabase : androidx.room.RoomDatabase() {
    abstract fun mealDao(): MealDao
    companion object {
        fun create(context: Context): CalorieDatabase = Room.databaseBuilder(context, CalorieDatabase::class.java, "calorie-lens.db").build()
    }
}

class PersistentMealStore(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao = CalorieDatabase.create(context.applicationContext).mealDao()
    val meals: Flow<List<Meal>> = dao.observeMeals().map { rows -> rows.map { row ->
        Meal(row.meal.id, row.meal.timestamp, row.meal.title, row.items.map { it.toDomain() }, row.meal.imagePath)
    } }

    fun addMeal(items: List<FoodItem>, title: String = "午餐", imagePath: String? = null) {
        val mealId = UUID.randomUUID().toString()
        scope.launch {
            dao.insertMeal(MealEntity(mealId, System.currentTimeMillis(), title, imagePath))
            dao.insertItems(items.map { it.toEntity(mealId) })
        }
    }

    fun deleteMeal(id: String) { scope.launch { dao.deleteItems(id); dao.deleteMeal(id) } }
}

private fun FoodItem.toEntity(mealId: String) = MealItemEntity(id, mealId, name, grams, caloriesPer100g, proteinPer100g, fatPer100g, carbsPer100g, fiberPer100g, confidence)
private fun MealItemEntity.toDomain() = FoodItem(id, name, grams, caloriesPer100g, proteinPer100g, fatPer100g, carbsPer100g, fiberPer100g, confidence)
