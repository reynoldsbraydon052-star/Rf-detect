import re

with open('app/src/main/java/com/example/IntelligenceDaos.kt', 'r') as f:
    content = f.read()

# Add getSessionById and deleteSessionById to RfSessionDao
session_dao_methods = """    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RfSessionEntity)
    
    @Query("SELECT * FROM rf_session WHERE id = :id")
    suspend fun getSessionById(id: String): RfSessionEntity?
    
    @Query("DELETE FROM rf_session WHERE id = :id")
    suspend fun deleteSessionById(id: String)
    
    @Query("DELETE FROM rf_session")
    suspend fun clearAll()"""

content = content.replace("""    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RfSessionEntity)
    
    @Query("DELETE FROM rf_session")
    suspend fun clearAll()""", session_dao_methods)

with open('app/src/main/java/com/example/IntelligenceDaos.kt', 'w') as f:
    f.write(content)
