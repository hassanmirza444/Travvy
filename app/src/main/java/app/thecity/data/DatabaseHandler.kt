package app.thecity.data

import android.content.ContentValues
import android.content.Context
import android.content.res.TypedArray
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import android.util.Log
import app.thecity.R
import app.thecity.model.Category
import app.thecity.model.Images
import app.thecity.model.NewsInfo
import app.thecity.model.Place
import app.thecity.utils.Tools
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

class DatabaseHandler(private val context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {
    private val db: SQLiteDatabase
    private val cat_id: IntArray // category id
    private val cat_name: Array<String> // category name
    private val cat_icon // category name
            : TypedArray

    // Creating Tables
    override fun onCreate(d: SQLiteDatabase) {
        createTablePlace(d)
        createTableImages(d)
        createTableCategory(d)
        createTableRelational(d)
        createTableFavorites(d)
        createTableNewsInfo(d)
    }

    private fun createTablePlace(db: SQLiteDatabase) {
        val CREATE_TABLE = ("CREATE TABLE " + TABLE_PLACE + " ("
                + KEY_PLACE_ID + " INTEGER PRIMARY KEY, "
                + KEY_NAME + " TEXT, "
                + KEY_IMAGE + " TEXT, "
                + KEY_ADDRESS + " TEXT, "
                + KEY_PHONE + " TEXT, "
                + KEY_WEBSITE + " TEXT, "
                + KEY_DESCRIPTION + " TEXT, "
                + KEY_LNG + " REAL, "
                + KEY_LAT + " REAL, "
                + KEY_DISTANCE + " REAL, "
                + KEY_LAST_UPDATE + " NUMERIC "
                + ")")
        db.execSQL(CREATE_TABLE)
    }

    private fun createTableImages(db: SQLiteDatabase) {
        val CREATE_TABLE = ("CREATE TABLE " + TABLE_IMAGES + " ("
                + KEY_IMG_PLACE_ID + " INTEGER, "
                + KEY_IMG_NAME + " TEXT, "
                + " FOREIGN KEY(" + KEY_IMG_PLACE_ID + ") REFERENCES " + TABLE_PLACE + "(" + KEY_PLACE_ID + ")"
                + " )")
        db.execSQL(CREATE_TABLE)
    }

    private fun createTableCategory(db: SQLiteDatabase) {
        val CREATE_TABLE = ("CREATE TABLE " + TABLE_CATEGORY + "("
                + KEY_CAT_ID + " INTEGER PRIMARY KEY, "
                + KEY_CAT_NAME + " TEXT, "
                + KEY_CAT_ICON + " INTEGER"
                + ")")
        db.execSQL(CREATE_TABLE)
    }

    private fun createTableFavorites(db: SQLiteDatabase) {
        val CREATE_TABLE = ("CREATE TABLE " + TABLE_FAVORITES + "("
                + KEY_PLACE_ID + " INTEGER PRIMARY KEY "
                + ")")
        db.execSQL(CREATE_TABLE)
    }

    private fun defineCategory(db: SQLiteDatabase) {
        db.execSQL("DELETE FROM " + TABLE_CATEGORY) // refresh table content
        db.execSQL("VACUUM")
        for (i in cat_id.indices) {
            val values = ContentValues()
            values.put(KEY_CAT_ID, cat_id[i])
            values.put(KEY_CAT_NAME, cat_name[i])
            values.put(KEY_CAT_ICON, cat_icon.getResourceId(i, 0))
            db.insert(TABLE_CATEGORY, null, values) // Inserting Row
        }
    }

    // Table Relational place_category
    private fun createTableRelational(db: SQLiteDatabase) {
        val CREATE_TABLE = ("CREATE TABLE " + TABLE_PLACE_CATEGORY + "("
                + KEY_RELATION_PLACE_ID + " INTEGER, " // id from table place
                + KEY_RELATION_CAT_ID + " INTEGER " // id from table category
                + ")")
        db.execSQL(CREATE_TABLE)
    }

    private fun createTableNewsInfo(db: SQLiteDatabase) {
        val CREATE_TABLE = ("CREATE TABLE " + TABLE_NEWS_INFO + " ("
                + KEY_NEWS_ID + " INTEGER PRIMARY KEY, "
                + KEY_NEWS_TITLE + " TEXT, "
                + KEY_NEWS_BRIEF_CONTENT + " TEXT, "
                + KEY_NEWS_FULL_CONTENT + " TEXT, "
                + KEY_NEWS_IMAGE + " TEXT, "
                + KEY_NEWS_LAST_UPDATE + " NUMERIC "
                + ")")
        db.execSQL(CREATE_TABLE)
    }

    // Upgrading database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("DB ", "onUpgrade $oldVersion to $newVersion")
        if (oldVersion < newVersion) {
            // Drop older table if existed
            truncateDB(db)
        }
    }

    fun truncateDB(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLACE)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGES)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORY)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLACE_CATEGORY)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NEWS_INFO)

        // Create tables again
        onCreate(db)
    }

    // refresh table place and place_category
    fun refreshTablePlace() {
        db.execSQL("DELETE FROM " + TABLE_PLACE_CATEGORY)
        db.execSQL("VACUUM")
        db.execSQL("DELETE FROM " + TABLE_IMAGES)
        db.execSQL("VACUUM")
        db.execSQL("DELETE FROM " + TABLE_PLACE)
        db.execSQL("VACUUM")
    }

    // refresh table place and place_category
    fun refreshTableNewsInfo() {
        db.execSQL("DELETE FROM " + TABLE_NEWS_INFO)
        db.execSQL("VACUUM")
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */
    // Insert List place
    fun insertListPlace(modelList: List<Place>) {
        var modelList = modelList
        modelList = Tools.itemsWithDistance(context, modelList)
        for (p in modelList) {
            val values = getPlaceValue(p)
            // Inserting or Update Row
            db.insertWithOnConflict(TABLE_PLACE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            // Insert relational place with category
            insertListPlaceCategory(p.place_id, p.categories)
            // Insert Images places
            insertListImages(p.images)
        }
    }

    // Insert List place with asynchronous scheme
    fun insertListPlaceAsync(modelList: List<Place>) {
        var modelList = modelList
        var sql = "INSERT OR REPLACE INTO " + TABLE_PLACE + " "
        sql =
            (sql + "(" + KEY_PLACE_ID + ", " + KEY_NAME + "," + KEY_IMAGE + ", " + KEY_ADDRESS + ", " + KEY_PHONE + ", "
                    + KEY_WEBSITE + ", " + KEY_DESCRIPTION + ", " + KEY_LNG + ", " + KEY_LAT + ", " + KEY_DISTANCE + ", " + KEY_LAST_UPDATE + ") ")
        sql = sql + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        db.beginTransaction()
        val stmt = db.compileStatement(sql)
        modelList = Tools.itemsWithDistance(context, modelList)
        for (p in modelList) {
            stmt.bindLong(1, p.place_id.toLong())
            stmt.bindString(2, p.name)
            stmt.bindString(3, p.image)
            stmt.bindString(4, p.address)
            stmt.bindString(5, p.phone)
            stmt.bindString(6, p.website)
            stmt.bindString(7, p.description)
            stmt.bindDouble(8, p.lng)
            stmt.bindDouble(9, p.lat)
            stmt.bindDouble(10, p.distance.toDouble())
            stmt.bindDouble(11, p.last_update.toDouble())
            stmt.execute()
            stmt.clearBindings()

            // Insert relational place with category
            insertListPlaceCategoryAsync(p.place_id, p.categories)
            // Insert Images places
            Log.d("place", p.toString())
            insertListImagesAsync(p)
        }
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    // Insert List place
    fun insertListNewsInfo(modelList: List<NewsInfo>) {
        for (n in modelList) {
            val values = getNewsInfoValue(n)
            // Inserting or Update Row
            db.insertWithOnConflict(TABLE_NEWS_INFO, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    // Update one place
    fun updatePlace(place: Place): Place? {
        val objcs: MutableList<Place> = ArrayList()
        objcs.add(place)
        insertListPlace(objcs)
        return if (isPlaceExist(place.place_id)) {
            getPlace(place.place_id)
        } else null
    }

    private fun getPlaceValue(model: Place): ContentValues {
        val values = ContentValues()
        values.put(KEY_PLACE_ID, model.place_id)
        values.put(KEY_NAME, model.name)
        values.put(KEY_IMAGE, model.image)
        values.put(KEY_ADDRESS, model.address)
        values.put(KEY_PHONE, model.phone)
        values.put(KEY_WEBSITE, model.website)
        values.put(KEY_DESCRIPTION, model.description)
        values.put(KEY_LNG, model.lng)
        values.put(KEY_LAT, model.lat)
        values.put(KEY_DISTANCE, model.distance)
        values.put(KEY_LAST_UPDATE, model.last_update)
        return values
    }

    private fun getNewsInfoValue(model: NewsInfo): ContentValues {
        val values = ContentValues()
        values.put(KEY_NEWS_ID, model.id)
        values.put(KEY_NEWS_TITLE, model.title)
        values.put(KEY_NEWS_BRIEF_CONTENT, model.brief_content)
        values.put(KEY_NEWS_FULL_CONTENT, model.full_content)
        values.put(KEY_NEWS_IMAGE, model.image)
        values.put(KEY_LAST_UPDATE, model.last_update)
        return values
    }

    // Adding new location by Category
    fun searchAllPlace(keyword: String): List<Place?> {
        var keyword = keyword
        var locList: List<Place?> = ArrayList()
        val cur: Cursor
        if (keyword == "") {
            cur = db.rawQuery(
                "SELECT p.* FROM " + TABLE_PLACE + " p ORDER BY " + KEY_LAST_UPDATE + " DESC",
                null
            )
        } else {
            keyword = keyword.lowercase(Locale.getDefault())
            cur = db.rawQuery(
                "SELECT * FROM " + TABLE_PLACE + " WHERE LOWER(" + KEY_NAME + ") LIKE ? OR LOWER(" + KEY_ADDRESS + ") LIKE ? OR LOWER(" + KEY_DESCRIPTION + ") LIKE ? ",
                arrayOf(
                    "%$keyword%", "%$keyword%", "%$keyword%"
                )
            )
        }
        locList = getListPlaceByCursor(cur)
        return locList
    }

    val allPlace: List<Place?>
        get() = getAllPlaceByCategory(-1)

    fun getPlacesByPage(c_id: Int, limit: Int, offset: Int): List<Place?> {
        var locList: List<Place?> = ArrayList()
        val sb = StringBuilder()
        sb.append(" SELECT DISTINCT p.* FROM " + TABLE_PLACE + " p ")
        if (c_id == -2) {
            sb.append(", " + TABLE_FAVORITES + " f ")
            sb.append(" WHERE p." + KEY_PLACE_ID + " = f." + KEY_PLACE_ID + " ")
        } else if (c_id != -1) {
            sb.append(", " + TABLE_PLACE_CATEGORY + " pc ")
            sb.append(" WHERE pc." + KEY_RELATION_PLACE_ID + " = p." + KEY_PLACE_ID + " AND pc." + KEY_RELATION_CAT_ID + "=" + c_id + " ")
        }
        sb.append(" ORDER BY p." + KEY_DISTANCE + " ASC, p." + KEY_LAST_UPDATE + " DESC ")
        sb.append(" LIMIT $limit OFFSET $offset ")
        val cursor = db.rawQuery(sb.toString(), null)
        if (cursor.moveToFirst()) {
            locList = getListPlaceByCursor(cursor)
        }
        return locList
    }

    fun getAllPlaceByCategory(c_id: Int): List<Place?> {
        var locList: List<Place?> = ArrayList()
        val sb = StringBuilder()
        sb.append(" SELECT DISTINCT p.* FROM " + TABLE_PLACE + " p ")
        if (c_id == -2) {
            sb.append(", " + TABLE_FAVORITES + " f ")
            sb.append(" WHERE p." + KEY_PLACE_ID + " = f." + KEY_PLACE_ID + " ")
        } else if (c_id != -1) {
            sb.append(", " + TABLE_PLACE_CATEGORY + " pc ")
            sb.append(" WHERE pc." + KEY_RELATION_PLACE_ID + " = p." + KEY_PLACE_ID + " AND pc." + KEY_RELATION_CAT_ID + "=" + c_id + " ")
        }
        sb.append(" ORDER BY p." + KEY_LAST_UPDATE + " DESC ")
        val cursor = db.rawQuery(sb.toString(), null)
        if (cursor.moveToFirst()) {
            locList = getListPlaceByCursor(cursor)
        }
        return locList
    }

    fun getPlace(place_id: Int): Place {
        var p = Place()
        val query = "SELECT * FROM " + TABLE_PLACE + " p WHERE p." + KEY_PLACE_ID + " = ?"
        val cursor = db.rawQuery(query, arrayOf(place_id.toString() + ""))
        p.place_id = place_id
        if (cursor.moveToFirst()) {
            cursor.moveToFirst()
            p = getPlaceByCursor(cursor)
        }
        return p
    }

    private fun getListPlaceByCursor(cur: Cursor): List<Place?> {
        val locList: MutableList<Place?> = ArrayList()
        // looping through all rows and adding to list
        if (cur.moveToFirst()) {
            do {
                // Adding place to list
                locList.add(getPlaceByCursor(cur))
            } while (cur.moveToNext())
        }
        return locList
    }

    private fun getListNewsInfoByCursor(cur: Cursor): List<NewsInfo> {
        val list: MutableList<NewsInfo> = ArrayList()
        // looping through all rows and adding to list
        if (cur.moveToFirst()) {
            do {
                // Adding place to list
                list.add(getNewsInfoByCursor(cur))
            } while (cur.moveToNext())
        }
        return list
    }

    private fun getPlaceByCursor(cur: Cursor): Place {
        val p = Place()
        p.place_id = cur.getInt(cur.getColumnIndex(KEY_PLACE_ID))
        p.name = cur.getString(cur.getColumnIndex(KEY_NAME))
        p.image = cur.getString(cur.getColumnIndex(KEY_IMAGE))
        p.address = cur.getString(cur.getColumnIndex(KEY_ADDRESS))
        p.phone = cur.getString(cur.getColumnIndex(KEY_PHONE))
        p.website = cur.getString(cur.getColumnIndex(KEY_WEBSITE))
        p.description = cur.getString(cur.getColumnIndex(KEY_DESCRIPTION))
        p.lng = cur.getDouble(cur.getColumnIndex(KEY_LNG))
        p.lat = cur.getDouble(cur.getColumnIndex(KEY_LAT))
        p.distance = cur.getFloat(cur.getColumnIndex(KEY_DISTANCE))
        p.last_update = cur.getLong(cur.getColumnIndex(KEY_LAST_UPDATE))
        return p
    }

    private fun getNewsInfoByCursor(cur: Cursor): NewsInfo {
        val n = NewsInfo()
        n.id = cur.getInt(cur.getColumnIndex(KEY_NEWS_ID))
        n.title = cur.getString(cur.getColumnIndex(KEY_NEWS_TITLE))
        n.brief_content = cur.getString(cur.getColumnIndex(KEY_NEWS_BRIEF_CONTENT))
        n.full_content = cur.getString(cur.getColumnIndex(KEY_NEWS_FULL_CONTENT))
        n.image = cur.getString(cur.getColumnIndex(KEY_NEWS_IMAGE))
        n.last_update = cur.getLong(cur.getColumnIndex(KEY_NEWS_LAST_UPDATE))
        return n
    }

    // Get LIst Images By Place Id
    fun getListImageByPlaceId(place_id: Int): List<Images> {
        val imageList: MutableList<Images> = ArrayList()
        val selectQuery = "SELECT * FROM " + TABLE_IMAGES + " WHERE " + KEY_IMG_PLACE_ID + " = ?"
        val cursor = db.rawQuery(selectQuery, arrayOf(place_id.toString() + ""))
        if (cursor.moveToFirst()) {
            do {
                val img = Images(cursor.getInt(0), cursor.getString(1))
                imageList.add(img)
            } while (cursor.moveToNext())
        }
        return imageList
    }

    fun getCategory(c_id: Int): Category? {
        val category = Category()
        try {
            val cur = db.rawQuery(
                "SELECT * FROM " + TABLE_CATEGORY + " WHERE " + KEY_CAT_ID + " = ?",
                arrayOf(c_id.toString() + "")
            )
            cur.moveToFirst()
            category.cat_id = cur.getInt(0)
            category.name = cur.getString(1)
            category.icon = cur.getInt(2)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Db Error", e.toString())
            return null
        }
        return category
    }

    // get list News Info
    fun getNewsInfoByPage(limit: Int, offset: Int): List<NewsInfo> {
        Log.d("DB", "Size : $newsInfoSize")
        Log.d("DB", "Limit : $limit Offset : $offset")
        var list: List<NewsInfo> = ArrayList()
        val sb = StringBuilder()
        sb.append(" SELECT DISTINCT n.* FROM " + TABLE_NEWS_INFO + " n ")
        sb.append(" ORDER BY n." + KEY_NEWS_ID + " DESC ")
        sb.append(" LIMIT $limit OFFSET $offset ")
        val cursor = db.rawQuery(sb.toString(), null)
        if (cursor.moveToFirst()) {
            list = getListNewsInfoByCursor(cursor)
        }
        return list
    }

    // Insert new imagesList
    fun insertListImages(images: List<Images>) {
        for (i in images.indices) {
            val values = ContentValues()
            values.put(KEY_IMG_PLACE_ID, images[i].place_id)
            values.put(KEY_IMG_NAME, images[i].name)
            // Inserting or Update Row
            db.insertWithOnConflict(TABLE_IMAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    // Insert new imagesList with asynchronous scheme
    fun insertListImagesAsync(place: Place) {
        var sql = "INSERT OR REPLACE INTO " + TABLE_IMAGES + " "
        sql = sql + "(" + KEY_IMG_PLACE_ID + ", " + KEY_IMG_NAME + ") VALUES (?, ?)"
        val stmt = db.compileStatement(sql)
        for (i in place.images) {
            stmt.bindLong(1, i.place_id.toLong())
            stmt.bindString(2, i.name)
            stmt.execute()
            stmt.clearBindings()
        }
    }

    // Inserting new Table PLACE_CATEGORY relational
    fun insertListPlaceCategory(place_id: Int, categories: List<Category>) {
        for (c in categories) {
            val values = ContentValues()
            values.put(KEY_RELATION_PLACE_ID, place_id)
            values.put(KEY_RELATION_CAT_ID, c.cat_id)
            // Inserting or Update Row
            db.insertWithOnConflict(
                TABLE_PLACE_CATEGORY,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }
    }

    // Inserting new Table PLACE_CATEGORY relational with asynchronous scheme
    fun insertListPlaceCategoryAsync(place_id: Int, categories: List<Category>) {
        var sql = "INSERT OR REPLACE INTO " + TABLE_PLACE_CATEGORY + " "
        sql = sql + "(" + KEY_RELATION_PLACE_ID + ", " + KEY_RELATION_CAT_ID + ") VALUES (?, ?)"
        val stmt = db.compileStatement(sql)
        for (c in categories) {
            stmt.bindLong(1, place_id.toLong())
            stmt.bindLong(2, c.cat_id.toLong())
            stmt.execute()
            stmt.clearBindings()
        }
    }

    // Adding new Connector
    fun addFavorites(id: Int) {
        val values = ContentValues()
        values.put(KEY_PLACE_ID, id)
        // Inserting Row
        db.insert(TABLE_FAVORITES, null, values)
    }

    // all Favorites
    val allFavorites: List<Place?>
        get() {
            var locList: List<Place?> = ArrayList()
            val cursor = db.rawQuery(
                "SELECT p.* FROM " + TABLE_PLACE + " p, " + TABLE_FAVORITES + " f" + " WHERE p." + KEY_PLACE_ID + " = f." + KEY_PLACE_ID,
                null
            )
            locList = getListPlaceByCursor(cursor)
            return locList
        }

    fun deleteFavorites(id: Int) {
        if (isFavoritesExist(id)) {
            db.delete(TABLE_FAVORITES, KEY_PLACE_ID + " = ?", arrayOf(id.toString() + ""))
        }
    }

    fun isFavoritesExist(id: Int): Boolean {
        val cursor = db.rawQuery(
            "SELECT * FROM " + TABLE_FAVORITES + " WHERE " + KEY_PLACE_ID + " = ?",
            arrayOf(id.toString() + "")
        )
        val count = cursor.count
        return if (count > 0) {
            true
        } else {
            false
        }
    }

    private fun isPlaceExist(id: Int): Boolean {
        val cursor = db.rawQuery(
            "SELECT * FROM " + TABLE_PLACE + " WHERE " + KEY_PLACE_ID + " = ?",
            arrayOf(id.toString() + "")
        )
        val count = cursor.count
        cursor.close()
        return if (count > 0) {
            true
        } else {
            false
        }
    }

    val placesSize: Int
        get() = DatabaseUtils.queryNumEntries(db, TABLE_PLACE)
            .toInt()
    val newsInfoSize: Int
        get() = DatabaseUtils.queryNumEntries(db, TABLE_NEWS_INFO)
            .toInt()

    fun getPlacesSize(c_id: Int): Int {
        val sb = StringBuilder()
        sb.append("SELECT COUNT(DISTINCT p." + KEY_PLACE_ID + ") FROM " + TABLE_PLACE + " p ")
        if (c_id == -2) {
            sb.append(", " + TABLE_FAVORITES + " f ")
            sb.append(" WHERE p." + KEY_PLACE_ID + " = f." + KEY_PLACE_ID + " ")
        } else if (c_id != -1) {
            sb.append(", " + TABLE_PLACE_CATEGORY + " pc ")
            sb.append(" WHERE pc." + KEY_RELATION_PLACE_ID + " = p." + KEY_PLACE_ID + " AND pc." + KEY_RELATION_CAT_ID + "=" + c_id + " ")
        }
        val cursor = db.rawQuery(sb.toString(), null)
        cursor.moveToFirst()
        val size = cursor.getInt(0)
        cursor.close()
        return size
    }

    val categorySize: Int
        get() = DatabaseUtils.queryNumEntries(db, TABLE_CATEGORY)
            .toInt()
    val favoritesSize: Int
        get() = DatabaseUtils.queryNumEntries(db, TABLE_FAVORITES)
            .toInt()
    val imagesSize: Int
        get() = DatabaseUtils.queryNumEntries(db, TABLE_IMAGES)
            .toInt()
    val placeCategorySize: Int
        get() = DatabaseUtils.queryNumEntries(db, TABLE_PLACE_CATEGORY)
            .toInt()

    // to export database file
    // for debugging only
    private fun exportDatabase() {
        try {
            val sd = Environment.getExternalStorageDirectory()
            if (sd.canWrite()) {
                val currentDBPath =
                    "/data/data/" + context.packageName + "/databases/" + DATABASE_NAME
                val backupDBPath = "backup_" + DATABASE_NAME + ".db"
                val currentDB = File(currentDBPath)
                val backupDB = File(sd, backupDBPath)
                if (currentDB.exists()) {
                    val src = FileInputStream(currentDB).channel
                    val dst = FileOutputStream(backupDB).channel
                    dst.transferFrom(src, 0, src.size())
                    src.close()
                    dst.close()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        // Database Version
        private const val DATABASE_VERSION = 4

        // Database Name
        private const val DATABASE_NAME = "the_city"

        // Main Table Name
        private const val TABLE_PLACE = "place"
        private const val TABLE_IMAGES = "images"
        private const val TABLE_CATEGORY = "category"
        private const val TABLE_NEWS_INFO = "news_info"

        // Relational table Place to Category ( N to N )
        private const val TABLE_PLACE_CATEGORY = "place_category"

        // table only for android client
        private const val TABLE_FAVORITES = "favorites_table"

        // Table Columns names TABLE_PLACE
        private const val KEY_PLACE_ID = "place_id"
        private const val KEY_NAME = "name"
        private const val KEY_IMAGE = "image"
        private const val KEY_ADDRESS = "address"
        private const val KEY_PHONE = "phone"
        private const val KEY_WEBSITE = "website"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_LNG = "lng"
        private const val KEY_LAT = "lat"
        private const val KEY_DISTANCE = "distance"
        private const val KEY_LAST_UPDATE = "last_update"

        // Table Columns names TABLE_IMAGES
        private const val KEY_IMG_PLACE_ID = "place_id"
        private const val KEY_IMG_NAME = "name"

        // Table Columns names TABLE_CATEGORY
        private const val KEY_CAT_ID = "cat_id"
        private const val KEY_CAT_NAME = "name"
        private const val KEY_CAT_ICON = "icon"

        // Table Columns names TABLE_NEWS_INFO
        private const val KEY_NEWS_ID = "id"
        private const val KEY_NEWS_TITLE = "title"
        private const val KEY_NEWS_BRIEF_CONTENT = "brief_content"
        private const val KEY_NEWS_FULL_CONTENT = "full_content"
        private const val KEY_NEWS_IMAGE = "image"
        private const val KEY_NEWS_LAST_UPDATE = "last_update"

        // Table Relational Columns names TABLE_PLACE_CATEGORY
        private const val KEY_RELATION_PLACE_ID = KEY_PLACE_ID
        private const val KEY_RELATION_CAT_ID = KEY_CAT_ID
    }

    init {
        db = writableDatabase

        // get data from res/values/category.xml
        cat_id = context.resources.getIntArray(R.array.id_category)
        cat_name = context.resources.getStringArray(R.array.category_name)
        cat_icon = context.resources.obtainTypedArray(R.array.category_icon)

        // if length not equal refresh table category
        if (categorySize != cat_id.size) {
            defineCategory(db) // define table category
        }
    }
}