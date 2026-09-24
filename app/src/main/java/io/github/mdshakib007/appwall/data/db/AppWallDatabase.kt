package io.github.mdshakib007.appwall.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun fromType(t: BlockType): String = t.name
    @TypeConverter fun toType(s: String): BlockType = BlockType.valueOf(s)
}

@Database(
    entities = [BlockItem::class, FocusSession::class, BlockAttempt::class, SiteSession::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppWallDatabase : RoomDatabase() {
    abstract fun blockItems(): BlockItemDao
    abstract fun focusSessions(): FocusSessionDao
    abstract fun attempts(): BlockAttemptDao
    abstract fun siteSessions(): SiteSessionDao

    companion object {
        fun build(context: Context): AppWallDatabase =
            Room.databaseBuilder(context, AppWallDatabase::class.java, "appwall.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
