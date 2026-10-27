package com.calorielens.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.Transaction
import com.calorielens.app.domain.DailySummary
import com.calorielens.app.domain.FoodItem
import com.calorielens.app.domain.Meal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val title: String,
    val imagePath: String?,
    val rating: Int = 0
)

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

@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey val localDate: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val targetCalories: Int,
    val mealCount: Int
)

data class MealWithItems(
    @Embedded val meal: MealEntity,
    @Relation(parentColumn = "id", entityColumn = "mealId") val items: List<MealItemEntity>
)

data class DayTotals(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val mealCount: Int
)

@Dao
interface MealDao {
    @Transaction
    @Query("SELECT * FROM meals WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC")
    fun observeMealsInRange(start: Long, end: Long): Flow<List<MealWithItems>>

    @Transaction
    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    fun observeAllMeals(): Flow<List<MealWithItems>>

    @Query("SELECT * FROM daily_summaries ORDER BY localDate DESC")
    fun observeDailySummaries(): Flow<List<DailySummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<MealItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: DailySummaryEntity)

    @Query("SELECT * FROM daily_summaries WHERE localDate = :date")
    suspend fun getSummary(date: String): DailySummaryEntity?

    @Query("SELECT * FROM daily_summaries ORDER BY localDate DESC")
    suspend fun getAllSummaries(): List<DailySummaryEntity>

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMeal(id: String)

    @Query("DELETE FROM meal_items WHERE mealId = :id")
    suspend fun deleteItems(id: String)

    @Query("DELETE FROM daily_summaries WHERE localDate = :date")
    suspend fun deleteSummary(date: String)

    @Query("UPDATE meals SET title = :title, rating = :rating WHERE id = :id")
    suspend fun updateMealMetadata(id: String, title: String, rating: Int)

    @Query("""
        SELECT COALESCE(SUM(i.caloriesPer100g * i.grams / 100.0), 0.0) AS calories,
               COALESCE(SUM(i.proteinPer100g * i.grams / 100.0), 0.0) AS protein,
               COALESCE(SUM(i.carbsPer100g * i.grams / 100.0), 0.0) AS carbs,
               COALESCE(SUM(i.fatPer100g * i.grams / 100.0), 0.0) AS fat,
               COALESCE(SUM(i.fiberPer100g * i.grams / 100.0), 0.0) AS fiber,
               COUNT(DISTINCT m.id) AS mealCount
        FROM meals m INNER JOIN meal_items i ON m.id = i.mealId
        WHERE m.timestamp >= :start AND m.timestamp < :end
    """)
    suspend fun aggregateDay(start: Long, end: Long): DayTotals

    @Transaction
    suspend fun replaceMeal(meal: MealEntity, items: List<MealItemEntity>) {
        insertMeal(meal)
        deleteItems(meal.id)
        insertItems(items)
    }

    @Transaction
    suspend fun removeMeal(id: String) {
        deleteItems(id)
        deleteMeal(id)
    }

    @Query("SELECT imagePath FROM meals WHERE timestamp >= :start AND timestamp < :end AND imagePath IS NOT NULL")
    suspend fun imagePathsInRange(start: Long, end: Long): List<String?>

    @Query("SELECT imagePath FROM meals WHERE timestamp < :cutoff AND imagePath IS NOT NULL")
    suspend fun imagePathsBefore(cutoff: Long): List<String?>

    @Query("DELETE FROM meal_items WHERE mealId IN (SELECT id FROM meals WHERE timestamp < :cutoff)")
    suspend fun deleteItemsBefore(cutoff: Long)

    @Query("DELETE FROM meals WHERE timestamp < :cutoff")
    suspend fun deleteMealsBefore(cutoff: Long)
}

@Database(
    entities = [MealEntity::class, MealItemEntity::class, DailySummaryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class CalorieDatabase : androidx.room.RoomDatabase() {
    abstract fun mealDao(): MealDao

    companion object {
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE meals ADD COLUMN rating INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_summaries (
                        localDate TEXT NOT NULL PRIMARY KEY,
                        calories INTEGER NOT NULL,
                        protein REAL NOT NULL,
                        carbs REAL NOT NULL,
                        fat REAL NOT NULL,
                        fiber REAL NOT NULL,
                        targetCalories INTEGER NOT NULL,
                        mealCount INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT OR REPLACE INTO daily_summaries
                        (localDate, calories, protein, carbs, fat, fiber, targetCalories, mealCount)
                    SELECT date(m.timestamp / 1000, 'unixepoch', 'localtime'),
                           CAST(ROUND(COALESCE(SUM(i.caloriesPer100g * i.grams / 100.0), 0)) AS INTEGER),
                           COALESCE(SUM(i.proteinPer100g * i.grams / 100.0), 0),
                           COALESCE(SUM(i.carbsPer100g * i.grams / 100.0), 0),
                           COALESCE(SUM(i.fatPer100g * i.grams / 100.0), 0),
                           COALESCE(SUM(i.fiberPer100g * i.grams / 100.0), 0),
                           0,
                           COUNT(DISTINCT m.id)
                    FROM meals m INNER JOIN meal_items i ON m.id = i.mealId
                    GROUP BY date(m.timestamp / 1000, 'unixepoch', 'localtime')
                    """.trimIndent()
                )
            }
        }

        fun create(context: Context): CalorieDatabase = Room
            .databaseBuilder(context, CalorieDatabase::class.java, "calorie-lens.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }
}

class PersistentMealStore(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao = CalorieDatabase.create(appContext).mealDao()

    val dailySummaries: Flow<List<DailySummary>> = dao.observeDailySummaries().map { rows ->
        rows.map { it.toDomain() }
    }
    val allMeals: Flow<List<Meal>> = dao.observeAllMeals().map { mapMeals(it) }

    init {
        scope.launch { performMaintenance() }
    }

    fun mealsForDate(date: LocalDate): Flow<List<Meal>> {
        val (start, end) = dayBounds(date)
        return dao.observeMealsInRange(start, end).map { mapMeals(it) }
    }

    suspend fun addMealAndReturn(
        items: List<FoodItem>,
        title: String,
        rating: Int = 0,
        targetCalories: Int,
        imageSourceUri: Uri? = null
    ): Meal {
        val mealId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val imagePath = imageSourceUri?.let { saveTodayImage(mealId, it) }
        val normalizedRating = rating.coerceIn(0, 5)
        dao.replaceMeal(
            MealEntity(mealId, timestamp, title, imagePath, normalizedRating),
            items.map { it.toEntity(mealId) }
        )
        refreshSummary(timestamp.toLocalDate(), targetCalories)
        purgeNonTodayImages()
        return Meal(mealId, timestamp, title, items, imagePath, normalizedRating)
    }

    suspend fun updateMealAndReturn(
        meal: Meal,
        items: List<FoodItem>,
        title: String,
        rating: Int,
        targetCalories: Int
    ): Meal {
        val normalizedRating = rating.coerceIn(0, 5)
        dao.replaceMeal(
            MealEntity(meal.id, meal.timestamp, title, meal.imagePath, normalizedRating),
            items.map { it.toEntity(meal.id) }
        )
        refreshSummary(meal.timestamp.toLocalDate(), targetCalories)
        return meal.copy(title = title, items = items, rating = normalizedRating)
    }

    fun deleteMeal(meal: Meal) {
        scope.launch {
            dao.removeMeal(meal.id)
            refreshSummary(meal.timestamp.toLocalDate(), 0)
            deleteImage(meal.imagePath)
        }
    }

    fun restoreMeal(meal: Meal, targetCalories: Int) {
        scope.launch {
            dao.replaceMeal(
                MealEntity(meal.id, meal.timestamp, meal.title, meal.imagePath, meal.rating),
                meal.items.map { it.toEntity(meal.id) }
            )
            refreshSummary(meal.timestamp.toLocalDate(), targetCalories)
        }
    }

    fun updateMeal(meal: Meal, items: List<FoodItem>, title: String, rating: Int, targetCalories: Int) {
        scope.launch { updateMealAndReturn(meal, items, title, rating, targetCalories) }
    }

    suspend fun allMealsSnapshot(): List<Meal> {
        return mapMeals(dao.observeAllMeals().first())
    }

    suspend fun summariesSnapshot(): List<DailySummary> = dao.getAllSummaries().map { it.toDomain() }

    suspend fun restoreBackup(meals: List<Meal>, summaries: List<DailySummary>) {
        meals.forEach { meal ->
            dao.replaceMeal(
                MealEntity(meal.id, meal.timestamp, meal.title, null, meal.rating),
                meal.items.map { it.toEntity(meal.id) }
            )
        }
        summaries.forEach { dao.insertSummary(it.toEntity()) }
        performMaintenance()
    }

    suspend fun performMaintenance() {
        pruneOldDetails()
        purgeNonTodayImages()
    }

    private suspend fun refreshSummary(date: LocalDate, requestedTarget: Int) {
        val (start, end) = dayBounds(date)
        val totals = dao.aggregateDay(start, end)
        if (totals.mealCount == 0) {
            dao.deleteSummary(date.toString())
            return
        }
        val existingTarget = dao.getSummary(date.toString())?.targetCalories?.takeIf { it > 0 }
        dao.insertSummary(
            DailySummaryEntity(
                localDate = date.toString(),
                calories = totals.calories.toInt(),
                protein = totals.protein,
                carbs = totals.carbs,
                fat = totals.fat,
                fiber = totals.fiber,
                targetCalories = existingTarget ?: requestedTarget.coerceAtLeast(0),
                mealCount = totals.mealCount
            )
        )
    }

    private suspend fun pruneOldDetails() {
        val cutoffDate = LocalDate.now().minusDays(DETAIL_RETENTION_DAYS.toLong())
        val cutoff = cutoffDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val expiredImages = dao.imagePathsBefore(cutoff)
        dao.deleteItemsBefore(cutoff)
        dao.deleteMealsBefore(cutoff)
        expiredImages.forEach { deleteImage(it) }
    }

    private suspend fun purgeNonTodayImages() {
        val imageDir = File(appContext.filesDir, IMAGE_DIR).apply { mkdirs() }
        val (start, end) = dayBounds(LocalDate.now())
        val validPaths = dao.imagePathsInRange(start, end).filterNotNull().toSet()
        imageDir.listFiles()?.forEach { file ->
            if (file.absolutePath !in validPaths) file.delete()
        }
    }

    private suspend fun saveTodayImage(mealId: String, source: Uri): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        appContext.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sample = 1
        while (bounds.outWidth / sample > 900 || bounds.outHeight / sample > 900) sample *= 2
        val bitmap = appContext.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@runCatching null
        val orientation = runCatching {
            appContext.contentResolver.openInputStream(source)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull()
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            }
        }
        val oriented = if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val output = ByteArrayOutputStream()
        oriented.compress(Bitmap.CompressFormat.JPEG, 76, output)
        if (oriented !== bitmap) oriented.recycle()
        bitmap.recycle()
        val file = File(File(appContext.filesDir, IMAGE_DIR).apply { mkdirs() }, "$mealId.jpg")
        file.writeBytes(output.toByteArray())
        file.absolutePath
    }.getOrNull()

    private fun deleteImage(path: String?) {
        path ?: return
        runCatching { File(path).takeIf { it.parentFile?.name == IMAGE_DIR }?.delete() }
    }

    private fun mapMeals(rows: List<MealWithItems>): List<Meal> = rows.map { row ->
        Meal(row.meal.id, row.meal.timestamp, row.meal.title, row.items.map { it.toDomain() }, row.meal.imagePath, row.meal.rating)
    }

    private companion object {
        const val IMAGE_DIR = "meal_images"
        const val DETAIL_RETENTION_DAYS = 30
    }
}

private fun dayBounds(date: LocalDate): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    return date.atStartOfDay(zone).toInstant().toEpochMilli() to
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private fun DailySummaryEntity.toDomain() = DailySummary(
    localDate = LocalDate.parse(localDate),
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    fiber = fiber,
    targetCalories = targetCalories,
    mealCount = mealCount
)

private fun DailySummary.toEntity() = DailySummaryEntity(
    localDate = localDate.toString(),
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    fiber = fiber,
    targetCalories = targetCalories,
    mealCount = mealCount
)

private fun FoodItem.toEntity(mealId: String) = MealItemEntity(
    id, mealId, name, grams, caloriesPer100g, proteinPer100g,
    fatPer100g, carbsPer100g, fiberPer100g, confidence
)

private fun MealItemEntity.toDomain() = FoodItem(
    id, name, grams, caloriesPer100g, proteinPer100g,
    fatPer100g, carbsPer100g, fiberPer100g, confidence
)
