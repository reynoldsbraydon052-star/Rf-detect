with open('app/src/main/java/com/example/GeminiThreatModels.kt', 'r') as f:
    content = f.read()

target1 = 'val threatCategory: ThreatCategory,'
if target1 in content and "val inferenceType: InferenceType" not in content:
    # We want to replace it in DetailedTargetAudit
    content = content.replace(target1, target1 + "\n    val inferenceType: InferenceType = InferenceType.UNKNOWN,", 1) # only first occurrence

target2 = 'val isImsiAlertActive: Boolean,'
if target2 in content and "val inferenceType: InferenceType" not in content:
    content = content.replace(target2, target2 + "\n    val inferenceType: InferenceType = InferenceType.UNKNOWN,")
    
target3 = 'val analyzedRfBufferCount: Int = 0,'
if target3 in content and "val inferenceType: InferenceType" not in content:
    content = content.replace(target3, target3 + "\n    val inferenceType: InferenceType = InferenceType.UNKNOWN,")
    
with open('app/src/main/java/com/example/GeminiThreatModels.kt', 'w') as f:
    f.write(content)
