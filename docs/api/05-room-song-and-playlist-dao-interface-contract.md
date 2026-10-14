# Subsystem API Specification 05: Room Song and Playlist Data Access Object Contract

## Key Methods
```kotlin
@Dao
interface SongDao {
    @Query("SELECT * FROM Song WHERE id = :songId")
    suspend fun getSongById(songId: String): SongEntity?

    @Query("SELECT * FROM Song ORDER BY dateAdded DESC")
    fun getAllSongsPaged(): PagingSource<Int, SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(song: SongEntity)
}
```

## Transaction Invariants
- Batch insertions must occur inside `@Transaction` blocks.
- Cache references must be foreign-key linked with `CASCADE` delete safety.\n