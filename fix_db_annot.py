import re

with open('app/src/main/java/com/example/RfRecordingDatabase.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "entities = [RfRecordedEventEntity::class, DeviceIdentityEntity::class, RfSessionEntity::class, RfAnomalyEntity::class, RfPatternEntity::class, AnomalyCorrelationEntity::class]",
    "entities = [RfRecordedEventEntity::class, DeviceIdentityEntity::class, RfSessionEntity::class, RfAnomalyEntity::class, RfPatternEntity::class, AnomalyCorrelationEntity::class, RfAnnotationEntity::class]"
)

content = content.replace("version = 6", "version = 7")

if "abstract fun rfAnnotationDao(): RfAnnotationDao" not in content:
    content = content.replace(
        "abstract fun rfPatternDao(): RfPatternDao",
        "abstract fun rfPatternDao(): RfPatternDao\n    abstract fun rfAnnotationDao(): RfAnnotationDao"
    )

with open('app/src/main/java/com/example/RfRecordingDatabase.kt', 'w') as f:
    f.write(content)

