import re

with open('app/src/main/java/com/example/RfRecordingDatabase.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "@Database(entities = [RfRecordedEventEntity::class, DeviceIdentityEntity::class, RfSessionEntity::class, RfAnomalyEntity::class, RfPatternEntity::class, AnomalyCorrelationEntity::class, RfAnnotationEntity::class], version = 7",
    "@Database(entities = [RfRecordedEventEntity::class, DeviceIdentityEntity::class, RfSessionEntity::class, RfAnomalyEntity::class, RfPatternEntity::class, AnomalyCorrelationEntity::class, RfAnnotationEntity::class, EvidenceItem::class], version = 8"
)

content = content.replace(
    "abstract fun rfAnnotationDao(): RfAnnotationDao",
    "abstract fun rfAnnotationDao(): RfAnnotationDao\n    abstract fun evidenceDao(): EvidenceDao"
)

with open('app/src/main/java/com/example/RfRecordingDatabase.kt', 'w') as f:
    f.write(content)
