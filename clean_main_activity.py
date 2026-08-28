import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()

out_lines = []
skip = False
i = 0
while i < len(lines):
    line = lines[i]
    if "if (isSelectedTarget && correlations.isNotEmpty())" in line:
        # Check if we are inside SpectrumInterceptCard. SpectrumInterceptCard ends around 2100.
        # It takes ~28 lines
        # if this isn't line ~2160, we skip it
        if not (2000 < i < 2200):
            # Skip until we hit @Composable
            j = i
            while j < len(lines) and "@Composable" not in lines[j]:
                j += 1
            # But wait, there are brackets. The original replacement replaced:
            #                 }
            #             }
            #         }
            #     }
            # }
            #
            # @Composable
            #
            # We want to replace it BACK with the original string.
            out_lines.append("                }\n")
            out_lines.append("            }\n")
            out_lines.append("        }\n")
            out_lines.append("    }\n")
            out_lines.append("}\n")
            out_lines.append("\n")
            if j < len(lines) and "@Composable" in lines[j]:
                out_lines.append(lines[j])
            i = j + 1
            continue
    out_lines.append(line)
    i += 1

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.writelines(out_lines)
