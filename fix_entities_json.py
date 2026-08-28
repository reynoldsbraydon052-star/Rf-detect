import re

with open('app/src/main/java/com/example/IntelligenceModels.kt', 'r') as f:
    content = f.read()

# Add to RfSessionEntity
if "@JsonClass(generateAdapter = true)\n@Entity" not in content:
    content = content.replace("@Entity(tableName = \"rf_session\")\ndata class RfSessionEntity", "@com.squareup.moshi.JsonClass(generateAdapter = true)\n@Entity(tableName = \"rf_session\")\ndata class RfSessionEntity")

with open('app/src/main/java/com/example/IntelligenceModels.kt', 'w') as f:
    f.write(content)


with open('app/src/main/java/com/example/AnnotationModels.kt', 'r') as f:
    content = f.read()

if "@JsonClass(generateAdapter = true)\n@Entity" not in content:
    content = content.replace("@Entity(tableName = \"rf_annotations\")\ndata class RfAnnotationEntity", "@com.squareup.moshi.JsonClass(generateAdapter = true)\n@Entity(tableName = \"rf_annotations\")\ndata class RfAnnotationEntity")

with open('app/src/main/java/com/example/AnnotationModels.kt', 'w') as f:
    f.write(content)

