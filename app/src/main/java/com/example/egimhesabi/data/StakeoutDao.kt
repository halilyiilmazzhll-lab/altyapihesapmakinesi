package com.example.egimhesabi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class StakeoutDao {
    @Query("SELECT * FROM neighborhoods WHERE districtId = :districtId ORDER BY name ASC")
    abstract fun neighborhoods(districtId: Long): Flow<List<NeighborhoodEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertNeighborhoodIgnoringConflict(neighborhood: NeighborhoodEntity): Long

    open suspend fun insertNeighborhood(neighborhood: NeighborhoodEntity): Long {
        require(neighborhood.districtId > 0) { "Mahalle için bir ilçe seçilmelidir." }
        val name = neighborhood.name.trim()
        require(name.isNotBlank()) { "Mahalle adı boş bırakılamaz." }
        return insertNeighborhoodIgnoringConflict(
            neighborhood.copy(name = name, nameKey = normalizeEntityName(name))
        )
    }

    @Query("SELECT * FROM stakeout_manholes WHERE neighborhoodId = :neighborhoodId ORDER BY name ASC")
    abstract fun records(neighborhoodId: Long): Flow<List<StakeoutManholeEntity>>

    @Query(
        "SELECT s.*, d.name AS districtName, n.name AS neighborhoodName " +
            "FROM stakeout_manholes s " +
            "INNER JOIN neighborhoods n ON n.id = s.neighborhoodId " +
            "INNER JOIN districts d ON d.id = n.districtId " +
            "ORDER BY d.name ASC, n.name ASC, s.name ASC"
    )
    abstract fun allRecords(): Flow<List<StakeoutRecord>>

    @Query("SELECT * FROM stakeout_manholes WHERE neighborhoodId = :neighborhoodId AND id = :id LIMIT 1")
    protected abstract suspend fun findRecord(neighborhoodId: Long, id: Long): StakeoutManholeEntity?

    @Query("SELECT * FROM stakeout_manholes WHERE neighborhoodId = :neighborhoodId AND nameKey = :nameKey LIMIT 1")
    protected abstract suspend fun findRecordByName(neighborhoodId: Long, nameKey: String): StakeoutManholeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertRecord(record: StakeoutManholeEntity): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun updateRecord(record: StakeoutManholeEntity): Int

    @Transaction
    open suspend fun saveRecord(entity: StakeoutManholeEntity) {
        val record = validatedRecord(entity)
        val duplicate = findRecordByName(record.neighborhoodId, record.nameKey)
        require(duplicate == null || duplicate.id == record.id) {
            "Bu mahallede aynı baca numarası zaten kayıtlı: ${record.name}"
        }
        if (record.id == 0L) {
            insertRecord(record)
        } else {
            val previous = findRecord(record.neighborhoodId, record.id)
            require(previous != null) { "Düzenlenecek baca seçilen mahallede bulunamadı." }
            if (previous.nameKey != record.nameKey) {
                retargetConnections(record.neighborhoodId, previous.nameKey, record.nameKey)
            }
            check(updateRecord(record) == 1) { "Baca kaydı güncellenemedi." }
        }
    }

    /** Re-imports update by neighborhood and name while retaining stable record IDs. */
    @Transaction
    open suspend fun importRecords(neighborhoodId: Long, records: List<StakeoutManholeEntity>): Int {
        require(neighborhoodId > 0) { "Yükleme için bir mahalle seçilmelidir." }
        require(records.all { it.neighborhoodId == neighborhoodId }) {
            "Yüklenen satırların tamamı seçilen mahalleye ait olmalıdır."
        }
        // Validate the entire batch before the first write, including rows with missing values.
        val validated = records.map(::validatedRecord)
        require(validated.map { it.nameKey }.distinct().size == validated.size) {
            "Yüklenen dosyada aynı baca numarası birden fazla kez bulunuyor."
        }
        validated.forEach { record ->
            val existing = findRecordByName(neighborhoodId, record.nameKey)
            if (existing == null) {
                insertRecord(record.copy(id = 0))
            } else {
                check(updateRecord(record.copy(id = existing.id, connectedToNameKey = existing.connectedToNameKey)) == 1) {
                    "Baca kaydı güncellenemedi: ${record.name}"
                }
            }
        }
        return validated.size
    }

    @Query("DELETE FROM stakeout_manholes WHERE neighborhoodId = :neighborhoodId AND id = :id")
    protected abstract suspend fun deleteRecordRow(neighborhoodId: Long, id: Long)

    @Transaction
    open suspend fun deleteRecord(neighborhoodId: Long, id: Long) {
        val record = findRecord(neighborhoodId, id) ?: return
        retargetConnections(neighborhoodId, record.nameKey, null)
        deleteRecordRow(neighborhoodId, id)
    }

    @Query("UPDATE stakeout_manholes SET connectedToNameKey = :target WHERE neighborhoodId = :neighborhoodId AND connectedToNameKey = :oldKey")
    protected abstract suspend fun retargetConnections(neighborhoodId: Long, oldKey: String, target: String?)

    @Query("SELECT * FROM stakeout_manholes WHERE id = :id LIMIT 1")
    protected abstract suspend fun findRecordById(id: Long): StakeoutManholeEntity?

    @Query("DELETE FROM stakeout_manholes WHERE neighborhoodId = :neighborhoodId")
    abstract suspend fun clearRecords(neighborhoodId: Long)

    private fun validatedRecord(entity: StakeoutManholeEntity): StakeoutManholeEntity {
        require(entity.id >= 0 && entity.neighborhoodId > 0) { "Geçerli bir mahalle ve baca kaydı seçilmelidir." }
        val name = entity.name.trim()
        require(name.isNotBlank()) { "Baca numarası boş bırakılamaz." }
        val numbers = listOf(
            entity.projectY, entity.projectX, entity.projectCoverLevel,
            entity.projectGroundLevel, entity.projectInvertLevel, entity.terrainGroundLevel,
            entity.dischargeCoverLevel, entity.dischargeDepth, entity.terrainInvertLevel
        )
        require(numbers.all { it == null || it.isFinite() }) {
            "Baca koordinatları, kotları ve derinliği geçerli sayılardan oluşmalıdır."
        }
        require(entity.dischargeDepth == null || entity.dischargeDepth >= 0.0) {
            "Deşarj derinliği negatif olamaz."
        }
        return entity.copy(name = name, nameKey = normalizeEntityName(name), updatedAt = System.currentTimeMillis())
    }

    @Query("UPDATE stakeout_manholes SET connectedToNameKey = :targetNameKey, updatedAt = :time WHERE id = :id")
    protected abstract suspend fun updateConnectionRow(id: Long, targetNameKey: String?, time: Long)

    @Transaction
    open suspend fun updateConnection(id: Long, targetNameKey: String?, time: Long) {
        val source = findRecordById(id) ?: error("Bağlantı kurulacak baca bulunamadı.")
        val target = targetNameKey?.let(::normalizeEntityName)
        require(target != source.nameKey) { "Bir baca kendisine bağlanamaz." }
        require(target == null || findRecordByName(source.neighborhoodId, target) != null) {
            "Hedef baca aynı mahallede bulunmalıdır."
        }
        updateConnectionRow(id, target, time)
    }
}
