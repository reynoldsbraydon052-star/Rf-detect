import re

with open('app/src/main/java/com/example/IntelligenceDaos.kt', 'r') as f:
    content = f.read()

annotation_dao = """
@Dao
interface RfAnnotationDao {
    @Query("SELECT * FROM rf_annotations WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    fun getAnnotationsBySessionId(sessionId: String): Flow<List<RfAnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: RfAnnotationEntity)

    @Query("DELETE FROM rf_annotations WHERE id = :id")
    suspend fun deleteAnnotation(id: String)
}
"""

content += "\n" + annotation_dao

with open('app/src/main/java/com/example/IntelligenceDaos.kt', 'w') as f:
    f.write(content)

